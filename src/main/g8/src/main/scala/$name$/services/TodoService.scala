package $name$
package services

import cats.effect.{IO, Resource}
import io.circe.syntax.*
import io.circe.generic.auto.*
import java.util.UUID
import org.http4s.AuthedRoutes
import org.http4s.QueryParamDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import $name$.domain.{Todo, UserSession}
import $name$.persistence.{SessionRepo, TodoRepo}
import TodoRepo.TodoRow

import skunk.Session

private object UserQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("user")

object TodoService:
  def read(userId: Option[Int], userSession: AuthErr Or UserSession)(using
      db: Resource[IO, Session[IO]]
  ): IO[AuthErr Or List[TodoRow]] =
    asUser(userSession) { implicit s => xa =>
      userId match
        case None     => TodoRepo.readAll
        case Some(id) => TodoRepo.readUser(id)
    }
