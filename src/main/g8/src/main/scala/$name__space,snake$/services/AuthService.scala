package $name;format="space,snake"$
package services

import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

import cats.effect.{IO, Resource}
import cats.implicits.*

import doobie.implicits.*
import doobie.util.transactor.Transactor

import org.slf4j.LoggerFactory

import domain.UserSession
import persistence.{UserRepo, UserSessionRepo}

object AuthService:
  import UserSessionRepo.UserSession

  def login(uname: String, pwd: String)(using xa: Transactor[IO]): IO[AuthErr Or UUID] =
    val query =
      for
        idOpt     <- UserRepo.login(uname, pwd)
        sessionId <- idOpt
                       .map(userId =>
                         UserSessionRepo.newSession(
                           UserSession(userId, now, now `plusDays` 1, UUID.randomUUID)
                         )
                       )
                       .sequence
      yield sessionId match
        case None            => AuthErr.IncorrectLoginDetails.asLeft
        case Some(sessionId) => sessionId.asRight

    query.transact(xa)
