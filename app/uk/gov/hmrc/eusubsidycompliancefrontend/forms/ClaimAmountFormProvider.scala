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

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Forms, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{IncorrectFormat, Required, TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ClaimAmount

import scala.util.Try

case class ClaimAmountFormProvider() extends FormProvider[ClaimAmount] {

  override protected def mapping: Mapping[ClaimAmount] = Forms.mapping(
    Fields.CurrencyCode ->
      text
        .verifying(currencyCodeIsValid),
    Fields.ClaimAmount ->
      text
        .verifying(claimAmountIsBelowMaximumLength)
        .verifying(claimAmountFormatIsValid)
        .verifying(claimAmountAboveMinimumAllowedValue)
  )(ClaimAmount.apply)(ClaimAmount.unapply)
    .verifying(claimAmountCurrencyMatchesSelection)
    .transform(c => c.copy(amount = cleanAmount(c.amount)), identity[ClaimAmount])

  override def form: Form[ClaimAmount] = Form(mapping)

  private val claimAmountIsBelowMaximumLength = Constraint[String] { claimAmount: String =>
    if (cleanAmount(claimAmount).length < 17) Valid else Invalid("error.tooBig")
  }

  private val claimAmountFormatIsValid = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid("error.incorrectFormat"),
      amount => if (amount.scale == 2 || amount.scale == 0) Valid else Invalid("error.incorrectFormat")
    )
  }

  private val claimAmountAboveMinimumAllowedValue = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => if (amount > 0.01) Valid else Invalid(TooSmall)
    )
  }

  // Verify that the user hasn't entered a currency symbol which doesn't match the currency they selected
  private val claimAmountCurrencyMatchesSelection = Constraint[ClaimAmount] { claimAmount: ClaimAmount =>
    claimAmount.amount.head match {
      case '£' if claimAmount.currencyCode != "GBP" => Invalid(IncorrectFormat)
      case '€' if claimAmount.currencyCode != "EUR" => Invalid(IncorrectFormat)
      case c if Seq('£', '€').contains(c) => Valid
      case c if !c.isDigit => Invalid(IncorrectFormat)
      case _ => Valid
    }
  }

  // TODO - use an enum for currency codes
  private val ValidCurrencyCodes = List("GBP", "EUR")

  private val currencyCodeIsValid = Constraint[String] { currencyCode: String =>
    if (currencyCode.isEmpty || !ValidCurrencyCodes.contains(currencyCode)) Invalid(IncorrectFormat)
    else Valid
  }

  private def cleanAmount(a: String) =
    a.replaceAll("[£€,]*", "").replaceAll("\\s", "")

}

object ClaimAmountFormProvider {

  object Fields {
    val CurrencyCode = "currency-code"
    val ClaimAmount = "claim-amount"
  }

  // TODO - share these across form providers?
  object Errors {
    val IncorrectFormat = "error.incorrectFormat"
    val Required = "error.required"
    val TooBig = "error.tooBig"
    val TooSmall = "error.tooSmall"
  }

}
