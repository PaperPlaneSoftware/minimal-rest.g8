package $name;format="space,snake"$
package routes

import cats.effect.{IO, Resource}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import skunk.Session

def devRoutes(using db: Resource[IO, Session[IO]]): HttpRoutes[IO] =
  val dsl = new Http4sDsl[IO] {}
  import dsl.*

  HttpRoutes.of[IO] {
    case req @ POST -> Root / "superuser" => Ok("yo")
    case req @ POST -> Root / "mockdata"  => Ok("mega!")
  }
