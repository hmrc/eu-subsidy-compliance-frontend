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
import play.api.test.Helpers.{GET, POST}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService

class EligibilityWillYouClaimControllerSpec
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

  private val controller = instanceOf[EligibilityWillYouClaimController]

  "EligibilityWillYouClaimController" when {
    "handling request to get will you claim" must {
      def performAction() = controller
        .getWillYouClaim(
          FakeRequest(GET, routes.EligibilityWillYouClaimController.getWillYouClaim.url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        val previousUrl = routes.EligibilityDoYouClaimController.getDoYouClaim.url

        inSequence {
          mockAuthWithoutEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("willyouclaim.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe previousUrl
            val selectedOptions = doc.select(".govuk-radios__input[checked]")
            selectedOptions.isEmpty shouldBe true
            val button = doc.select("form")
            button.attr("action") shouldBe routes.EligibilityWillYouClaimController.postWillYouClaim.url
          }
        )
      }
    }

    "handling request to post will you claim" must {
      def performAction(data: (String, String)*) = controller
        .postWillYouClaim(
          FakeRequest(POST, routes.EligibilityWillYouClaimController.getWillYouClaim.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "show form error" when {
        "nothing is submitted" in {
          inSequence {
            mockAuthWithoutEnrolment()
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("willyouclaim.title"),
            messageFromMessageKey("willyouclaim.error.required")
          )
        }
      }

      "redirect to next page" when {
        def testRedirection(input: Boolean, nextCall: String): Unit = {
          inSequence {
            mockAuthWithoutEnrolment()
          }
          checkIsRedirect(
            performAction("willyouclaim" -> input.toString),
            nextCall
          )
        }

        "Yes is selected" in {
          testRedirection(input = true, routes.EligibilityEoriCheckController.getEoriCheck.url)
        }

        "No is selected" in {
          testRedirection(input = false, routes.EligibilityWillYouClaimController.getNotEligible.url)
        }
      }
    }

    "handling request to get not eligible" must {
      def performAction() = controller
        .getNotEligible(
          FakeRequest(GET, routes.EligibilityWillYouClaimController.getNotEligible.url)
            .withFormUrlEncodedBody()
        )

      "display the page" in {
        inSequence {
          mockAuthWithoutEnrolment()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("notEligible.title")
        )
      }
    }
  }
}
