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

import play.api.data.Forms.text
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationResult}
import play.api.data.{Forms, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Errors.{TooBig, TooSmall}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors.{IncorrectFormat, Required}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.PositiveSubsidyAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ClaimAmount, CurrencyCode, types}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.util.Try

case class ClaimAmountFormProvider(conversionRate: BigDecimal) extends FormProvider[ClaimAmount] {

  override protected def mapping: Mapping[ClaimAmount] = Forms
    .mapping(
      Fields.CurrencyCode ->
        text
          .verifying(currencyCodeIsValid)
          .transform(CurrencyCode.withName, (c: CurrencyCode) => c.entryName),
      Fields.ClaimAmountEUR -> mandatoryIfEqual(Fields.CurrencyCode, EUR.entryName, claimAmountMapping()),
      Fields.ClaimAmountGBP -> mandatoryIfEqual(Fields.CurrencyCode, GBP.entryName, claimAmountMapping(true))
    )(ClaimAmount.fromForm)(ClaimAmount.toForm)
    .verifying(claimAmountCurrencyMatchesSelection)
    .transform(c => c.copy(amount = cleanAmount(c.amount)), identity[ClaimAmount])

  private def claimAmountMapping(isGbp: Boolean = false): Mapping[String] =
    text
      .verifying(claimAmountFormatIsValid)
      .verifying(claimAmountAboveZero)
      .verifying(if (isGbp) claimAmountIsBelowMaximumAllowedValueGBP else claimAmountIsBelowMaximumAllowedValueEUR)

  private val allowedCurrencySymbols = CurrencyCode.values.map(_.symbol)

  private val claimAmountIsBelowMaximumAllowedValueEUR = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => PositiveSubsidyAmount.validateAndTransform(amount).fold[ValidationResult](Invalid(TooBig))(_ => Valid)
    )
  }

  private val claimAmountIsBelowMaximumAllowedValueGBP = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => {
        val maxValue = BigDecimal(types.MaxInputValue) * conversionRate
        if ((amount >= 0) && (amount <= maxValue) && (amount.scale <= 2)) Valid
        else Invalid(TooBig)
      }
    )
  }

  private val claimAmountFormatIsValid = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    if (claimAmount.isEmpty) {
      Invalid(Required)
    } else {
      Try(BigDecimal(amount)).fold(
        _ => Invalid(IncorrectFormat),
        amount => if (amount.scale == 2 || amount.scale == 0) Valid else Invalid(IncorrectFormat)
      )
    }
  }

  private val claimAmountAboveZero = Constraint[String] { claimAmount: String =>
    val amount = cleanAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid(IncorrectFormat),
      amount => if (amount > 0.00) Valid else Invalid(TooSmall)
    )
  }

  // Verify that the user hasn't entered a currency symbol which doesn't match the currency they selected
  private val claimAmountCurrencyMatchesSelection = Constraint[ClaimAmount] { claimAmount: ClaimAmount =>
    claimAmount.amount.head match {
      case GBP.symbol if claimAmount.currencyCode != GBP => Invalid(IncorrectFormat)
      case EUR.symbol if claimAmount.currencyCode != EUR => Invalid(IncorrectFormat)
      case c if allowedCurrencySymbols.contains(c) => Valid
      case _ => Valid
    }
  }

  private val currencyCodeIsValid = Constraint[String] { currencyCode: String =>
    if (currencyCode.isEmpty || !CurrencyCode.namesToValuesMap.contains(currencyCode)) Invalid(IncorrectFormat)
    else Valid
  }

  private def cleanAmount(a: String) =
    a.replaceAll("[£€,]*", "").replaceAll("\\s", "")

}

object ClaimAmountFormProvider {

  object Fields {
    val CurrencyCode = "currency-code"
    val ClaimAmountGBP = "claim-amount-gbp"
    val ClaimAmountEUR = "claim-amount-eur"
  }

  object Errors {
    val TooBig = "error.tooBig"
    val TooSmall = "error.tooSmall"
  }

}
