package $name$
package routes

import cats.effect.{IO, Resource}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import skunk.Session
import $name$.services.AuthService

final case class Deets(username: String, password: String)

def authRoutes(using db: Resource[IO, Session[IO]]): HttpRoutes[IO] =
  val dsl = new Http4sDsl[IO] {}
  import dsl.*

  HttpRoutes.of[IO] { case req @ POST -> Root / "login" =>
    for
      deets     <- req.as[Deets]
      sessionId <- AuthService.login(deets.username, deets.password)
      response <- sessionId match
        case Left(err)        => Forbidden(err.msg)
        case Right(sessionId) => Ok(sessionId)
    yield response
  }
