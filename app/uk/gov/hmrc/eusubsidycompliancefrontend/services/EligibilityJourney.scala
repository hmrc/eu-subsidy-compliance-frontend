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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form

case class EligibilityJourney(
  customsWaivers: CustomsWaiversFormPage = CustomsWaiversFormPage(),
  willYouClaim: WillYouClaimFormPage = WillYouClaimFormPage(),
  notEligible: NotEligibleFormPage = NotEligibleFormPage(),
  eoriCheck: EoriCheckFormPage = EoriCheckFormPage(),
  signOutBadEori: SignOutBadEoriFormPage = SignOutBadEoriFormPage(),
  mainBusinessCheck: MainBusinessCheckFormPage = MainBusinessCheckFormPage(),
  signOut: SignOutFormPage = SignOutFormPage(),
  acceptTerms: AcceptTermsFormPage = AcceptTermsFormPage(),
  createUndertaking: CreateUndertakingFormPage = CreateUndertakingFormPage()
) extends Journey {

  private val journeySteps = List(
    customsWaivers,
    willYouClaim,
    notEligible,
    eoriCheck,
    signOutBadEori,
    mainBusinessCheck,
    signOut,
    acceptTerms,
    createUndertaking
  )

  override def steps: Array[FormPage[_]] =
    journeySteps
      // Remove steps based on user responses during the eligibility journey.
      .filterNot {
        case CustomsWaiversFormPage(_) => true
        case WillYouClaimFormPage(_) => true
        case NotEligibleFormPage(_) => true
        case SignOutFormPage(_) => mainBusinessCheck.value.contains(true)
        case SignOutBadEoriFormPage(_) => eoriCheck.value.contains(true)
        case _ => false
      }.toArray

  def setMainBusinessCheck(newMainBusinessCheck: Boolean): EligibilityJourney =
    this.copy(mainBusinessCheck = mainBusinessCheck.copy(value = Some(newMainBusinessCheck)))

  def setAcceptTerms(newAcceptTerms: Boolean): EligibilityJourney =
    this.copy(acceptTerms = acceptTerms.copy(value = Some(newAcceptTerms)))

  def setEoriCheck(newEoriCheck: Boolean): EligibilityJourney =
    this.copy(eoriCheck = eoriCheck.copy(value = Some(newEoriCheck)))

  def setCreateUndertaking(newCreateUndertaking: Boolean): EligibilityJourney =
    this.copy(createUndertaking = createUndertaking.copy(value = Some(newCreateUndertaking)))
}

object EligibilityJourney {

  implicit val format: Format[EligibilityJourney] = Json.format[EligibilityJourney]

  object Forms {

    private val controller = routes.EligibilityController

    case class CustomsWaiversFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCustomsWaivers().url
    }
    case class WillYouClaimFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getWillYouClaim().url
    }
    case class NotEligibleFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getNotEligible().url
    }
    case class MainBusinessCheckFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getMainBusinessCheck().url
    }
    case class SignOutFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getNotEligibleToLead().url
    }
    case class AcceptTermsFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getTerms().url
    }
    case class EoriCheckFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getEoriCheck().url
    }
    case class SignOutBadEoriFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getIncorrectEori().url
    }
    case class CreateUndertakingFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getCreateUndertaking().url
    }

    object CustomsWaiversFormPage {
      implicit val customsWaiversFormPageFormat: OFormat[CustomsWaiversFormPage] = Json.format
    }
    object WillYouClaimFormPage { implicit val willYouClaimFormPageFormat: OFormat[WillYouClaimFormPage] = Json.format }
    object NotEligibleFormPage { implicit val notEligibleFormPageFormat: OFormat[NotEligibleFormPage] = Json.format }
    object MainBusinessCheckFormPage {
      implicit val mainBusinessCheckFormPageFormat: OFormat[MainBusinessCheckFormPage] = Json.format
    }
    object SignOutBadEoriFormPage { implicit val signOutFormPageFormat: OFormat[SignOutBadEoriFormPage] = Json.format }
    object AcceptTermsFormPage { implicit val acceptTermsFormPageFormat: OFormat[AcceptTermsFormPage] = Json.format }
    object EoriCheckFormPage { implicit val eoriCheckFormPageFormat: OFormat[EoriCheckFormPage] = Json.format }
    object SignOutFormPage { implicit val signOutFormPageFormat: OFormat[SignOutFormPage] = Json.format }
    object CreateUndertakingFormPage {
      implicit val createUndertakingFormPageFormat: OFormat[CreateUndertakingFormPage] = Json.format
    }

  }

}
