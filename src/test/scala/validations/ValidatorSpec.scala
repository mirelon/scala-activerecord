package com.github.aselab.activerecord

import org.specs2.mutable._
import org.specs2.mock._

object ValidatorSpec extends Specification with Mockito {
  import annotation.target._
  type CustomAnnotation = sample.CustomAnnotation @field

  val customValidator =  new Validator[sample.CustomAnnotation] {
    def validate(value: Any) =
      if (value != annotation.value) errors.add(fieldName, "custom")
  }

  def mockAnnotation[T <: Validator.AnnotationType](
    on: String = "save"
  )(implicit m: Manifest[T]) = {
    val a = mock[T]
    a.message returns ""
    a.on returns on
    a
  }

  def validate[A <: Validator.AnnotationType, T <: Validator[A]](validator: T, a: A, m: Model) =
    validator.validateWith(m.value, a, m, "value")

  case class Model(value: Any, isNewInstance: Boolean = true) extends Validatable {
    var valueConfirmation = value
    var other = value
  }
  val modelClass = classOf[Model]

  "Validator" should {
    "be able to register and unregister custom validator" in {
      val c = classOf[sample.CustomAnnotation]

      Validator.get(c) must beNone
      customValidator.register
      Validator.get(c) must beSome(customValidator)
      customValidator.unregister
      Validator.get(c) must beNone
    }

    "validateWith" in {
      "on save" in {
        val a = mockAnnotation[sample.CustomAnnotation](on="save")
        a.value returns "match"

        val onCreate = Model("", true)
        validate(customValidator, a, onCreate)
        onCreate.errors must not beEmpty

        val onUpdate = Model("", false)
        validate(customValidator, a, onUpdate)
        onUpdate.errors must not beEmpty
      }
      
      "on create" in {
        val a = mockAnnotation[sample.CustomAnnotation](on="create")
        a.value returns "match"

        val onCreate = Model("", true)
        validate(customValidator, a, onCreate)
        onCreate.errors must not beEmpty

        val onUpdate = Model("", false)
        validate(customValidator, a, onUpdate)
        onUpdate.errors must beEmpty
      }
      
      "on update" in {
        val a = mockAnnotation[sample.CustomAnnotation](on="update")
        a.value returns "match"

        val onCreate = Model("", true)
        validate(customValidator, a, onCreate)
        onCreate.errors must beEmpty

        val onUpdate = Model("", false)
        validate(customValidator, a, onUpdate)
        onUpdate.errors must not beEmpty
      }
    }

    "requiredValidator" in {
      val validator = Validator.requiredValidator
      def a = mockAnnotation[annotations.Required]()
        
      "invalid if value is null" in {
        val m = Model(null)
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "required"))
      }

