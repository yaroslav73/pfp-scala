package domain

import cats.Show
import cats.implicits.toBifunctorOps
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Decoder.Result
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor }
import monocle.Iso
import optics.IsUUID

import java.util.UUID
import javax.crypto.Cipher
import scala.util.control.NoStackTrace

object Auth {
  final case class UserId(value: UUID)
  final case class UserName(value: String)
  final case class Password(value: String)
  final case class EncryptedPassword(value: String)

  final case class EncryptCipher(value: Cipher)
  final case class DecryptCipher(value: Cipher)

  final case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.value.toLowerCase)
  }
  final case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value.value)
  }

  final case class CreateUser(username: UserNameParam, password: PasswordParam)
  final case class LoginUser(username: UserNameParam, password: PasswordParam)

  final case class ClaimContent(uuid: UUID)

  final case class UserNotFound(username: UserName) extends NoStackTrace
  final case class UserNameExist(username: UserName) extends NoStackTrace
  final case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation extends NoStackTrace

  case object TokenNotFound extends NoStackTrace

  implicit val userIdShow: Show[UserId]     = (userId: UserId) => userId.toString
  implicit val userNameShow: Show[UserName] = (userName: UserName) => userName.toString
  implicit val passwordShow: Show[Password] = (password: Password) => password.toString
  implicit val encryptedPasswordShow: Show[EncryptedPassword] = (encryptedPassword: EncryptedPassword) =>
    encryptedPassword.toString

  implicit val userNameEncoder: Encoder[UserName]                   = deriveEncoder[UserName]
  implicit val userIdEncoder: Encoder[UserId]                       = deriveEncoder[UserId]
  implicit val encryptedPasswordEncoder: Encoder[EncryptedPassword] = deriveEncoder[EncryptedPassword]

  implicit val isUserId: IsUUID[UserId] = new IsUUID[UserId] {
    def _UUID: Iso[UUID, UserId] = Iso[UUID, UserId](uuid => UserId(uuid))(userId => userId.value)
  }

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
  implicit val loginUserDecoder: Decoder[LoginUser]   = deriveDecoder[LoginUser]
  implicit val createUserDecoder: Decoder[CreateUser] = deriveDecoder[CreateUser]

  implicit val jwtTokenEncoder: Encoder[JwtToken] = deriveEncoder[JwtToken]

  implicit val claimContentDecoder: Decoder[ClaimContent] = Decoder.forProduct1("uuid")(ClaimContent.apply)
}
