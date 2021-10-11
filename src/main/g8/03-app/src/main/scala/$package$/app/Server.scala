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
  val dbHost        = sys.env.get("POSTGRES_HOST").get
  val dbPort        = sys.env.get("POSTGRES_PORT").get.toInt
  val dbName        = sys.env.get("POSTGRES_DB").get
  val dbMaxConns    = sys.env.get("POSTGRES_MAX_CONN").get.toInt
  val dbDebug       = sys.env.get("POSTGRES_DEBUG").get.toBoolean
  val dbAutoMigrate = sys.env.get("POSTGRES_AUTO_MIGRATE").get.toBoolean
  val dbWorker      = sys.env.get("POSTGRES_WORKER").get
  val dbWorkerPass  = sys.env.get("POSTGRES_WORKER_PASSWORD").get

  def migrate[F[_]](retry: Int = 0)(using Async[F]): F[Unit] = {
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
