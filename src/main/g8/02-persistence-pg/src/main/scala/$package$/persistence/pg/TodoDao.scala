package $package$
package persistence
package pg

import cats.implicits.*
import cats.effect.*

import skunk.*
import skunk.implicits.*
import skunk.codec.all.*

import domain.Todo

object TodoDao {
  def make[F[_]: MonadCancelThrow](db: Resource[F, Session[F]]): F[TodoDao[F, Int]] =
    summon[MonadCancelThrow[F]].pure(
      new TodoDao[F, Int] {
        override def readAll(): F[List[TodoEntity[Int]]] =
          db.use(_.execute(TodoSql.selectAll))

        override def read(id: Int): F[Todo] =
          db.use(_.prepare(TodoSql.selectOne).use(_.unique(id)))

        override def insert(todo: Todo): F[Int] =
          db.use(_.prepare(TodoSql.insertOne).use(_.unique(todo.task, todo.done)))

        override def delete(k: Int): F[Unit] =
          db.use(_.prepare(TodoSql.deleteOne).use(_.execute(k))).map(_ => ())

        override def update(k: Int, todo: Todo): F[Unit] =
          db.use(_.prepare(TodoSql.updateOne).use(_.execute(((todo.task, todo.done), k))))
            .map(_ => ())
      }
    )
}
