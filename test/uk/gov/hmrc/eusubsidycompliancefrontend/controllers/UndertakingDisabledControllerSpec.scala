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

import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EscService}

class UndertakingDisabledControllerSpec
    extends ControllerSpec
    with AuthSupport
    with EscServiceSupport
    with AuthAndSessionDataBehaviour {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService)
  )

  private val controller = instanceOf[UndertakingDisabledController]

  "UndertakingDisabledController" when {

    "handling request to get Undertaking Disabled" must {

      def performAction() =
        controller.getUndertakingDisabled(FakeRequest())

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertakingDisabled.title"),
          { doc =>
            doc.getElementById("undertakingDisabledParaOne").text shouldBe "We have sent you a confirmation email."
            doc.getElementById("undertakingDisabledParaTwo").text shouldBe "You have been signed out."
            doc.getElementById("undertakingDisabledFeedbackHeader").text shouldBe "Before you go"
            doc
              .getElementById("undertakingDisabledParaThree")
              .text shouldBe "Your feedback helps us make our service better."
            doc
              .getElementById("undertakingDisabledParaFour")
              .text shouldBe "Take our survey to share your feedback on this service. It takes about 1 minute to complete."
          }
        )
      }
    }
  }
}
