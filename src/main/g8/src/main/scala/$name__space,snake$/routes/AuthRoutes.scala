package $name;format="space,snake"$
package routes

import scala.tools.nsc.interactive.Response

import cats.effect.{IO, Resource}

import doobie.util.transactor.Transactor

import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import services.AuthService

val logger = LoggerFactory.getLogger("AuthRoutes")

final case class Deets(username: String, password: String)

def authRoutes(using Transactor[IO]): HttpRoutes[IO] =

  val dsl = new Http4sDsl[IO] {}
  import dsl.*

  HttpRoutes.of[IO] { case req @ POST -> Root / "login" =>
    val res = for
      deets     <- req.as[Deets]
      sessionId <- AuthService.login(deets.username, deets.password)
      response  <- sessionId match
                     case Left(err)        => Forbidden(err.msg)
                     case Right(sessionId) => Ok(sessionId)
    yield response

    res.handleErrorWith(err =>
      logger.error(err.getMessage)
      InternalServerError("Service error")
    )
  }
