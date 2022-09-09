package domain

import cats.implicits.toBifunctorOps
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder.Result
import io.circe.{ Decoder, DecodingFailure, HCursor }
import io.circe.generic.semiauto.deriveDecoder

import java.util.UUID
import scala.util.control.NoStackTrace

object Auth {
  final case class UserId(value: UUID)
  final case class UserName(value: String)
  final case class Password(value: String)
  final case class EncryptedPassword(value: String)
  final case class JwtToken(value: String)

  final case class User(id: UserId, name: UserName)

  final case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase)
  }
  final case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }
  final case class LoginUser(username: UserNameParam, password: PasswordParam)
  object LoginUser {
    implicit val userNameParamDecoder: Decoder[UserNameParam] = new Decoder[UserNameParam] {
      override def apply(c: HCursor): Result[UserNameParam] =
        c.downField("username").as[String] match {
          case Left(error) => Left(error)
          case Right(value) =>
            NonEmptyString.from(value).bimap(error => DecodingFailure(error, List.empty), value => UserNameParam(value))
        }
    }
    implicit val passwordParamDecoder: Decoder[PasswordParam] = new Decoder[PasswordParam] {
      override def apply(c: HCursor): Result[PasswordParam] =
        c.downField("password").as[String] match {
          case Left(error) => Left(error)
          case Right(value) =>
            NonEmptyString.from(value).bimap(error => DecodingFailure(error, List.empty), value => PasswordParam(value))
        }
    }
    implicit val loginUserDecoder: Decoder[LoginUser] = deriveDecoder[LoginUser]
  }

  final case class UserNotFound(username: UserName) extends NoStackTrace
  final case class UserNameExist(username: UserName) extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation extends NoStackTrace

  case object TokenNotFound extends NoStackTrace
}
