package $name;format="space,snake"$
package domain

import java.time.LocalDateTime
import java.util.UUID

case class User(username: String, id: Option[Int])

case class UserSession(
    userId: Int,
    createdAt: LocalDateTime,
    expiresAt: LocalDateTime,
    sessionId: UUID
)
