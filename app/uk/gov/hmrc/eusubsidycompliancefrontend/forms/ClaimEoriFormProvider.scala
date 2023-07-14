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
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Forms, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimEoriFormProvider.Fields._
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimEoriFormProvider.{fromOptionalClaimEori, toOptionalClaimEori}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormProvider.CommonErrors._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.withGbPrefix
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{OptionalClaimEori, Undertaking}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

case class ClaimEoriFormProvider(undertaking: Undertaking) extends FormProvider[OptionalClaimEori] {

  override protected def mapping: Mapping[OptionalClaimEori] = Forms.mapping(
    YesNoRadioButton -> text,
    EoriNumber -> mandatoryIfEqual(YesNoRadioButton, "true", eoriNumberMapping)
  )(toOptionalClaimEori)(fromOptionalClaimEori)

  private val isEoriLengthValid = Constraint[String] { eori: String =>
    if (EORI.ValidLengthsWithPrefix.contains(removeSpaces(eori).length)) Valid
    else Invalid(IncorrectLength)
  }

  private def removeSpaces(eori: String) = {
    eori.replaceAll(" ", "")
  }

  private val eoriEntered = Constraint[String] { eori: String =>
    if (eori.isEmpty) Invalid(Required)
    else Valid
  }

  private val enteredEoriIsValid = Constraint[String] { eori: String =>
    if (removeSpaces(eori).matches(EORI.regex)) Valid
    else Invalid(IncorrectFormat)
  }

  private val eoriNumberMapping: Mapping[String] =
    text
      .verifying(eoriEntered)
      .verifying(isEoriLengthValid)
      .transform(e => e, (s: String) => s)
      .verifying(enteredEoriIsValid)

}

object ClaimEoriFormProvider {

  object Fields {
    val YesNoRadioButton = "should-claim-eori"
    val EoriNumber = "claim-eori"
  }

  object Errors {
    val InAnotherUndertaking = "error.in-another-undertaking"
  }

  def toOptionalClaimEori(s: String, v: Option[String]) = OptionalClaimEori(s, v)

  def fromOptionalClaimEori(oe: OptionalClaimEori): Option[(String, Option[String])] =
    OptionalClaimEori.unapply(oe).map(result => (result._1, result._2))

}
