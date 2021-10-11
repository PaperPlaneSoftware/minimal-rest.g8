package $package$
package persistence
package pg

import cats.effect.Concurrent
import cats.effect.std.Console
import fs2.io.net.Network
import skunk.*

object SessionPool:
  def make[F[_]](
      host: String,
      port: Int = 5432,
      user: String,
      database: String,
      password: Option[String] = None,
      max: Int = 10,
      debug: Boolean = true
  )(using
      Concurrent[F],
      Console[F],
      Network[F],
      natchez.Trace[F]
  ): SessionPool[F] = Session.pooled(host, port, user, database, password, max, debug)
