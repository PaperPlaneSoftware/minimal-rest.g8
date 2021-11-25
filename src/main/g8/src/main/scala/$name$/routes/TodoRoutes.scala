package $name$
package routes

import cats.effect.{IO, Resource}
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import $name$.domain.UserSession
import $name$.persistence.TodoRepo
import $name$.services.{asUser, TodoService}
import skunk.Session

private object UserQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("user")

def todoRoutes(using db: Resource[IO, Session[IO]]): AuthedRoutes[AuthErr Or UserSession, IO] =
  val dsl = new Http4sDsl[IO] {}
  import dsl.*

  AuthedRoutes.of { case GET -> Root / "todos" :? UserQueryParamMatcher(userId) as userSessionId =>
    TodoService.read(userId, userSessionId).flatMap {
      case Left(err)    => Forbidden("")
      case Right(todos) => Ok(todos)
    }
  }
