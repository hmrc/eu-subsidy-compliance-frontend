package uk.gov.hmrc.eusubsidycompliancefrontend.forms

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.Errors.{InvalidFormat, MaxLength}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.Fields.Email
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.MaximumEmailLength
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.Required
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues

class EmailFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val emailFormProvider = EmailFormProvider()
  private val optionalEmailFormProvider = OptionalEmailFormProvider()

  "EmailFormProvider" when {

    "validating an EmailForm" must {

      val testSuccess = validateAndCheckSuccess(emailFormProvider) _
      val testError = validateAndCheckError(emailFormProvider) _

      "return no errors for a valid email address" in {
        testSuccess("someone@example.com")
      }

      "return no errors for a valid email address that meets the maximum allowed length" in {
        val domain = "@example.com"
        val user = (1 to (MaximumEmailLength - domain.length)).map(_ => "l").mkString("")

        testError(s"$user$domain")
      }

      "return an error if no email is entered" in {
        testError("")(Email, Required)
      }

      "return an error for an invalid email address" in {
        testError("this is not a valid email address")(Email, InvalidFormat)
      }

      "return an error if the email address is too long" in {
        val domain = "@example.com"
        val user = (1 to 500).map(_ => "l").mkString("")
        val email = s"$user$domain"

        testError(email)(Email, MaxLength)
      }

    }

    "validating an OptionalEmailForm" must {

      val testSuccess = validateAndCheckSuccess(optionalEmailFormProvider) _
      val testError = validateAndCheckError(optionalEmailFormProvider) _

      "return no errors for a valid email address" in {
        testSuccess("someone@example.com")
      }

      "return no errors for a valid email address that meets the maximum allowed length" in {
        val domain = "@example.com"
        val user = (1 to (MaximumEmailLength - domain.length)).map(_ => "l").mkString("")

        testError(s"$user$domain")
      }

      "return an error if no email is entered" in {
        testError("")(Email, Required)
      }

      "return an error for an invalid email address" in {
        testError("this is not a valid email address")(Email, InvalidFormat)
      }

      "return an error if the email address is too long" in {
        val domain = "@example.com"
        val user = (1 to 500).map(_ => "l").mkString("")
        val email = s"$user$domain"

        testError(email)(Email, MaxLength)
      }

    }

  }

  private def validateAndCheckSuccess[A](provider: FormProvider[A])(emailAddress: String) = {
    val result = processForm[A](provider)(Map(Email -> emailAddress))
    result mustBe Right(FormValues(emailAddress))
  }

  private def validateAndCheckError[A](provider: FormProvider[A])(emailAddress: String)(errorField: String, errorMessage: String) = {
    val result = processForm[A](provider)(Map(Email -> emailAddress))

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.map(_.message).contains(errorMessage)
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $errorField"
  }

  private def processForm[A](provider: FormProvider[A])(data: Map[String, String]): Either[Seq[FormError], A] =
    provider.form.mapping.bind(data)

}
