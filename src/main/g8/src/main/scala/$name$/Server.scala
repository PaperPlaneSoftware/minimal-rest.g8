package $name$

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.{Async, IO, Resource}
import cats.implicits.*
import com.comcast.ip4s.*
import fs2.Stream
import java.time.LocalDateTime
import java.util.UUID
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.jdbc.DriverDataSource
import org.http4s.{Credentials, Request}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.server.staticcontent.{fileService, FileService}
import org.slf4j.LoggerFactory
import $name$.domain.UserSession
import $name$.persistence.{SessionPool, SessionRepo, TodoRepo}
import $name$.routes.{authRoutes, todoRoutes}
import scala.util.{Failure, Success, Try}
import skunk.Session

// ---- Authorization helper functions ----
private def hasExpired(session: UserSession) = session.expiresAt `isBefore` LocalDateTime.now

private def getHeader(header: Option[Authorization]) =
  EitherT {
    header match
      case None         => IO.pure(AuthErr.NoHeader.asLeft)
      case Some(header) => IO.pure(header.credentials.toString.replaceFirst("Basic ", "").asRight)
  }

private def parseHeader(header: String) =
  EitherT {
    Try(UUID.fromString(header)) match
      case Failure(_)         => IO.pure(AuthErr.InvalidHeader.asLeft)
      case Success(sessionId) => IO.pure(sessionId.asRight)
  }

private def validateSession(session: IO[Option[UserSession]]) =
  EitherT {
    session.map {
      case None                     => AuthErr.SessionNotFound.asLeft
      case Some(s) if hasExpired(s) => AuthErr.SessionExpired.asLeft
      case Some(s)                  => s.asRight
    }
  }

private def buildAuthFunc(db: Resource[IO, Session[IO]]) = Kleisli { (req: Request[IO]) =>
  OptionT.liftF {
    val eitherT = for
      authHeader   <- getHeader(req.headers.get[Authorization])
      parsedHeader <- parseHeader(authHeader)
      session <- validateSession {
        db.use(implicit s => SessionRepo.readSession(parsedHeader))
      }
    yield session

    eitherT.value
  }
}
// ---- ---- ---- ----

object Server:
  val logger = LoggerFactory.getLogger(getClass())

  // get database configuration
  val dbHost       = sys.env.get("POSTGRES_HOST").get
  val dbPort       = sys.env.get("POSTGRES_PORT").get.toInt
  val dbName       = sys.env.get("POSTGRES_DB").get
  val dbMaxConns   = sys.env.get("POSTGRES_MAX_CONN").get.toInt
  val dbDebug      = sys.env.get("POSTGRES_DEBUG").get.toBoolean
  val dbWorker     = sys.env.get("POSTGRES_WORKER").get
  val dbWorkerPass = sys.env.get("POSTGRES_WORKER_PASSWORD")

  val autoMigrate    = sys.env.get("AUTO_MIGRATE").get.toBoolean
  val testMigrations = sys.env.get("TEST_MIGRATIONS").get.toBoolean

  def migrate(retry: Int = 0): IO[Unit] =
    val dbOwner     = sys.env.get("POSTGRES_USER").get
    val dbOwnerPass = sys.env.get("POSTGRES_PASSWORD").get

    try
      val dataSource = new DriverDataSource(
        getClass().getClassLoader(),
        null,
        s"jdbc:postgresql://\${dbHost}:\${dbPort}/\${dbName}",
        dbOwner,
        dbOwnerPass
      )

      val defaultLocation = "db/migrations/default"
      val testLocation    = if testMigrations then "db/migrations/test" else null

      Flyway
        .configure()
        .dataSource(dataSource)
        .locations(defaultLocation, testLocation)
        .load()
        .migrate()

      IO.pure(())
    catch
      case _ if retry < 3 =>
        logger.info("Retrying migration...")
        Thread.sleep(100)
        migrate(retry + 1)

      case ex =>
        logger.error(s"Migration failed.\n\${ex.getMessage()}")
        IO.raiseError(ex)
  end migrate

  def buildServer(using natchez.Trace[IO]): IO[Unit] = SessionPool
    .make(dbHost, dbPort, dbWorker, dbName, dbWorkerPass, dbMaxConns, dbDebug)
    .use { implicit db =>
      // Authorization function
      val authUser = buildAuthFunc(db)

      // Define middleware
      val withLogging = Logger.httpApp[IO](true, true)(_)
      val withCors    = CORS.policy.withAllowOriginAll.httpApp[IO]
      val withAuth    = AuthMiddleware(authUser)

      // Define service
      val router = Router(
        "/auth" -> authRoutes,
        "/api"  -> withAuth(todoRoutes),
        "/"     -> fileService[IO](FileService.Config("./public"))
      ).orNotFound

      // Construct http app
      val httpApp = (withCors compose withLogging)(router)

      // run the server
      Stream
        .resource(
          EmberServerBuilder
            .default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8080")
            .withHttpApp(httpApp)
            .build >>
            Resource.eval(Async[IO].never)
        )
        .compile
        .drain
    }
  end buildServer

  def serve(using Monad[IO], natchez.Trace[IO]): IO[Unit] =
    for
      _ <- if autoMigrate then migrate() else IO.pure(())
      _ <- buildServer
    yield ()

end Server
