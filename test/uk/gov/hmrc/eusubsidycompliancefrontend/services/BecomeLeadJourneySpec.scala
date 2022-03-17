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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.FormPages.{BecomeLeadEoriFormPage, controller}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms.{CustomsWaiversFormPage, EoriCheckFormPage, MainBusinessCheckFormPage, WillYouClaimFormPage}

class BecomeLeadJourneySpec extends AnyWordSpecLike with Matchers {

  "BecomeLeadJourneySpec" when {

    "steps is called" should {

      "return all forms at the start of the journey" in {
        val underTest = BecomeLeadJourney()
        underTest.steps shouldBe Array(
          underTest.becomeLeadEori,
          underTest.acceptTerms,
          underTest.confirmation
        )
      }
    }
    "uris correct" should {

      "Become lead" in {
        BecomeLeadJourney.FormPages.BecomeLeadEoriFormPage().uri shouldBe  routes.BecomeLeadController.getBecomeLeadEori().url
      }

      "Accept terms" in {
        BecomeLeadJourney.FormPages.TermsAndConditionsFormPage().uri shouldBe  routes.BecomeLeadController.getAcceptPromotionTerms().url
      }

      "Confirmation" in {
        BecomeLeadJourney.FormPages.ConfirmationFormPage().uri shouldBe  routes.BecomeLeadController.getPromotionConfirmation().url
      }
    }
  }
}
