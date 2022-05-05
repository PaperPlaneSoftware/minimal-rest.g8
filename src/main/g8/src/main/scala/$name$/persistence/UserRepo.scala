package $name$
package persistence

import cats.effect.IO
import java.util.UUID
import skunk.Session
import skunk.codec.all.*
import skunk.data.Completion
import skunk.implicits.*
import skunk.Transaction

import domain.{User, UserSession}

object UserRepo:
  type UserRow = User

  protected[persistence] object Codecs:
    val userCodec = (text *: int4.opt).pimap[UserRow]

  protected[persistence] object Sql:
    val table          = sql"tp_user"
    val cols           = sql"username, id"
    val colsWithPasswd = sql"\$cols, passwd"
    val userIdFromSession =
      sql"SELECT user_id FROM \${SessionRepo.Sql.table} WHERE session_id = \$uuid"
    val idFromDeets =
      sql"SELECT id FROM \$table WHERE username = \$text AND passwd = crypt(\$text, passwd)"

  import Codecs.*
  import Sql.*

  def login(username: String, password: String)(using s: Session[IO]): IO[Option[Int]] =
    s.prepare(idFromDeets.query(int4)).use(_.option(username -> password))
