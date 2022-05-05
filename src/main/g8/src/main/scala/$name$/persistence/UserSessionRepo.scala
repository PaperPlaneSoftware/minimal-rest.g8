package $name$
package persistence

import cats.effect.IO
import cats.implicits.*
import java.util.UUID
import java.time.LocalDateTime
import skunk.Session
import skunk.data.Completion
import skunk.codec.all.*
import skunk.implicits.*

import domain.UserSession

object SessionRepo:
  object Codecs:
    val userSession = (int4 *: timestamp *: timestamp *: uuid).pimap[UserSession]

  object Sql:
    val table     = sql"tp_user_session"
    val cols      = sql"tp_user_id, created_at, expires_at, session_id"
    val selectOne = sql"SELECT \$cols FROM \$table WHERE session_id = \$uuid"
    val insertOne = sql"INSERT INTO \$table VALUES (\$int4, \$timestamp, \$timestamp, \$uuid)"
    def asUser(id: String) = sql"SET auth.id = #\$id"

  import Codecs.*
  import Sql.*

  def readSession(id: UUID)(using s: Session[IO]): IO[Option[UserSession]] =
    s.prepare(selectOne.query(userSession)).use(_.option(id))

  def newSession(
      userId: Int,
      createdAt: LocalDateTime,
      expiresAt: LocalDateTime,
      sessionId: Option[UUID] = None
  )(using
      s: Session[IO]
  ): IO[UUID] =
    val uuid = sessionId.getOrElse(UUID.randomUUID)

    s.prepare(insertOne.command.gcontramap[UserSession])
      .use(_.execute(UserSession(userId, createdAt, expiresAt, uuid)))
      .map(_ => uuid)

  def as(userId: Int)(using s: Session[IO]): IO[Completion] =
    s.execute(asUser(userId.toString).command) // .use(_.execute(userId))
