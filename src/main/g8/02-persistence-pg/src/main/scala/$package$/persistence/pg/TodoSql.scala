package $package$
package persistence
package pg

import skunk.*
import skunk.implicits.*
import skunk.codec.all.*

import persistence.TodoEntity
import domain.Todo

object TodoSql:
  import Fragments.*
  import Encoders.*

  protected[pg] object Encoders:
    val todo       = (text *: bool).pimap[Todo]
    val todoEntity = (todo *: int4).pimap[TodoEntity[Int]]

  protected[pg] object Fragments:
    val todoCols       = sql"todo.task, todo.done"
    val todoEntityCols = sql"\$todoCols, todo.id"

  def selectAll: Query[Void, TodoEntity[Int]] =
    sql"SELECT * FROM todo".query(todoEntity)

  def selectOne: Query[Int, Todo] =
    sql"SELECT \${todoCols} FROM todo WHERE todo.id = \$int4".query(todo)

  def insertOne: Query[String ~ Boolean, Int] =
    sql"INSERT INTO todo (\${todoCols}) VALUES (\$text, \$bool) RETURNING todo.id".query(int4)

  def deleteOne: Command[Int] =
    sql"DELETE FROM todo WHERE id = \$int4".command

  def updateOne: Command[String ~ Boolean ~ Int] =
    sql"UPDATE todo SET todo.task = \$text, todo.done = \$bool WHERE todo.id = \$int4".command
