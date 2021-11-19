package $name$
package domain

import java.util.UUID
import java.time.LocalDateTime

case class User(username: String, id: Option[Int])

case class UserSession(
    userId: Int,
    createdAt: LocalDateTime,
    expiresAt: LocalDateTime,
    sessionId: UUID
)
