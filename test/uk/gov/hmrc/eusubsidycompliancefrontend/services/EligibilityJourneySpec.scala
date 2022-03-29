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

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms.{AcceptTermsFormPage, CreateUndertakingFormPage, CustomsWaiversFormPage, EoriCheckFormPage, MainBusinessCheckFormPage, WillYouClaimFormPage}

class EligibilityJourneySpec extends AnyWordSpecLike with Matchers {

  "EligibilityJourney" when {

    "steps is called" should {

      "return all forms at the start of the journey" in {
        val underTest = EligibilityJourney()
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.notEligible,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.createUndertaking
        )
      }

      "remove will you claim and notEligible steps if customs waivers form has true value" in {
        val underTest = EligibilityJourney(
          customsWaivers = CustomsWaiversFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.customsWaivers,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.createUndertaking
        )
      }

      "remove not eligible step if do you claim form has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.createUndertaking
        )
      }

      "remove sign out step if main business check has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true)),
          mainBusinessCheck = MainBusinessCheckFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.acceptTerms,
          underTest.createUndertaking
        )
      }

      "remove sign out bad eori step if eori check has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true)),
          mainBusinessCheck = MainBusinessCheckFormPage(Some(true)),
          eoriCheck = EoriCheckFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.acceptTerms,
          underTest.createUndertaking
        )
      }

    }

    "set methods are called" should {

      "return an updated instance with the specified WillYouClaim boolean value" in {
        EligibilityJourney().setWillYouClaim(true) shouldBe
          EligibilityJourney(willYouClaim = WillYouClaimFormPage(true.some))
      }

      "return an updated instance with the specified CustomsWaiver boolean value" in {
        EligibilityJourney().setCustomsWaiver(true) shouldBe
          EligibilityJourney(customsWaivers = CustomsWaiversFormPage(true.some))
      }

      "return an updated instance with the specified MainBusinessCheck boolean value" in {
        EligibilityJourney().setMainBusinessCheck(true) shouldBe
          EligibilityJourney(mainBusinessCheck = MainBusinessCheckFormPage(true.some))
      }

      "return an updated instance with the specified AcceptTerms boolean value" in {
        EligibilityJourney().setAcceptTerms(true) shouldBe
          EligibilityJourney(acceptTerms = AcceptTermsFormPage(true.some))
      }

      "return an updated instance with the specified EoriCheck boolean value" in {
        EligibilityJourney().setEoriCheck(true) shouldBe
          EligibilityJourney(eoriCheck = EoriCheckFormPage(true.some))
      }

      "return an updated instance with the specified CreateUndertaking boolean value" in {
        EligibilityJourney().setCreateUndertaking(true) shouldBe
          EligibilityJourney(createUndertaking = CreateUndertakingFormPage(true.some))
      }

    }
  }

}
