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
  signOut: SignOutFormPage = SignOutFormPage(),
) extends Journey {

  private val journeySteps = List(
    customsWaivers,
    willYouClaim,
    notEligible,
    eoriCheck,
    signOutBadEori,
    signOut,
  )

  override def steps: Array[FormPage[_]] =
    journeySteps
      // Remove steps based on user responses during the eligibility journey.
      .filterNot {
        case CustomsWaiversFormPage(_) => true
        case WillYouClaimFormPage(_) => true
        case NotEligibleFormPage(_) => true
        case SignOutFormPage(_) => eoriCheck.value.contains(true)
        case SignOutBadEoriFormPage(_) => eoriCheck.value.contains(true)
        case _ => false
      }.toArray

  def setEoriCheck(newEoriCheck: Boolean): EligibilityJourney =
    this.copy(eoriCheck = eoriCheck.copy(value = Some(newEoriCheck)))

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
    case class SignOutFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getNotEligibleToLead().url
    }
    case class EoriCheckFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getEoriCheck().url
    }
    case class SignOutBadEoriFormPage(value: Form[Boolean] = None) extends FormPage[Boolean] {
      def uri = controller.getIncorrectEori().url
    }

    object CustomsWaiversFormPage {implicit val customsWaiversFormPageFormat: OFormat[CustomsWaiversFormPage] = Json.format}
    object WillYouClaimFormPage { implicit val willYouClaimFormPageFormat: OFormat[WillYouClaimFormPage] = Json.format }
    object NotEligibleFormPage { implicit val notEligibleFormPageFormat: OFormat[NotEligibleFormPage] = Json.format }
    object SignOutBadEoriFormPage { implicit val signOutFormPageFormat: OFormat[SignOutBadEoriFormPage] = Json.format }
    object EoriCheckFormPage { implicit val eoriCheckFormPageFormat: OFormat[EoriCheckFormPage] = Json.format }
    object SignOutFormPage { implicit val signOutFormPageFormat: OFormat[SignOutFormPage] = Json.format }

  }

}
