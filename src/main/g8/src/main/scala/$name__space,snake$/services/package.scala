package $name;format="space,snake"$
package services

import cats.implicits.*
import cats.effect.{IO, Resource}
import skunk.{Session, Transaction}

import domain.UserSession
import persistence.SessionRepo

/** Executes the given function within a postgres transaction.
  *
  * @param f
  *   The function to execute.
  */
def withinTransaction[B](f: Session[IO] => Transaction[IO] => IO[B])(using
    db: Resource[IO, Session[IO]]
): IO[B] =
  db.use { s =>
    s.transaction.use { xa =>
      f(s)(xa)
    }
  }

def asUser[B](sessionOrErr: AuthErr Or UserSession)(
    f: Session[IO] => Transaction[IO] => IO[B]
)(using db: Resource[IO, Session[IO]]): IO[AuthErr Or B] =
  withinTransaction { implicit s => implicit xa =>
    sessionOrErr.map(session => SessionRepo.as(session.userId).flatMap(_ => f(s)(xa))).sequence
  }
