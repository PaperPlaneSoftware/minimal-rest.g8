package $name$
package services

import cats.effect.{IO, Resource}
import cats.implicits.*
import java.util.UUID
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import persistence.{SessionRepo, UserRepo}
import skunk.Session

final case class SigninDeets(username: String, password: String)

object AuthService:
  def login(uname: String, pwd: String)(using
      db: Resource[IO, Session[IO]]
  ): IO[AuthErr Or UUID] =
    withinTransaction { implicit s => xa =>
      for
        idOpt     <- UserRepo.login(uname, pwd)
        sessionId <- idOpt.map(SessionRepo.newSession(_, now, now `plusDays` 1)).sequence
      yield sessionId match
        case None            => AuthErr.IncorrectLoginDetails.asLeft
        case Some(sessionId) => sessionId.asRight
    }
