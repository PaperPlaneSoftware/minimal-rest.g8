package $name$
package persistence

import cats.effect.IO
import skunk.{Session, SessionPool}

object SessionPool:
  def make(
      host: String,
      port: Int = 5432,
      user: String,
      database: String,
      password: Option[String] = None,
      max: Int = 10,
      debug: Boolean = true
  )(using natchez.Trace[IO]): SessionPool[IO] =
    Session.pooled(host, port, user, database, password, max, debug)
