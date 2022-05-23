package $name;format="space,snake"$
package domain

case class Todo(task: String, done: Boolean, created_by: Option[User], id: Option[Int])
