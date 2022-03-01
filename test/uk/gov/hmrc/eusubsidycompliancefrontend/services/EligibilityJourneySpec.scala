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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms.{CustomsWaiversFormPage, EoriCheckFormPage, MainBusinessCheckFormPage, WillYouClaimFormPage}

class EligibilityJourneySpec extends AnyWordSpecLike with Matchers {

  "EligibilityJourney" when {

    "steps is called" should {

      "return all forms at the start of the journey" in {
        val underTest = EligibilityJourney()
        underTest.formPages shouldBe List(
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.notEligible,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.createUndertaking
        )
      }

      "remove will you claim and notEligible steps if customs waivers form has true value" in {
        val underTest = EligibilityJourney(
          customsWaivers = CustomsWaiversFormPage(Some(true)),
        )
        underTest.formPages shouldBe List(
          underTest.customsWaivers,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.createUndertaking
        )
      }

      "remove not eligible step if do you claim form has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true))
        )
        underTest.formPages shouldBe List(
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.signOut,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.createUndertaking
        )
      }

      "remove sign out step if main business check has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true)),
          mainBusinessCheck = MainBusinessCheckFormPage(Some(true)),
        )
        underTest.formPages shouldBe List(
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.signOutBadEori,
          underTest.createUndertaking
        )
      }

      "remove sign out bad eori step if eori check has true value" in {
        val underTest = EligibilityJourney(
          willYouClaim = WillYouClaimFormPage(Some(true)),
          mainBusinessCheck = MainBusinessCheckFormPage(Some(true)),
          eoriCheck = EoriCheckFormPage(Some(true)),
        )
        underTest.formPages shouldBe List(
          underTest.customsWaivers,
          underTest.willYouClaim,
          underTest.mainBusinessCheck,
          underTest.acceptTerms,
          underTest.eoriCheck,
          underTest.createUndertaking
        )
      }

    }

  }

}
