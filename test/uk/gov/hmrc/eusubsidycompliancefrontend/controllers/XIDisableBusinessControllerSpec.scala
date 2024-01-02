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
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{businessEntityJourney, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class XIDisableBusinessControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with LeadOnlyRedirectSupport
    with EscServiceSupport
    with ScalaFutures
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[AuditService].toInstance(mockAuditService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |
                                   |play.i18n.langs = ["en", "cy", "fr"]
                                   | email-send {
                                   |     add-member-to-be-template-en = "template_add_be_EN"
                                   |     add-member-to-be-template-cy = "template_add_be_CY"
                                   |     add-member-to-lead-template-en = "template_add_lead_EN"
                                   |     add-member-to-lead-template-cy = "template_add_lead_CY"
                                   |     remove-member-to-be-template-en = "template_remove_be_EN"
                                   |     remove-member-to-be-template-cy = "template_remove_be_CY"
                                   |     remove-member-to-lead-template-en = "template_remove_lead_EN"
                                   |     remove-member-to-lead-template-cy = "template_remove_lead_CY"
                                   |     member-remove-themself-email-to-be-template-en = "template_remove_yourself_be_EN"
                                   |     member-remove-themself-email-to-be-template-cy = "template_remove_yourself_be_CY"
                                   |     member-remove-themself-email-to-lead-template-en = "template_remove_yourself_lead_EN"
                                   |     member-remove-themself-email-to-lead-template-cy = "template_remove_yourself_lead_CY"
                                   |  }
                                   |features.xi-eori-adding-disabled  = "true"
                                   |""".stripMargin)
    )
  )

  private val controller = instanceOf[BusinessEntityEoriController]

  "BusinessEntityControllerWithoutXI" when {

    "handling request to get EORI Page" must {
      def performAction = controller.getEori(FakeRequest(GET, routes.BusinessEntityEoriController.getEori.url))

      "display the page" when {

        def test(businessEntityJourney: BusinessEntityJourney): Unit = {
          val previousUrl = routes.AddBusinessEntityController.getAddBusinessEntity().url
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }

          checkPageIsDisplayed(
            performAction,
            messageFromMessageKey("businessEntityEori.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousUrl

              val input = doc.select(".govuk-input").attr("value")
              input shouldBe businessEntityJourney.eori.value.getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.BusinessEntityEoriController.postEori.url
            }
          )
        }

        "BusineEntityEORI title is available " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }
          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityTitle = document.getElementById("businessEntityEoriTitleId").text()
          entityTitle shouldBe "Business EORI Number"
        }

        "BusineEntityEORI Paragraph Body is available " in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[BusinessEntityJourney](eori1)(Right(businessEntityJourney.some))
          }
          val result = performAction
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val entityP1 = document.getElementById("businessEntityEoriP1Id").text()
          val entityP2 = document.getElementById("businessEntityEoriP2Id").text()
          val entityP3 = document.getElementById("businessEntityEoriP3Id").text()

          entityP1 shouldBe "We need to know the EORI number of the business you want to add."
          entityP2 shouldBe "The first 2 letters will be the country code, GB. This is followed by 12 or 15 digits, like GB123456123456."
          entityP3 shouldBe "This is the same as, and linked with any XI EORI number you may also have. That means that if you have GB123456123456, the XI version of it would be XI123456123456."
        }
      }
    }
  }
}
