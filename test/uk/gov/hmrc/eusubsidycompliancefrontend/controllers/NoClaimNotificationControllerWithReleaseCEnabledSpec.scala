/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.Jsoup
import play.api.Configuration
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

class NoClaimNotificationControllerWithReleaseCEnabledSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with TimeProviderSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration.from(
      Map(
        // Disable CSP n=once hashes in rendered output
        "play.filters.csp.nonce.enabled" -> false,
        "features.release-c-enabled" -> true
      )
    )
  )

  private val controller = instanceOf[NoClaimNotificationController]

  "NoClaimNotificationControllerSpec" when {

    "handling request to get getNotificationConfirmation" must {

      "display correct content when suspended" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(fixedDate)
        }

        val result = controller.getNotificationConfirmation(isSuspended = true)(FakeRequest())
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.title shouldBe "Report sent - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
        document
          .getElementById("claim-confirmation-p1")
          .text shouldBe "It may take up to 24 hours before you can continue to claim any further Customs Duty waivers that you may be entitled to."
        document
          .getElementById("claim-confirmation-p2")
          .text shouldBe "Your next report must be made by 20 April 2021. This date is 90 days after the missed deadline."
        document.getElementById("betaFeedbackHeaderId").text shouldBe "Before you go"
        document
          .getElementById("betaFeedbackFirstParaId")
          .text shouldBe "Your feedback helps us make our service better."
        document
          .getElementById("beta-feedback-second-para")
          .text shouldBe "Take our survey to share your feedback on this service. It takes about 1 minute to complete."
      }

      "display correct content when NOT suspended" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockTimeProviderToday(fixedDate)
        }

        val result = controller.getNotificationConfirmation(isSuspended = false)(FakeRequest())
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))

        document.title shouldBe "Report sent - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
        document.getElementById("claim-confirmation-p1").text shouldBe "Your next report must be made by 20 April 2021."
        document.getElementById("betaFeedbackHeaderId").text shouldBe "Before you go"
        document
          .getElementById("betaFeedbackFirstParaId")
          .text shouldBe "Your feedback helps us make our service better."
        document
          .getElementById("beta-feedback-second-para")
          .text shouldBe "Take our survey to share your feedback on this service. It takes about 1 minute to complete."
      }
    }
  }

}
