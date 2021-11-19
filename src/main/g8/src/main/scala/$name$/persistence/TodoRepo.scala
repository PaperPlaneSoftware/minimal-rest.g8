package $name$
package persistence

import cats.effect.{IO, Resource}
import fs2.Stream
import skunk.Session
import skunk.implicits.*
import skunk.codec.all.*

object TodoRepo:
  case class TodoRow(task: String, done: Boolean, author: Int, id: Option[Int])

  protected[persistence] object Codecs:
    val todo = (text *: bool *: int4 *: int4.opt).pimap[TodoRow]

  protected[persistence] object Sql:
    val cols       = sql"task, done, author"
    val selectAll  = sql"SELECT * FROM todo"
    val selectUser = sql"SELECT * from todo WHERE author = \$int4"
    val selectOne  = sql"SELECT \${cols}, id FROM todo WHERE id = \$int4"
    val insertOne  = sql"INSERT INTO todo (\${cols}) VALUES (\$text, \$bool, \$int4) RETURNING id"
    val deleteOne  = sql"DELETE FROM todo WHERE id = \$int4"
    val updateOne  = sql"UPDATE todo SET task = \$text, done = \$bool WHERE id = \$int4"

  import Codecs.*
  import TodoRepo.Sql.*

  def readAll(using s: Session[IO]): IO[List[TodoRow]] =
    s.execute(selectAll.query(todo))

  def readUser(author: Int)(using s: Session[IO]): IO[List[TodoRow]] =
    s.prepare(selectUser.query(todo)).use(_.stream(author, 10).compile.toList)

  def read(id: Int)(using s: Session[IO]): IO[TodoRow] =
    s.prepare(selectOne.query(todo)).use(_.unique(id))

  def insert(todo: TodoRow)(using s: Session[IO]): IO[Int] =
    s.prepare(insertOne.query(int4)).use(_.unique((todo.task, todo.done), todo.author))

  def delete(k: Int)(using s: Session[IO]): IO[Unit] =
    s.prepare(deleteOne.command).use(_.execute(k)).map(_ => ())

  def update(k: Int, todo: TodoRow)(using s: Session[IO]): IO[Unit] =
    s.prepare(updateOne.command).use(_.execute(((todo.task, todo.done), k))).map(_ => ())
