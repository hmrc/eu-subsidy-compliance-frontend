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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms.{DoYouClaimFormPage, EoriCheckFormPage}

class EligibilityJourneySpec extends AnyWordSpecLike with Matchers {

  "EligibilityJourney" when {

    "steps is called" should {

      "return all forms at the start of the journey" in {
        val underTest = EligibilityJourney()
        underTest.steps shouldBe Array(
          underTest.doYouClaim,
          underTest.willYouClaim,
          underTest.notEligible,
          underTest.eoriCheck,
          underTest.signOutBadEori,
        )
      }

      "remove sign out step if main claim customs waiver has true value" in {
        val underTest = EligibilityJourney(
          doYouClaim = DoYouClaimFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
          underTest.signOutBadEori,
        )
      }

      "remove sign out bad eori step if eori check has true value" in {
        val underTest = EligibilityJourney(
          doYouClaim = DoYouClaimFormPage(Some(true)),
          eoriCheck = EoriCheckFormPage(Some(true))
        )
        underTest.steps shouldBe Array(
          underTest.eoriCheck,
        )
      }

    }

    "set methods are called" should {

      "return an updated instance with the specified EoriCheck boolean value" in {
        EligibilityJourney().setEoriCheck(true) shouldBe
          EligibilityJourney(eoriCheck = EoriCheckFormPage(true.some))
      }

    }
  }

}
