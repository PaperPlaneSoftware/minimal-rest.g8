package $name;format="space,snake"$
package routes

import cats.effect.{IO, Resource}

import doobie.util.transactor.Transactor

import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.slf4j.LoggerFactory

import domain.UserSession
import services.CompanyService

private val logger = LoggerFactory.getLogger("CompanyRoutes")

// private object UserQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("user")

def companyRoutes(using Transactor[IO]): AuthedRoutes[AuthErr Or UserSession, IO] =
  val dsl = new Http4sDsl[IO] {}
  import dsl.*

  AuthedRoutes.of { case GET -> Root / "all" as userSessionId =>
    val res = CompanyService.read(userSessionId).flatMap {
      case Left(err)        => Forbidden("")
      case Right(companies) => Ok(companies)
    }

    res.handleErrorWith(err =>
      logger.error(err.getMessage)
      InternalServerError("Service error")
    )
  }
