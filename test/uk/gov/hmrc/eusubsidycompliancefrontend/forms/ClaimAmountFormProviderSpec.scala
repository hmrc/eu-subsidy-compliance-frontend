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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.FormError
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ClaimAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.getValidClaimAmount

class ClaimAmountFormProviderSpec extends AnyWordSpecLike with Matchers {

  private val underTest = new ClaimAmountFormProvider()

  "claim amount form validation" must {

    "return no errors for valid GBP form submissions" in {
      val validSubmissions = Seq(
        "100.00",
        "£100.00",
        "1,000,000.00",
        "£1,000,000.00",
      )

      validSubmissions.foreach { amount =>
        validateAndCheckSuccess("GBP", amount)
      }
    }


    "return no errors for valid EUR form submissions" in {
      val validSubmissions = Seq(
        "100.00",
        "€100.00",
        "1,000,000.00",
        "€1,000,000.00",
      )

      validSubmissions.foreach { amount =>
        validateAndCheckSuccess("EUR", amount)
      }
    }

  }

  private def validateAndCheckSuccess(currencyCode: String, amount: String) = {
    val result = processForm(currencyCode, amount)
    // TODO - this should be part of the form handling, not in the subs journey
    val parsedAmount = getValidClaimAmount(amount)
    result mustBe  Right(ClaimAmount(currencyCode, parsedAmount))
  }

  private def processForm(currencyCode: String, amount: String): Either[Seq[FormError], ClaimAmount] =
    underTest.form.mapping.bind(
      Map(
        Fields.CurrencyCode -> currencyCode,
        Fields.ClaimAmount -> amount
      )
    )

}
