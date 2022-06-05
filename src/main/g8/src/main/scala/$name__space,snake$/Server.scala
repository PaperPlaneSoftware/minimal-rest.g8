package $name;format="space,snake"$

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.{Async, IO, Resource}
import cats.implicits.*

import doobie.*
import doobie.hikari.*
import doobie.implicits.*
import doobie.util.transactor.Transactor

import fs2.Stream

import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.server.{AuthMiddleware, Router}
import org.slf4j.LoggerFactory

import domain.UserSession
import persistence.UserSessionRepo
import routes.{authRoutes, companyRoutes}

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

private def buildAuthFunc(using xa: Transactor[IO]) = Kleisli { (req: Request[IO]) =>
  OptionT.liftF {
    val eitherT = for
      authHeader   <- getHeader(req.headers.get[Authorization])
      parsedHeader <- parseHeader(authHeader)
      session      <- validateSession(UserSessionRepo.readSession(parsedHeader).transact(xa))
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
  val dbWorkerPass = sys.env.get("POSTGRES_WORKER_PASSWORD").get

  def buildServer: IO[Unit] =
    ExecutionContexts
      .fixedThreadPool[IO](32)
      .flatMap(ec =>
        HikariTransactor.newHikariTransactor[IO](
          "org.postgresql.Driver",                            // driver classname
          s"jdbc:postgresql://\$dbHost:\$dbPort/\$dbName", // connect URL
          dbWorker,                                           // username
          dbWorkerPass,                                       // password
          ec                                                  // await connection here
        )
      )
      .use { implicit xa =>
        // Authorization function
        val authUser = buildAuthFunc

        // Define middleware
        val withLogging = Logger.httpApp[IO](true, true)(_)
        val withCors    = CORS.policy.withAllowOriginAll.httpApp[IO]
        val withAuth    = AuthMiddleware(authUser)

        // Define service
        val apiRouter = Router(
          "/company" -> withAuth(companyRoutes)
        )
        val router    = Router(
          "/auth" -> authRoutes,
          "/api"  -> apiRouter,
          "/"     -> fileService[IO](FileService.Config("./public"))
        )

        // Construct http app
        val httpApp = (withCors compose withLogging)(router.orNotFound)

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

  def serve(using Monad[IO]): IO[Unit] = buildServer

end Server
