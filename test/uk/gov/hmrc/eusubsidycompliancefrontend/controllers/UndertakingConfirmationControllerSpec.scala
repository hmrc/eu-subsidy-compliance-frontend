/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class UndertakingConfirmationControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EmailService].toInstance(mockEmailService)
  )

  private val controller = instanceOf[UndertakingConfirmationController]
  private val exception = new Exception("oh no")

  "UndertakingConfirmationController" when {

    "handling request to get confirmation" must {

      def performAction() = controller.getConfirmation(undertakingRef.value)(
        FakeRequest(GET, routes.UndertakingConfirmationController.getConfirmation(undertakingRef.value).url)
      )

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.confirmation.title")
        )
      }

      "display the page with intent to add business" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[UndertakingJourney](eori1)(
            Right(undertakingJourneyComplete.copy(addBusiness = UndertakingAddBusinessFormPage(true.some)).some)
          )
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.confirmation.title")
        )
      }

      "show confirmation page even if undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          checkPageIsDisplayed(
            performAction(),
            "Undertaking registered"
          )
        }
      }
    }

    "handling request to Post Confirmation page" must {

      def performAction(data: (String, String)*) =
        controller.postConfirmation(FakeRequest(POST, "/").withFormUrlEncodedBody(data: _*))

      "throw technical error" when {

        "confirmation form is empty" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
          }

          assertThrows[Exception](await(performAction()))
        }

        "call to update undertaking confirmation fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("confirm" -> "true")))
        }
      }

      "redirect to next page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))
        }

        checkIsRedirect(
          performAction("confirm" -> "true"),
          routes.AccountController.getAccountPage.url
        )
      }
    }
  }
}
