package ext

import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.collection.Size
import io.circe.Decoder

object Refined {
  implicit def validateSizeN[N <: Int, R](implicit expectedSize: ValueOf[N]): Validate.Plain[R, Size[N]] =
    Validate.fromPredicate[R, Size[N]](
      predicate => predicate.toString.length == expectedSize.value,
      _ => s"Must have ${expectedSize.value} digits",
      Size[N](expectedSize.value)
    )

  def decoderOf[T, P](implicit validate: Validate[T, P], decoder: Decoder[T]): Decoder[T Refined P] =
    decoder.emap(value => refineV[P](value))
}
