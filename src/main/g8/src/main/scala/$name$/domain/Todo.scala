package $name$.domain

case class Todo(task: String, done: Boolean, author: Option[User], id: Option[Int])
