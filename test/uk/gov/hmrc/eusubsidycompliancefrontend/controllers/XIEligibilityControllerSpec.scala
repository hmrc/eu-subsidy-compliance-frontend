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
import play.api.Configuration
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class XIEligibilityControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EscServiceSupport {

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration(
      ConfigFactory.parseString(s"""
                                   |features.xi-eori-adding-disabled  = "true"
                                   |""".stripMargin)
    )
  )

  private val controller = instanceOf[EligibilityController]

  "XIEligibilityControllerSpec" when {

    "handling request to get XIEORI check" must {

      def performAction() = controller.getEoriCheck(
        FakeRequest(GET, routes.SubsidyController.getReportPaymentFirstTimeUser.url)
      )

      "display the page" when {

        "XIEORI title is available " in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("page-heading").text() shouldBe "Registering an EORI number"
        }

        "XIEORI paragraph body is available " in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
            mockGetOrCreate[EligibilityJourney](eori1)(Right(eligibilityJourney))
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val paraOne = document.getElementById("eoricheck-desc-1").text()
          paraOne shouldBe "This is the EORI number that is registered to your Government Gateway ID."
          val paraTwo = document.getElementById("paragraphId").text()
          paraTwo shouldBe
            "This is the same as, and linked with any XI EORI number you may also have. That means that if you have GB123456123456, the XI version of it would be XI123456123456."
          val legend = document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m")
          legend.text shouldBe s"Is the EORI number you want to register $eori1?"
          val button = document.select("form")
          button.attr("action") shouldBe routes.EligibilityController.postEoriCheck.url
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }

  }

}
