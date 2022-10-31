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
import play.api.data.Forms.nonEmptyText
import play.api.data.validation.Constraints
import play.api.data.{Forms, Mapping}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider.{EmailAddressFieldMapping, Fields}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.mandatory
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, OptionalEmailFormInput}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

case class EmailFormProvider() extends FormProvider[FormValues] {

  override protected def mapping: Mapping[FormValues] = Forms.mapping(
      Fields.Email -> EmailAddressFieldMapping
    )(FormValues.apply)(FormValues.unapply)

}

case class OptionalEmailFormProvider() extends FormProvider[OptionalEmailFormInput] {

  override protected def mapping: Mapping[OptionalEmailFormInput] = Forms.mapping(
    Fields.UsingStoredEmail -> mandatory(Fields.UsingStoredEmail),
    Fields.Email -> mandatoryIfEqual(Fields.UsingStoredEmail, "false", EmailAddressFieldMapping)
  )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)

}

object EmailFormProvider {

  // Per RFC 3696 - Restrictions on Email addresses
  // see https://www.rfc-editor.org/errata/eid1690
  val MaximumEmailLength = 254

  val EmailAddressFieldMapping: Mapping[String] =
    nonEmptyText(maxLength = MaximumEmailLength)
      .verifying(Constraints.emailAddress)

  object Fields {
    val Email = "email"
    val UsingStoredEmail = "using-stored-email"
  }

  object Errors {
    val MaxLength = "error.maxLength"
    val InvalidFormat = "error.email"
  }

}