package $name;format="space,snake"$
package persistence

import java.time.LocalDateTime

import cats.effect.{IO, Resource}

import doobie.ConnectionIO

import fs2.Stream

import io.getquill.doobie.DoobieContext
import io.getquill.{idiom as *, *}

case class Company(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    createdByUserId: Option[Int],
    companyAddress: Option[String],
    companyName: String,
    postcode: String,
    id: Option[Int] = None
)
type CompanyRow = Company

object CompanyRepo:
  val ctx = DoobieContext.Postgres(NamingStrategy(SnakeCase, LowerCase))
  import ctx.*

  def readAll: ConnectionIO[List[CompanyRow]] = run(query[CompanyRow])

  // def read(id: Int): ConnectionIO[Option[Company]] =
  //   run(query[Company].filter(_.id.exists(_ == lift(id)))).map(_.headOption)

  // def insert(todo: Company): ConnectionIO[Int] =
  //   run(query[Company].insertValue(lift(todo)).returning(_.id.get))

  // def delete(k: Int): ConnectionIO[Long] =
  //   run(query[Company].filter(_.id.exists(_ == lift(k))).delete)

  // def update(k: Int, todo: TodoRow): ConnectionIO[Long] =
  //   run(quote(todos.filter(_.id.exists(_ == lift(k))).updateValue(lift(todo))))
