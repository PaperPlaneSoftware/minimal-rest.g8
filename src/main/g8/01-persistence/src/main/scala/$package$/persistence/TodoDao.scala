package $package$
package persistence

import domain.Todo

trait TodoDao[F[_], TodoKey] {
  def readAll(): F[List[TodoEntity[TodoKey]]]
  def read(k: TodoKey): F[Todo]
  def insert(todo: Todo): F[TodoKey]
  def delete(k: TodoKey): F[Unit]
  def update(k: TodoKey, todo: Todo): F[Unit]
}
