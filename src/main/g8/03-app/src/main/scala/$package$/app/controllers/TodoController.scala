package $package$
package app
package controllers

import cats.*
import cats.effect.*
import cats.implicits.*

import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.circe.CirceEntityDecoder.*

import io.circe.syntax.*
import io.circe.generic.auto.*

import domain.*
import service.*

object TodoController {
  def routes[F[_]: Sync](todoService: TodoService[F, Int]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "todos" =>
      for
        todos    <- todoService.readAll()
        response <- Ok(todos)
      yield response
    }
}
