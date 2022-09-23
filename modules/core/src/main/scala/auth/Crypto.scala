package auth

import cats.effect.kernel.Sync
import cats.implicits.toFunctorOps
import config.Types.PasswordSalt
import domain.Auth.{ DecryptCipher, EncryptCipher, EncryptedPassword, Password }

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.spec.{ IvParameterSpec, PBEKeySpec, SecretKeySpec }
import javax.crypto.{ Cipher, SecretKeyFactory }

trait Crypto {
  def encrypt(password: Password): EncryptedPassword
  def decrypt(encryptedPassword: EncryptedPassword): Password
}

object Crypto {
  def make[F[_]: Sync](passwordSalt: PasswordSalt): F[Crypto] =
    Sync[F]
      .delay {
        val random  = new SecureRandom()
        val ivBytes = new Array[Byte](16)
        random.nextBytes(ivBytes)
        val iv       = new IvParameterSpec(ivBytes);
        val salt     = passwordSalt.secret.value.getBytes("UTF-8")
        val keySpec  = new PBEKeySpec("password".toCharArray, salt, 65536, 256)
        val factory  = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val bytes    = factory.generateSecret(keySpec).getEncoded
        val sKeySpec = new SecretKeySpec(bytes, "AES")
        val eCipher  = EncryptCipher(Cipher.getInstance("AES/CBC/PKCS5Padding"))
        eCipher.value.init(Cipher.ENCRYPT_MODE, sKeySpec, iv)
        val dCipher = DecryptCipher(Cipher.getInstance("AES/CBC/PKCS5Padding"))
        dCipher.value.init(Cipher.DECRYPT_MODE, sKeySpec, iv)
        (eCipher, dCipher)
      }
      .map {
        case (ec, dc) =>
          new Crypto {
            def encrypt(password: Password): EncryptedPassword = {
              val base64            = Base64.getEncoder
              val bytes             = password.value.getBytes("UTF-8")
              val encryptedPassword = new String(base64.encode(ec.value.doFinal(bytes)), "UTF-8")
              EncryptedPassword(encryptedPassword)
            }

            def decrypt(encryptedPassword: EncryptedPassword): Password = {
              val base64            = Base64.getDecoder
              val bytes             = base64.decode(encryptedPassword.value.getBytes("UTF-8"))
              val decryptedPassword = new String(dc.value.doFinal(bytes), "UTF-8")
              Password(decryptedPassword)
            }
          }
      }
}
