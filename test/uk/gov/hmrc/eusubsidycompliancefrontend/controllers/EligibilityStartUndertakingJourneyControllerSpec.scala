/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

class EligibilityStartUndertakingJourneyControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EscServiceSupport {
  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore)
  )

  private val controller = instanceOf[EligibilityStartUndertakingJourneyController]

  "EligibilityStartUndertakingJourneyController" when {
    "startUndertakingJourney" must {
      def performAction() = controller
        .startUndertakingJourney(
          FakeRequest(GET, "/some/url/")
            .withFormUrlEncodedBody()
        )

      "redirect to check eori page" in {
        inSequence {
          mockAuthWithEnrolment()
          mockPut[EligibilityJourney](EligibilityJourney(), eori1)(Right(EligibilityJourney()))
          mockPut[UndertakingJourney](UndertakingJourney(), eori1)(Right(UndertakingJourney()))
        }
        checkIsRedirect(performAction(), routes.EligibilityEoriCheckController.getEoriCheck.url)
      }
    }
  }
}