      "invalid if value is empty string" in {
        val m = Model("")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "required"))
      }

      "valid if value is not empty" in {
        val m = Model("a")
        validate(validator, a, m)
        m.errors must beEmpty
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("")
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "lengthValidator" in {
      val validator = Validator.lengthValidator
      def a = {
        val a = mockAnnotation[annotations.Length]()
        a.min returns 2
        a.max returns 4
        a
      }
        
      "skip if value is null or empty string" in {
        val m1 = Model(null)
        val m2 = Model("")
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "valid if length is within min to max" in {
        val m1 = Model("aa")
        val m2 = Model("aaaa")
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "invalid if length is shorter than min value" in {
        val m = Model("a")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "minLength", 2)).only
      }

      "invalid if length is longer than max value" in {
        val m = Model("aaaaa")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "maxLength", 4)).only
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("a")
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "rangeValidator" in {
      val validator = Validator.rangeValidator
      def a = {
        val a = mockAnnotation[annotations.Range]()
        a.min returns -2
        a.max returns 2
        a
      }
       
      "skip if value is not number" in {
        val m1 = Model(null)
        val m2 = Model("test")
        val m3 = Model(new Object)
        validate(validator, a, m1)
        validate(validator, a, m2)
        validate(validator, a, m3)
        m1.errors must beEmpty
        m2.errors must beEmpty
        m3.errors must beEmpty
      }

      "valid if value is within min to max" in {
        val m1 = Model(-2)
        val m2 = Model(2)
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "invalid if value is less than min value" in {
        val m = Model(-2.01)
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "minValue", -2)).only
      }

      "invalid if value is greater than max value" in {
        val m = Model(2.01)
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "maxValue", 2)).only
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model(33)
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "acceptedValidator" in {
      val validator = Validator.acceptedValidator
      def a = mockAnnotation[annotations.Accepted]()
        
      "skip if value is not Boolean" in {
        val m1 = Model(null)
        val m2 = Model("aaa")
        val m3 = Model(new Object)
        validate(validator, a, m1)
        validate(validator, a, m2)
        validate(validator, a, m3)
        m1.errors must beEmpty
        m2.errors must beEmpty
        m3.errors must beEmpty
      }

      "valid if value is true" in {
        val m = Model(true)
        validate(validator, a, m)
        m.errors must beEmpty
      }

      "invalid if value is false" in {
        val m = Model(false)
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "accepted"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model(false)
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "emailValidator" in {
      val validator = Validator.emailValidator
      def a = mockAnnotation[annotations.Email]()
        
      "skip if value is null or empty string" in {
        val m1 = Model(null)
        val m2 = Model("")
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "valid email" in {
        val m = Model("user@some.com")
        validate(validator, a, m)
        m.errors must beEmpty
      }

      "invalid email" in {
        val m = Model("aaa")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "invalid"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("aaa")
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "formatValidator" in {
      val validator = Validator.formatValidator
      def a = {
        val a = mockAnnotation[annotations.Format]()
        a.value returns "^[a-z]+$"
        a
      }
        
      "skip if value is null or empty string" in {
        val m1 = Model(null)
        val m2 = Model("")
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "valid format" in {
        val m = Model("abcxyz")
        validate(validator, a, m)
        m.errors must beEmpty
      }

      "invalid format" in {
        val m = Model("a123z")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "format"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("123")
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test"))
      }
    }

    "confirmationValidator" in {
      val validator = Validator.confirmationValidator
      def a = mockAnnotation[annotations.Confirmation]()
      
      "skip if value is null or empty string" in {
        val m1 = Model(null)
        val m2 = Model("")
        m1.valueConfirmation = "aaa"
        m2.valueConfirmation = "aaa"
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "equals confirmation field" in {
        val m = Model("aaa")
        validate(validator, a, m)
        m.errors must beEmpty
      }

      "not equals confirmation field" in {
        val m = Model("aaa")
        m.valueConfirmation = "zzz"
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "valueConfirmation", "confirmation"))
      }

      "change confirmation field" in {
        val ac = a
        ac.value returns "other"
        val m = Model("aaa")
        m.other = "zzz"
        validate(validator, ac, m)
        m.errors must contain(ValidationError(modelClass, "other", "confirmation"))
      }

      "throws exception when confirmation field does not exists" in {
        val ac = a
        ac.value returns "notExists"
        val m = Model("aaa")
        validate(validator, ac, m) must throwA(ActiveRecordException.notfoundConfirmationField("notExists"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("aaa")
        m.valueConfirmation = "zzz"
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "valueConfirmation", "test"))
      }
    }

    "stringEnumValidator" in {
      val validator = Validator.stringEnumValidator
      def a = {
        val a = mockAnnotation[annotations.StringEnum]()
        a.value returns Array("a", "b")
        a
      }
        
      "valid if it is enumerated value" in {
        val m1 = Model("a")
        val m2 = Model("b")
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "invalid if it is not enumerated value" in {
        val m = Model("c")
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "enum", "a, b"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model("c")
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test", "a, b"))
      }
    }

    "numberEnumValidator" in {
      val validator = Validator.numberEnumValidator
      def a = {
        val a = mockAnnotation[annotations.NumberEnum]()
        a.value returns Array(2, 3)
        a
      }
        
      "valid if it is enumerated value" in {
        val m1 = Model(2)
        val m2 = Model(3)
        validate(validator, a, m1)
        validate(validator, a, m2)
        m1.errors must beEmpty
        m2.errors must beEmpty
      }

      "invalid if it is not enumerated value" in {
        val m = Model(1)
        validate(validator, a, m)
        m.errors must contain(ValidationError(modelClass, "value", "enum", "2, 3"))
      }

      "annotation message" in {
        val am = a
        am.message returns "test"
        val m = Model(1)
        validate(validator, am, m)
        m.errors must contain(ValidationError(modelClass, "value", "test", "2, 3"))
      }
    }

  }
}

