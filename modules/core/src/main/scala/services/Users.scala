package services

import cats.effect.MonadCancelThrow
import cats.effect.kernel.Resource
import cats.implicits.{
  catsSyntaxApplicativeError,
  catsSyntaxApplicativeErrorId,
  catsSyntaxOptionId,
  none,
  toFlatMapOps,
  toFunctorOps
}
import domain.Auth.{ EncryptedPassword, UserId, UserName, UserNameExist }
import domain.ID
import effects.GenUUID
import http.auth.User
import http.auth.User.UserWithPassword
import skunk._
import skunk.implicits._

trait Users[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName, password: EncryptedPassword): F[UserId]
}

object Users {
  def make[F[_]: GenUUID: MonadCancelThrow](postgres: Resource[F, Session[F]]): Users[F] =
    new Users[F] {
      import UserSQL._

      def find(username: UserName): F[Option[UserWithPassword]] =
        postgres.use { session =>
          session.prepare(selectUser).use { preparedQuery =>
            preparedQuery.option(username).map {
              case Some(user ~ password) => UserWithPassword(user.id, user.name, password).some
              case _                     => none[UserWithPassword]
            }
          }
        }

      def create(username: UserName, password: EncryptedPassword): F[UserId] =
        postgres.use { session =>
          session.prepare(insertUser).use { command =>
            ID.make[F, UserId].flatMap { id =>
              command
                .execute(User(id, username) ~ password)
                .as(id)
                .recoverWith {
                  case SqlState.UniqueViolation(_) => UserNameExist(username).raiseError[F, UserId]
                }
            }
          }
        }
    }

  private object UserSQL {
    import sql.Codecs._

    val codec: Codec[User ~ EncryptedPassword] =
      (userId ~ userName ~ encryptedPassword).imap {
        case id ~ name ~ password => User(id, name) ~ password
      } {
        case user ~ password => user.id ~ user.name ~ password
      }

    val selectUser: Query[UserName, User ~ EncryptedPassword] =
      sql"SELECT * FROM users WHERE name = $userName".query(codec)

    val insertUser: Command[User ~ EncryptedPassword] =
      sql"INSERT INTO users VALUES ($codec)".command
  }
}
