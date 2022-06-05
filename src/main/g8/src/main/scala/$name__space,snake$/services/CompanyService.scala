package $name;format="space,snake"$
package services

import java.util.UUID

import cats.data.*
import cats.effect.{IO, Resource}
import cats.implicits.*

import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.transactor.Transactor

import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.AuthedRoutes
import org.http4s.QueryParamDecoder.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher
import org.slf4j.LoggerFactory

import domain.{Company, UserSession}
import persistence.{CompanyRepo, CompanyRow, UserSessionRepo}

object CompanyService:
  val logger = LoggerFactory.getLogger(getClass())

  def toDomain(c: CompanyRow): Company = Company(
    createdAt = c.createdAt,
    updatedAt = c.updatedAt,
    companyAddress = c.companyAddress,
    companyName = c.companyName,
    postcode = c.postcode,
    id = c.id.get
  )

  def read(userSession: AuthErr Or UserSession)(using
      xa: Transactor[IO]
  ): IO[AuthErr Or List[Company]] =
    userSession
      .map(userSession =>
        val query =
          for
            _           <- UserSessionRepo.as(userSession.userId.toString)
            companyRows <- CompanyRepo.readAll
            companies   <- companyRows.map(toDomain).pure[ConnectionIO]
          yield companies

        query.transact(xa)
      )
      .sequence
