/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.IncorrectFormat
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ClaimAmount, CurrencyCode}

class ClaimAmountFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val underTest = new ClaimAmountFormProvider()

  "claim amount form validation" must {

    "return no errors" when {

      "handling valid GBP form submissions" in {
        val amounts = Seq(
          "100",
          "0.01",
          "£0.01",
          "100.00",
          "£100.00",
          "10  0.00",
          "£10  0.00",
          "100,000.00",
          "£100,000.00",
        )

        amounts.foreach { amount =>
          validateAndCheckSuccess(GBP, amount)
        }
    }


      "handling valid EUR form submissions" in {
        val amounts = Seq(
          "100",
          "0.01",
          "€0.01",
          "100.00",
          "€100.00",
          "10  0.00",
          "€10  0.00",
          "100,000.00",
          "€100,000.00",
        )

        amounts.foreach { amount =>
          validateAndCheckSuccess(EUR, amount)
        }
      }

    }

    "return an amount too big error" when {

      "the amount is larger than the maximum allowed value" in {

        val amounts = Seq(
          "12121212121212121",
          "12112121212121.21",
          "12121212121212121",
          "1,212,121,212,121,212.10",
          "£1,212,121,212,121,212.10",
          "9999999999999",
          "9999999999999.00",
        )

        amounts.foreach { amount =>
          validateAndCheckError(GBP.entryName, amountGBP = amount)(Fields.ClaimAmountGBP, TooBig)
        }

      }

    }

    "return an incorrect format error" when {

      "the amount does not confirm to the required format" in {
        val amounts = Seq(
          "£1,000,000.00.00",
          "€1,000,000.00.00",
          "this is definitely not a number",
          "$100.00",
          "A100.00",
        )

        amounts.foreach { amount =>
          validateAndCheckError(GBP.entryName, amountGBP = amount)(Fields.ClaimAmountGBP, IncorrectFormat)
        }

      }

      "an invalid currency code is specified" in {
        validateAndCheckError("THB", "100.00")(Fields.CurrencyCode, IncorrectFormat)
      }

      "the currency prefix does not match the selected currency code" in {
        validateAndCheckError("GBP", amountGBP = "€100.00")("", IncorrectFormat)
        validateAndCheckError("EUR", amountEUR = "£100.00")("", IncorrectFormat)
      }

    }

    "return an amount too small error" when {

      "the amount is zero" in {
        validateAndCheckError("GBP", amountGBP = "0")(Fields.ClaimAmountGBP, TooSmall)
      }

      "the amount is negative" in {
        validateAndCheckError("GBP", amountGBP = "-2")(Fields.ClaimAmountGBP, TooSmall)
      }

    }

  }

  private def validateAndCheckSuccess(currencyCode: CurrencyCode, amount: String) = {
    val result = currencyCode match {
      case EUR => processForm(currencyCode.entryName, amount, "")
      case GBP => processForm(currencyCode.entryName, "", amount)
    }
    val parsedAmount = amount.replaceAll("[^\\d.]*", "")
    result mustBe  Right(ClaimAmount(currencyCode, parsedAmount))
  }

  private def validateAndCheckError(currencyCode: String, amountEUR: String = "", amountGBP: String = "")(field: String, errorMessage: String) = {
    val result = processForm(currencyCode, amountEUR, amountGBP)

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError(field, s"$errorMessage"))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $field"
  }

  private def processForm(currencyCode: String, amountEUR: String, amountGBP: String): Either[Seq[FormError], ClaimAmount] =
    underTest.form.mapping.bind(
      Map(
        Fields.CurrencyCode -> currencyCode,
        Fields.ClaimAmountEUR -> amountEUR,
        Fields.ClaimAmountGBP -> amountGBP,
      )
    )

}
