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

import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import scala.concurrent.Future

class ReleaseCEnabledControllerSpec
  extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[ExchangeRateService].toInstance(mockExchangeRateService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |features.release-c-enabled  = "true"
                                   |""".stripMargin)
    )
  )

  private val subsidycontroller = instanceOf[SubsidyController]
  private val noclaimnotificationcontroller = instanceOf[NoClaimNotificationController]

  "ClaimConfirmationPage with ReleaseCEnabled being true" when {

    "handling request to get Payment reported Page" must {
      def performAction() = subsidycontroller.getClaimConfirmationPage(
        FakeRequest(GET, routes.SubsidyController.startFirstTimeUserJourney.url)
      )

      "display the page" when {

        "ClaimConfirmationPage title and paragraph body is available" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockTimeProviderToday(fixedDate)
          }

          val result = performAction()
          contentAsString(result)
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val panel = document.getElementsByClass("govuk-panel__title")
          panel.size() shouldBe 1
          panel.text() shouldBe "Payment reported"

          document.getElementById("claimSubheadingId").text() shouldBe "What happens next"
          document.getElementById("claimSubsidyNewParagraph").text() shouldBe "It may take up to 24 hours before you can continue to claim any further Customs Duty waivers that you may be entitled to."
          document.getElementById("claimNewParaId").text() shouldBe "Your next report must be made by 20 April 2021. This date is 90 days after the missed deadline."
        }
      }
    }
  }

  "NoClaimConfirmationPage with ReleaseCEnabled being true" when {

    "handling request to get Report sent Page" must {
      def performAction() = noclaimnotificationcontroller.getNotificationConfirmation(
        FakeRequest(GET, routes.NoClaimNotificationController.getNotificationConfirmation.url)
      )

      "display the page" when {

        "NoClaimConfirmationPage paragraph body is available" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(Future.successful(Some(undertaking1)))
            mockTimeProviderToday(fixedDate)
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val panel = document.getElementsByClass("govuk-panel__title")
          panel.size() shouldBe 1
          panel.text() shouldBe "Report sent"

          document.getElementById("noClaimSubheadingId").text() shouldBe "What happens next"
          document.getElementById("noClaimSubsidyNewParagraph").text() shouldBe "It may take up to 24 hours before you can continue to claim any further Customs Duty waivers that you may be entitled to."
          document.getElementById("noClaimNewParaId").text() shouldBe "Your next report must be made by 20 April 2021. This date is 90 days after the missed deadline."
        }
      }
    }
  }
}

