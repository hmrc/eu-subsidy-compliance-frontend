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
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NilReturnJourney.Forms.NilReturnFormPage

class NilReturnJourneySpec extends AnyWordSpecLike with Matchers {

  "NilReturnJourney" when {

    "hasNilJourneyStarted is called" should {
      "return true if counter is 1" in {
        NilReturnJourney(nilReturnCounter = 1).hasNilJourneyStarted shouldBe true
      }

      "return false otherwise" in {
        NilReturnJourney().hasNilJourneyStarted shouldBe false
      }
    }

    "isNilJourneyDoneRecently" should {
      "return true if counter is 2" in {
        NilReturnJourney(nilReturnCounter = 2).isNilJourneyDoneRecently shouldBe true

      }
      "return false otherwise" in {
        NilReturnJourney(nilReturnCounter = 1).isNilJourneyDoneRecently shouldBe false
      }
    }

    "setNilReturnValues" should {
      "return an updated instance with the specified boolean value and reset the counter to 1" in {
        NilReturnJourney(nilReturnCounter = 2).setNilReturnValues(true) shouldBe
          NilReturnJourney(nilReturn = NilReturnFormPage(true.some), nilReturnCounter = 1)
      }
    }

    "incrementNilReturnCounter" should {
      "return an updated instance with the counter incremented by 1" in {
        NilReturnJourney().incrementNilReturnCounter shouldBe NilReturnJourney(nilReturnCounter = 1)
      }
    }

    "canIncrementNilReturnCounter" should {

      "return true if hasNilJourneyStarted returns true" in {
        NilReturnJourney(nilReturnCounter = 1).canIncrementNilReturnCounter shouldBe true
      }

      "return true if isNilJourneyDoneRecently returns true" in {
        NilReturnJourney(nilReturnCounter = 2).canIncrementNilReturnCounter shouldBe true
      }

      "return false otherwise" in {
        NilReturnJourney().canIncrementNilReturnCounter shouldBe false
      }
    }
  }

}
