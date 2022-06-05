package $name;format="space,snake"$
package persistence

import java.time.LocalDateTime
import java.util.UUID

import cats.effect.IO
import cats.implicits.*

import doobie.ConnectionIO
import doobie.implicits.*

import io.getquill.doobie.DoobieContext
import io.getquill.{idiom as *, *}
import org.slf4j.LoggerFactory

import domain.UserSession

object UserSessionRepo:
  val logger = LoggerFactory.getLogger(getClass())

  val ctx = DoobieContext.Postgres(NamingStrategy(SnakeCase, LowerCase))
  import ctx.*

  case class AppUserSession(
      userId: Int,
      createdAt: LocalDateTime,
      expiresAt: LocalDateTime,
      sessionId: UUID
  )

  def readSession(id: UUID): ConnectionIO[Option[UserSession]] =
    val q = quote {
      query[AppUserSession]
        .filter(_.sessionId == lift(id))
        .map(a => UserSession(a.userId, a.createdAt, a.expiresAt, a.sessionId))
    }
    run(q).map(_.headOption)

  def newSession(session: AppUserSession): ConnectionIO[UUID] =
    val q = quote {
      query[AppUserSession].insertValue(lift(session)).returning(_.sessionId)
    }
    run(q)

  def as(userId: String): ConnectionIO[Unit] =
    sql"select set_config('auth.id', \$userId, false)".query[Unit].unique
