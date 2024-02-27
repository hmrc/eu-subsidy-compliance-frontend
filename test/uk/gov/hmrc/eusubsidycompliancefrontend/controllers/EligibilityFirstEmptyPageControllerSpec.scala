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

import cats.implicits.catsSyntaxOptionId
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, await}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eligibilityJourney, eori1}
import play.api.test.Helpers._

class EligibilityFirstEmptyPageControllerSpec
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

  private val controller = instanceOf[EligibilityFirstEmptyPageController]

  "EligibilityFirstEmptyPageController" when {
    val exception = new Exception("oh no!")

    def performAction() = controller
      .firstEmptyPage(
        FakeRequest(GET, routes.EligibilityFirstEmptyPageController.firstEmptyPage.url)
          .withFormUrlEncodedBody()
      )

    "throw technical error" when {
      "call to get eligibility journey fails" in {
        inSequence {
          mockAuthWithEnrolment()
          mockGet[EligibilityJourney](eori1)(Left(ConnectorError(exception)))
        }
        assertThrows[Exception](await(performAction()))
      }
    }

    "redirect to correct page" when {
      def redirect(eligibilityJourney: Option[EligibilityJourney], expectedRedirectLocation: String) = {
        inSequence {
          mockAuthWithEnrolment()
          mockGet[EligibilityJourney](eori1)(Right(eligibilityJourney))
        }
        checkIsRedirect(performAction(), expectedRedirectLocation)
      }

      "no eligibility journey present in store" in {
        redirect(None, routes.EligibilityStartUndertakingJourneyController.startUndertakingJourney.url)
      }

      "no values are set in the eligibility journey" in {
        redirect(EligibilityJourney().some, routes.EligibilityDoYouClaimController.getDoYouClaim.url)
      }

      "eligibility journey is complete" in {
        redirect(eligibilityJourney.some, routes.UndertakingController.firstEmptyPage.url)
      }
    }
  }
}
