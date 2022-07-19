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
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{IncorrectFormat, TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ClaimAmount, CurrencyCode}

class ClaimAmountFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val underTest = new ClaimAmountFormProvider()

  "claim amount form validation" must {

    "return no errors" when {

      "handling valid GBP form submissions" in {
        val amounts = Seq(
          "100",
          "100.00",
          "£100.00",
          "1,000,000.00",
          "£1,000,000.00",
        )

        amounts.foreach { amount =>
          validateAndCheckSuccess("GBP", amount)
        }
    }


      "handling valid EUR form submissions" in {
        val amounts = Seq(
          "100",
          "100.00",
          "€100.00",
          "1,000,000.00",
          "€1,000,000.00",
        )

        amounts.foreach { amount =>
          validateAndCheckSuccess("EUR", amount)
        }
      }

    }

    "return an amount too big error" when {

      "the amount contains too many digits" in {

        val amounts = Seq(
          "12121212121212121",
          "12112121212121.21",
          "12121212121212121",
          "1,212,121,212,121,212.10",
          "£1,212,121,212,121,212.10",
        )

        amounts.foreach { amount =>
          validateAndCheckError("GBP", amount)(Fields.ClaimAmount, TooBig)
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
        )

        amounts.foreach { amount =>
          validateAndCheckError("GBP", amount)(Fields.ClaimAmount, IncorrectFormat)
        }

      }

      "an invalid currency code is specified" in {
        validateAndCheckError("THB", "100.00")(Fields.CurrencyCode, IncorrectFormat)
      }

      "the currency prefix does not match the selected currency code" in {
        val scenarios = Seq(
          "GBP" -> "€100.00",
          "EUR" -> "£100.00",
        )

        scenarios.foreach { params =>
          validateAndCheckError(params._1, params._2)("", IncorrectFormat)
        }
      }

    }

    "return an amount too small error" when {

      "the amount is zero" in {
        validateAndCheckError("GBP", "0")(Fields.ClaimAmount, TooSmall)
      }

      "the amount is negative" in {
        validateAndCheckError("GBP", "-2")(Fields.ClaimAmount, TooSmall)
      }

    }

  }

  private def validateAndCheckSuccess(currencyCode: String, amount: String) = {
    val result = processForm(currencyCode, amount)
    val parsedAmount = amount.replaceAll("[^\\d.]*", "")
    result mustBe  Right(ClaimAmount(CurrencyCode.withName(currencyCode), parsedAmount))
  }

  private def validateAndCheckError(currencyCode: String, amount: String)(field: String, errorMessage: String) = {
    val result = processForm(currencyCode, amount)

    val foundExpectedErrorMessage = result.leftSideValue match {
      case Left(errors) => errors.contains(FormError(field, s"$errorMessage"))
      case _ => false
    }

    foundExpectedErrorMessage mustBe true withClue
      s"got result $result which did not contain expected error $errorMessage for field $field"
  }

  private def processForm(currencyCode: String, amount: String): Either[Seq[FormError], ClaimAmount] =
    underTest.form.mapping.bind(
      Map(
        Fields.CurrencyCode -> currencyCode,
        Fields.ClaimAmount -> amount
      )
    )

}
