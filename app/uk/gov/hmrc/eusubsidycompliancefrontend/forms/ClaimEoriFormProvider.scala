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
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimEoriFormProvider.Fields._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{OptionalEORI, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.getValidEori
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

case class ClaimEoriFormProvider(undertaking: Undertaking) extends FormProvider[OptionalEORI] {

  override def form: Form[OptionalEORI] = Form(mapping)

  override protected def mapping: Mapping[OptionalEORI] = Forms.mapping(
    YesNoRadioButton -> text.verifying(radioButtonSelected),
    EoriNumber -> mandatoryIfEqual(YesNoRadioButton, "true", eoriNumberMapping)
  )(OptionalEORI.apply)(OptionalEORI.unapply)

  private val eoriEntered = Constraint[String] { eori: String =>
    if (eori.isEmpty) Invalid("error.required")
    else Valid
  }

  private val enteredEoriIsValid = Constraint[String] { eori: String =>
    if (getValidEori(eori).matches(EORI.regex)) Valid
    else Invalid("error.format")
  }

  private val eoriIsPartOfUndertaking = Constraint[String] { eori: String =>
    undertaking.undertakingBusinessEntity
      .find(_.businessEntityIdentifier.toString == getValidEori(eori))
      .map(_ => Valid)
      .getOrElse(Invalid(s"error.not-in-undertaking"))
  }

  private val eoriNumberMapping: Mapping[String] =
    text
      .verifying(eoriEntered)
      .transform(e => getValidEori(e), (s: String) => s.drop(2))
      .verifying(enteredEoriIsValid)
      .verifying(eoriIsPartOfUndertaking)

  private val radioButtonSelected = Constraint[String] { r: String =>
    if (r.isEmpty) Invalid(s"error.$YesNoRadioButton.required")
    else Valid
  }

}

object ClaimEoriFormProvider {
  object Fields {
    val YesNoRadioButton = "should-claim-eori"
    val EoriNumber = "claim-eori"
  }
}
