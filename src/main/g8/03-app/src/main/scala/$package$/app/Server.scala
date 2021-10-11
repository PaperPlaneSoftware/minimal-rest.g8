package $package$
package app

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.ExecutionContext.global

import cats.effect.*
import cats.effect.std.*
import cats.implicits.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.io.net.Network
import skunk.*

import org.http4s.server.middleware.*
import org.http4s.server.staticcontent.*
import org.http4s.server.Router
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*

import org.slf4j.LoggerFactory
import natchez.Trace.Implicits.noop

import org.flywaydb.core.internal.jdbc.DriverDataSource
import org.flywaydb.core.Flyway

import persistence.pg.*
import service.*
import app.controllers.*

object Server {
  val logger = LoggerFactory.getLogger(getClass())

  // get database configuration
  val dbHost        = sys.env.getOrElse("POSTGRES_HOST", "localhost")
  val dbPort        = sys.env.getOrElse("POSTGRES_PORT", "5432").toInt
  val dbName        = sys.env.getOrElse("POSTGRES_DB", "osi")
  val dbMaxConns    = sys.env.getOrElse("POSTGRES_MAX_CONN", "10").toInt
  val dbDebug       = sys.env.getOrElse("POSTGRES_DEBUG", "true").toBoolean
  val dbAutoMigrate = sys.env.getOrElse("POSTGRES_AUTO_MIGRATE", "false").toBoolean
  val dbWorker      = sys.env.getOrElse("POSTGRES_WORKER", "$name$_worker")
  val dbWorkerPass  = sys.env.get("POSTGRES_WORKER_PASSWORD")

  def migrate[F[_]](retry: Int = 0)(using Async[F]): F[Unit] = {
    val dbOwner     = sys.env.getOrElse("POSTGRES_USER", "$name$_owner")
    val dbOwnerPass = sys.env.get("POSTGRES_PASSWORD")

    try
      val dataSource = new DriverDataSource(
        getClass().getClassLoader(),
        null,
        s"jdbc:postgresql://\${dbHost}:\${dbPort}/\${dbName}",
        dbOwner,
        dbOwnerPass.getOrElse("")
      )

      Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()

      summon[Async[F]].pure(())
    catch
      case _ if retry < 3 =>
        logger.info("Retrying migration...")
        Thread.sleep(100)
        migrate(retry + 1)
      case ex =>
        logger.error(s"Migration failed.\n\${ex.getMessage()}")
        summon[Async[F]].raiseError(ex)
  }

  def serve[F[_]](using Async[F], Console[F], Network[F]): F[Unit] = {
    val migrateOrNot = if dbAutoMigrate then migrate() else summon[Async[F]].pure(())

    for
      _ <- migrateOrNot
      _ <- SessionPool
        .make(dbHost, dbPort, dbWorker, dbName, dbWorkerPass, dbMaxConns, dbDebug)
        .use { db =>
          for
            // Construct dependency graph
            todoDao     <- TodoDao.make(db)
            todoService <- TodoService.make(todoDao)

            // Define middleware
            withLogging = Logger.httpApp[F](true, true)(_)
            withCors    = CORS.httpApp[F]

            // Define routes
            httpApp = Router(
              "/api" -> TodoController.routes(todoService),
              "/"    -> fileService[F](FileService.Config("./public"))
            ).orNotFound

            // Construct http app
            finalHttpApp = (withCors compose withLogging)(httpApp)

            // Run http server
            exitCode <- Stream
              .resource(
                EmberServerBuilder
                  .default[F]
                  .withHost(ipv4"0.0.0.0")
                  .withPort(port"8080")
                  .withHttpApp(finalHttpApp)
                  .build >>
                  Resource.eval(Async[F].never)
              )
              .compile
              .drain
          yield exitCode
        }
    yield ()
  }
}
