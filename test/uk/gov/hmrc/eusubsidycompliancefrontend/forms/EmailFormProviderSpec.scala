/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eusubsidycompliancefrontend.forms

import org.scalatest.AppendedClues.convertToClueful
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.Errors.{InvalidFormat, MaxLength}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.Fields.Email
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.MaximumEmailLength
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.Required
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec

class EmailFormProviderSpec extends BaseSpec with Matchers {

  "EmailFormProvider" when {

    "validating an EmailForm" must {

      val underTest = EmailFormProvider()

      def testSuccess(form: (String, String)*)(expected: FormValues) =
        validateAndCheckSuccess(underTest)(form: _*)(expected)

      def testError(form: (String, String)*)(field: String, errorMessage: String) =
        validateAndCheckError(underTest)(form: _*)(field, errorMessage)

      "return no errors for a valid email address" in {
        testSuccess(Email -> "someone@example.com")(FormValues("someone@example.com"))
      }

      "return no errors for a valid email address that meets the maximum allowed length" in {
        val domain = "@example.com"
        val user = (1 to (MaximumEmailLength - domain.length)).map(_ => "l").mkString("")

        testSuccess(Email -> s"$user$domain")(FormValues(s"$user$domain"))
      }

      "return an error if no email is entered" in {
        testError(Email -> "")(Email, Required)
      }

      "return an error for an invalid email address" in {
        testError(Email -> "this is not a valid email address")(Email, InvalidFormat)
      }

      "return an error if the email address is too long" in {
        val domain = "@example.com"
        val user = (1 to 500).map(_ => "l").mkString("")
        val email = s"$user$domain"

        testError(Email -> email)(Email, MaxLength)
      }

    }

  }

  private def validateAndCheckSuccess[A](provider: FormProvider[A])(form: (String, String)*)(expected: A) = {
    val result = processForm[A](provider)(form: _*)
    result mustBe Right(expected)
  }

  private def validateAndCheckError[A](
    provider: FormProvider[A]
  )(form: (String, String)*)(errorField: String, errorMessage: String) = {
    val result = processForm[A](provider)(form: _*)

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.map(_.message).contains(errorMessage)
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $errorField"
  }

  private def processForm[A](provider: FormProvider[A])(form: (String, String)*): Either[Seq[FormError], A] =
    provider.form.mapping.bind(form.toMap)

}
