package $package$
package service

import cats.Monad
import cats.implicits.*
import domain.Todo
import persistence.{TodoDao, TodoEntity}

object TodoService {
  def make[F[_]: Monad, TodoKey](todoDao: TodoDao[F, TodoKey]): F[TodoService[F, TodoKey]] =
    summon[Monad[F]].pure(TodoService(todoDao))
}

class TodoService[F[_]: Monad, TodoKey] private (todoDao: TodoDao[F, TodoKey]) {
  def readAll(): F[List[TodoEntity[TodoKey]]] = todoDao.readAll()
  def insert(todo: Todo): F[TodoKey]          = todoDao.insert(todo)
  def delete(k: TodoKey): F[Unit]             = todoDao.delete(k)
  def complete(k: TodoKey): F[Unit] =
    for {
      todo <- todoDao.read(k)
      _    <- todoDao.update(k, todo.copy(done = true))
    } yield ()
}
