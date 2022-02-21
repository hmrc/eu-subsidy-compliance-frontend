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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.FormUrls._

case class EligibilityJourney(
  customsWaivers: FormPage[Boolean] = FormPage(CustomsWaivers),
  willYouClaim: FormPage[Boolean] = FormPage(WillYouClaim),
  notEligible: FormPage[Boolean] = FormPage(NotEligible),
  mainBusinessCheck: FormPage[Boolean] = FormPage(MainBusinessCheck),
  signOut: FormPage[Boolean] = FormPage(SignOut),
  acceptTerms: FormPage[Boolean] = FormPage(AcceptTerms),
  eoriCheck: FormPage[Boolean] = FormPage(EoriCheck),
  signOutBadEori: FormPage[Boolean] = FormPage(SignOutBadEori),
  createUndertaking: FormPage[Boolean] = FormPage(CreateUndertaking),
) extends Journey {

  // TODO - remove public steps if not accessed externally
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

  // TODO - is steps ever called directly anywhere?
  // TODO - review this - can it be further simplified?
  override def steps: List[Option[FormPage[_]]] =
    journeySteps
      .filterNot(removeWillYouClaimIfDoYouClaimTrue)
      .filterNot(removeNotEligibleIfCustomsWaiversClaimed)
      .filterNot(removeSignOutIfMainBusinessCheckPassed)
      .filterNot(removeSignOutBadEoriIfEoriCheckPassed)
      .map(Some(_))

  private def removeWillYouClaimIfDoYouClaimTrue(f: FormPage[_]) =
    predicate(f, WillYouClaim)(customsWaivers.value.contains(true))

  private def removeNotEligibleIfCustomsWaiversClaimed(f: FormPage[_]) =
    predicate(f, NotEligible)(Seq(customsWaivers.value, willYouClaim.value).flatten.contains(true))

  private def removeSignOutIfMainBusinessCheckPassed(f: FormPage[_]) =
    predicate(f, SignOut)(mainBusinessCheck.value.contains(true))

  private def removeSignOutBadEoriIfEoriCheckPassed(f: FormPage[_]) =
    predicate(f, SignOutBadEori)(eoriCheck.value.contains(true))

  private def predicate(f: FormPage[_], uri: String)(p: Boolean) = f.uri == uri && p

}

object EligibilityJourney {
  import Journey._ // N.B. don't let intellij delete this
  implicit val format: Format[EligibilityJourney] = Json.format[EligibilityJourney]

  // TODO - it would be nicer to have a class for each form, which can then encapsulate this URL value.
  object FormUrls {
    val CustomsWaivers = "do-you-claim-customs-waivers"
    val WillYouClaim = "will-you-claim-customs-waivers"
    val NotEligible = "not-eligible"
    val MainBusinessCheck = "main-business-check"
    val SignOut = "not-eligible-to-lead"
    val AcceptTerms = "terms-conditions"
    val EoriCheck = "eoricheck"
    val SignOutBadEori = "incorrect-eori"
    val CreateUndertaking = "create-undertaking"
  }

}


