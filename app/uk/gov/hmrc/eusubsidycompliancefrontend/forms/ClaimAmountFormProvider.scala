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
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{IncorrectFormat, TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ClaimAmount, CurrencyCode}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.util.Try

case class ClaimAmountFormProvider() extends FormProvider[ClaimAmount] {

  // TODO - clean this up if the apply/unapply stuff works
  override protected def mapping: Mapping[ClaimAmount] = Forms.mapping(
    Fields.CurrencyCode ->
      text
        .verifying(currencyCodeIsValid)
        .transform(CurrencyCode.withName, (c: CurrencyCode) => c.entryName),
    Fields.ClaimAmountEUR -> mandatoryIfEqual(Fields.CurrencyCode, EUR.entryName, claimAmountMapping),
    Fields.ClaimAmountGBP -> mandatoryIfEqual(Fields.CurrencyCode, GBP.entryName, claimAmountMapping),
  )(ClaimAmount.fromForm)(ClaimAmount.toForm)
    .verifying(claimAmountCurrencyMatchesSelection)
    .transform(c => c.copy(amount = cleanAmount(c.amount)), identity[ClaimAmount])

  override def form: Form[ClaimAmount] = Form(mapping)

  private def claimAmountMapping: Mapping[String] =
    text
      .verifying(claimAmountIsBelowMaximumLength)
      .verifying(claimAmountFormatIsValid)
      .verifying(claimAmountAboveMinimumAllowedValue)

  private val allowedCurrencySymbols = CurrencyCode.values.map(_.symbol)

  private val claimAmountIsBelowMaximumLength = Constraint[String] { claimAmount: String =>
    println(s"validating claim amount length: $claimAmount")
    if (cleanAmount(claimAmount).length < 17) Valid else Invalid(TooBig)
  }

  private val claimAmountFormatIsValid = Constraint[String] { claimAmount: String =>
    println(s"validating claim amount format: $claimAmount")
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => if (amount.scale == 2 || amount.scale == 0) Valid else Invalid(IncorrectFormat)
    )
  }

  private val claimAmountAboveMinimumAllowedValue = Constraint[String] { claimAmount: String =>
    println(s"validating claim amount above minimum: $claimAmount")
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => if (amount > 0.01) Valid else Invalid(TooSmall)
    )
  }

  // Verify that the user hasn't entered a currency symbol which doesn't match the currency they selected
  private val claimAmountCurrencyMatchesSelection = Constraint[ClaimAmount] { claimAmount: ClaimAmount =>
    println(s"validating amount and currency code match: $claimAmount")
    claimAmount.amount.head match {
      case GBP.symbol if claimAmount.currencyCode != GBP => Invalid(IncorrectFormat)
      case EUR.symbol if claimAmount.currencyCode != EUR => Invalid(IncorrectFormat)
      case c if allowedCurrencySymbols.contains(c) => Valid
      case c if !c.isDigit => Invalid(IncorrectFormat)
      case _ => Valid
    }
  }

  private val currencyCodeIsValid = Constraint[String] { currencyCode: String =>
    println(s"validating currency code: $currencyCode")
    if (currencyCode.isEmpty || !CurrencyCode.namesToValuesMap.contains(currencyCode)) Invalid(IncorrectFormat)
    else Valid
  }

  private def cleanAmount(a: String) =
    a.replaceAll("[£€,]*", "").replaceAll("\\s", "")

}

object ClaimAmountFormProvider {

  object Fields {
    val CurrencyCode = "currency-code"
    val ClaimAmount = "claim-amount"
    val ClaimAmountGBP = "claim-amount-gbp"
    val ClaimAmountEUR = "claim-amount-eur"
  }

  // TODO - share these across form providers?
  object Errors {
    val IncorrectFormat = "error.incorrectFormat"
    val Required = "error.required"
    val TooBig = "error.tooBig"
    val TooSmall = "error.tooSmall"
  }

}
