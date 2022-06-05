package $name;format="space,snake"$
package domain

import java.time.LocalDateTime

import persistence.UserRepo.AppUser

case class Company(
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    companyAddress: Option[String],
    companyName: String,
    postcode: String,
    id: Int
)

case class CompanyWithUser(company: Company, user: AppUser):
  export company.*
