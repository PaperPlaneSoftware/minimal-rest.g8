package $package$
package persistence

import domain.*

case class TodoEntity[TodoKey](todo: Todo, id: TodoKey) {
  export todo.*
}
