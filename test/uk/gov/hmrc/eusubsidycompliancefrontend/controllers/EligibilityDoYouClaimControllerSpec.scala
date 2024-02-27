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
import play.api.test.Helpers.POST
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms.EoriCheckFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService

class EligibilityDoYouClaimControllerSpec
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

  private val controller = instanceOf[EligibilityDoYouClaimController]

  "EligibilityDoYouClaimController" when {
    "handling request to get do you claim" must {
      def performAction() = controller.getDoYouClaim(FakeRequest(POST, "/"))

      "display the page" in {
        val eligibilityJourney = EligibilityJourney(eoriCheck = EoriCheckFormPage(true.some))

        def testDisplay(eligibilityJourney: EligibilityJourney): Unit = {
          inSequence {
            mockAuthWithoutEnrolment()
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")

              eligibilityJourney.doYouClaim.value match {
                case Some(value) => selectedOptions.attr("value") shouldBe value.toString
                case None => selectedOptions.isEmpty shouldBe true
              }
              val button = doc.select("form")
              button.attr("action") shouldBe routes.EligibilityDoYouClaimController.getDoYouClaim.url

            }
          )
        }

        testDisplay(eligibilityJourney)
      }
    }

    "handling request to post do you claim" must {
      def performAction(data: (String, String)*) = controller
        .postDoYouClaim(
          FakeRequest(POST, routes.EligibilityDoYouClaimController.getDoYouClaim.url)
            .withFormUrlEncodedBody(data: _*)
            .withHeaders("Referer" -> routes.EligibilityDoYouClaimController.getDoYouClaim.url)
        )

      "display form error" when {
        "nothing is submitted" in {
          inSequence {
            mockAuthWithoutEnrolment()
          }

          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("customswaivers.title"),
            messageFromMessageKey("customswaivers.error.required")
          )
        }
      }

      "redirect to next page" when {
        def testRedirection(inputValue: Boolean, nextCall: String): Unit = {
          inSequence {
            mockAuthWithoutEnrolment()
          }
          checkIsRedirect(
            performAction("customswaivers" -> inputValue.toString),
            nextCall
          )
        }

        "Yes is selected" in {
          testRedirection(inputValue = true, appConfig.eccEscSubscribeUrl)
        }

        "No is selected" in {
          testRedirection(inputValue = false, routes.EligibilityWillYouClaimController.getWillYouClaim.url)
        }
      }

    }

  }
}
