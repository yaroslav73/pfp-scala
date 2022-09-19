package auth

import domain.Auth.{ EncryptedPassword, Password }

trait Crypto {
  def encrypt(password: Password): EncryptedPassword
  def decrypt(encryptedPassword: EncryptedPassword): Password
}
