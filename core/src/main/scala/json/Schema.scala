package json

import scala.language.higherKinds

sealed trait Schema[+T] extends Product {

  private var _refName: Option[String] = None

  private var _validations: Seq[ValidationDef[_]] = Seq.empty

  def jsonType: String = productPrefix

  def withValidation(v: ValidationDef[_], vs: ValidationDef[_]*): Schema[T] = {
    this._validations = v +: vs
    this
  }

  def apply(refName: String): Schema[T] = {
    this._refName = Some(refName)
    this
  }

  def refName: Option[String] = _refName

  def validations: Seq[ValidationDef[_]] = _validations
}

object Schema {

  case object `boolean` extends Schema[Boolean]

  case object `integer` extends Schema[Int]

  case class `number`[T : Numeric]() extends Schema[T]

  case class `string`[T](format: Option[`string`.Format], pattern: Option[String]) extends Schema[T]

  object `string` {

    trait Format extends Product

    object Format {

      case object `date` extends Format

      case object `time` extends Format

      case object `date-time` extends Format // Date representation, as defined by RFC 3339, section 5.6.

      case object `email` extends Format // Internet email address, see RFC 5322, section 3.4.1.

      case object `hostname` extends Format // Internet host name, see RFC 1034, section 3.1.

      case object `ipv4` extends Format // Internet host name, see RFC 1034, section 3.1.

      case object `ipv6` extends Format // IPv6 address, as defined in RFC 2373, section 2.2.

      case object `uri` extends Format // A universal resource identifier (URI), according to RFC3986.
    }
  }

  case class `set`[T, C[_]](componentType: Schema[T]) extends Schema[C[T]] { override def jsonType = "array" }

  case class `array`[T, C[_]](componentType: Schema[T]) extends Schema[C[T]]

  case class `string-map`[T](valueType: Schema[T]) extends Schema[Map[String, T]] { override def jsonType = "object" }

  case class `int-map`[T](valueType: Schema[T]) extends Schema[Map[Int, T]] { override def jsonType = "object" }

  case class `object`[T](fields: Set[`object`.Field[_]]) extends Schema[T]

  object `object` {

    case class Field[T](name: String, tpe: Schema[T], required: Boolean = true) {

      def canEqual(that: Any): Boolean = that.isInstanceOf[Field[T]]

      override def equals(that: Any): Boolean = canEqual(that) && {
        val other = that.asInstanceOf[Field[T]]

        this.name     == other.name &&
        this.required == other.required &&
        this.tpe      == other.tpe
      }

      override def hashCode: Int = name.hashCode
    }

    def apply[T](field: Field[_], xs: Field[_]*): `object`[T] = new `object`((field +: xs.toSeq).toSet)
  }

  case class `enum`[T](values: Set[String]) extends Schema[T]

  case class $ref[T](sig: String, tpe: Schema[T]) extends Schema[T]
}