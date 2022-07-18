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
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimAmountFormProvider.Fields.CurrencyCode
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ClaimAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.getValidClaimAmount

import scala.util.Try

case class ClaimAmountFormProvider() extends FormProvider[ClaimAmount] {

  override protected def mapping: Mapping[ClaimAmount] = Forms.mapping(
    Fields.CurrencyCode ->
      text
        .verifying(radioButtonSelected),
    Fields.ClaimAmount ->
      text
        .verifying(isClaimAmountTooBig)
        .verifying(isClaimAmountFormatCorrect)
        .verifying(isClaimAmountTooSmall)
        // TODO - temporary fudge until the code is moved out of the SubsidyJourney
        .transform(getValidClaimAmount, identity[String])
  )(ClaimAmount.apply)(ClaimAmount.unapply)

  override def form: Form[ClaimAmount] = Form(mapping)

  private val isClaimAmountTooBig = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    if (amount.length < 17) Valid else Invalid("error.tooBig")
  }

  private val isClaimAmountFormatCorrect = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid("error.incorrectFormat"),
      amount => if (amount.scale == 2 || amount.scale == 0) Valid else Invalid("error.incorrectFormat")
    )
  }

  private val isClaimAmountTooSmall = Constraint[String] { claimAmount: String =>
    val amount = getValidClaimAmount(claimAmount)
    Try(BigDecimal(amount)).fold(
      _ => Invalid("error.incorrectFormat"),
      amount => if (amount > 0.01) Valid else Invalid("error.tooSmall")
    )
  }

  // TODO - can this be shared?
  // TODO - test coverage of this in provider spec?
  private val radioButtonSelected = Constraint[String] { r: String =>
    println(s"Radio Button Selected: ${r.nonEmpty}")
    // TODO - do we need to use the fieldname here?
    if (r.isEmpty) Invalid(s"$CurrencyCode.error.required")
    else Valid
  }

}

object ClaimAmountFormProvider {
  object Fields {
    val CurrencyCode = "currency-code"
    val ClaimAmount = "claim-amount"
  }
}
