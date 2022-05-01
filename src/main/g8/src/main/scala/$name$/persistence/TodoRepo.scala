package $name$
package persistence

import cats.effect.{IO, Resource}
import fs2.Stream
import skunk.Session
import skunk.implicits.*
import skunk.codec.all.*
import tydal.*

object TodoSchema
    extends TableSchema[
      "todo",
      (
          "task" :=: text,
          "done" :=: bool,
          "created_by" :=: int4,
          "id" :=: nullable[int4]
      )
    ]

object TodoRepo:
  case class TodoRow(task: String, done: Boolean, created_by: Int, id: Option[Int])

  protected[persistence] object Codecs:
    val todo = (text *: bool *: int4 *: int4.opt).pimap[TodoRow]

  protected[persistence] object Sql:
    val table     = sql"todo"
    val cols      = sql"task, done, created_by"
    val selectAll = sql"SELECT $cols, id FROM $table"
    val selectOne = sql"SELECT $cols, id FROM $table WHERE id = $int4"
    val insertOne = sql"INSERT INTO $table (${cols}) VALUES ($text, $bool, $int4) RETURNING id"
    val deleteOne = sql"DELETE FROM $table WHERE id = $int4"
    val updateOne = sql"UPDATE $table SET task = $text, done = $bool WHERE id = $int4"

    val selectTydal =
      Select
        .from(TodoSchema `as` "t")
        .take(_("t", "task"))
        .where(x => (x("t", "done") === "done?") `and` (x("t", "id") === "id?"))
        .compile

  import Codecs.*
  import TodoRepo.Sql.*

  def poopy(using s: Session[IO]): IO[List[String]] =
    s.prepare(selectTydal)
      .use(
        _.stream(
          (
            "done?" ~~> false,
            "id?" ~~> Some(1)
          ),
          10
        ).compile.toList
      )

  def readAll(using s: Session[IO]): IO[List[TodoRow]] =
    s.execute(selectAll.query(todo))

  def read(id: Int)(using s: Session[IO]): IO[TodoRow] =
    s.prepare(selectOne.query(todo)).use(_.unique(id))

  def insert(todo: TodoRow)(using s: Session[IO]): IO[Int] =
    s.prepare(insertOne.query(int4)).use(_.unique((todo.task, todo.done), todo.created_by))

  def delete(k: Int)(using s: Session[IO]): IO[Unit] =
    s.prepare(deleteOne.command).use(_.execute(k)).map(_ => ())

  def update(k: Int, todo: TodoRow)(using s: Session[IO]): IO[Unit] =
    s.prepare(updateOne.command).use(_.execute(((todo.task, todo.done), k))).map(_ => ())
