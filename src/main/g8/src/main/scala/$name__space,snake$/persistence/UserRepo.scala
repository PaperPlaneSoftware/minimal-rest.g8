package $name;format="space,snake"$
package persistence

import java.util.UUID

import cats.effect.IO

import doobie.ConnectionIO
import doobie.implicits.*

import io.getquill.doobie.DoobieContext
import io.getquill.{idiom as *, *}
import org.slf4j.LoggerFactory

import domain.UserSession

object UserRepo:
  val ctx = DoobieContext.Postgres(NamingStrategy(SnakeCase, LowerCase))
  import ctx.*

  case class AppUser(email: String, passwd: String, id: Option[Int])

  def login(email: String, password: String): ConnectionIO[Option[Int]] =
    sql"select id from app_user where email = \$email and passwd = crypt(\$password, passwd)"
      .query[Int]
      .option
