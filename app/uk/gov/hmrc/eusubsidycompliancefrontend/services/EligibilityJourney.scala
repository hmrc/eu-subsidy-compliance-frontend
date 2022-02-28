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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes

case class EligibilityJourney(
  customsWaivers: CustomsWaiversFormPage = CustomsWaiversFormPage(),
  willYouClaim: WillYouClaimFormPage = WillYouClaimFormPage(),
  notEligible: NotEligibleFormPage = NotEligibleFormPage(),
  mainBusinessCheck: MainBusinessCheckFormPage = MainBusinessCheckFormPage(),
  signOut: SignOutFormPage = SignOutFormPage(),
  acceptTerms: AcceptTermsFormPage = AcceptTermsFormPage(),
  eoriCheck: EoriCheckFormPage = EoriCheckFormPage(),
  signOutBadEori: SignOutBadEoriFormPage = SignOutBadEoriFormPage(),
  createUndertaking: CreateUndertakingFormPage = CreateUndertakingFormPage(),
) extends Journey {

  private val journeySteps = List(
    customsWaivers,
    willYouClaim,
    notEligible,
    mainBusinessCheck,
    signOut,
    acceptTerms,
    eoriCheck,
    signOutBadEori,
    createUndertaking
  )

  override protected def steps: List[FormPageBase[_]] =
    journeySteps
      .filterNot(removeWillYouClaimIfDoYouClaimTrue)
      .filterNot(removeNotEligibleIfCustomsWaiversClaimed)
      .filterNot(removeSignOutIfMainBusinessCheckPassed)
      .filterNot(removeSignOutBadEoriIfEoriCheckPassed)

  private def removeWillYouClaimIfDoYouClaimTrue(f: FormPageBase[_]) =
    predicate(f, WillYouClaim)(customsWaivers.value.contains(true))

  private def removeNotEligibleIfCustomsWaiversClaimed(f: FormPageBase[_]) =
    predicate(f, NotEligible)(Seq(customsWaivers.value, willYouClaim.value).flatten.contains(true))

  private def removeSignOutIfMainBusinessCheckPassed(f: FormPageBase[_]) =
    predicate(f, SignOut)(mainBusinessCheck.value.contains(true))

  private def removeSignOutBadEoriIfEoriCheckPassed(f: FormPageBase[_]) =
    predicate(f, SignOutBadEori)(eoriCheck.value.contains(true))

  // TODO - review this - match on types instead of uri string?
  private def predicate(f: FormPageBase[_], uri: String)(p: Boolean) = f.uri == uri && p

}

object EligibilityJourney {
  import Journey._ // N.B. don't let intellij delete this

  implicit val format: Format[EligibilityJourney] = Json.format[EligibilityJourney]

  // TODO - consider introducing form classes for each page
  object FormUrls {
    val CustomsWaivers = routes.EligibilityController.getCustomsWaivers().url
    val WillYouClaim = routes.EligibilityController.getWillYouClaim().url
    val NotEligible = routes.EligibilityController.getNotEligible().url
    val MainBusinessCheck = routes.EligibilityController.getMainBusinessCheck().url
    val SignOut = routes.EligibilityController.getNotEligibleToLead().url
    val AcceptTerms = routes.EligibilityController.getTerms().url
    val EoriCheck = routes.EligibilityController.getEoriCheck().url
    val SignOutBadEori = routes.EligibilityController.getIncorrectEori().url
    val CreateUndertaking = routes.EligibilityController.getCreateUndertaking().url
  }

  object Forms {
    case class CustomsWaiversFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.CustomsWaivers}
    case class WillYouClaimFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.WillYouClaim}
    case class NotEligibleFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.NotEligible}
    case class MainBusinessCheckFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.MainBusinessCheck}
    case class SignOutFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.SignOut}
    case class AcceptTermsFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.AcceptTerms}
    case class EoriCheckFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.EoriCheck}
    case class SignOutBadEoriFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.SignOutBadEori}
    case class CreateUndertakingFormPage(value: Form[Boolean] = None) extends FormPageBase[Boolean] { val uri = FormUrls.CreateUndertaking}

    object CustomsWaiversFormPage { implicit val customsWaiversFormPageFormat: OFormat[CustomsWaiversFormPage] = Json.format }
    object WillYouClaimFormPage { implicit val willYouClaimFormPageFormat: OFormat[WillYouClaimFormPage] = Json.format }
    object NotEligibleFormPage { implicit val notEligibleFormPageFormat: OFormat[NotEligibleFormPage] = Json.format }
    object MainBusinessCheckFormPage { implicit val mainBusinessCheckFormPageFormat: OFormat[MainBusinessCheckFormPage] = Json.format }
    object SignOutBadEoriFormPage { implicit val signOutFormPageFormat: OFormat[SignOutBadEoriFormPage] = Json.format }
    object AcceptTermsFormPage { implicit val acceptTermsFormPageFormat: OFormat[AcceptTermsFormPage] = Json.format }
    object EoriCheckFormPage { implicit val eoriCheckFormPageFormat: OFormat[EoriCheckFormPage] = Json.format }
    object SignOutFormPage { implicit val signOutFormPageFormat: OFormat[SignOutFormPage] = Json.format }
    object CreateUndertakingFormPage { implicit val createUndertakingFormPageFormat: OFormat[CreateUndertakingFormPage] = Json.format }
  }

}
