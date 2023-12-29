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
import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status.OK
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class UndertakingSuspendedPageControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {
  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)
  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[UndertakingSuspendedPageController]

  "UndertakingSuspendedPageController" when {
    "showPage is called" must {
      "return the undertaking suspended page with Lead Undertaking content" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        def performAction() =
          controller.showPage(true)(FakeRequest(GET, routes.UndertakingSuspendedPageController.showPage(true).url))
        val result =
          performAction()
        contentAsString(result)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document
          .getElementById("undertaking-suspended-banner-title")
          .text shouldBe "Your undertaking has been suspended"
        document
          .getElementById("undertaking-suspended-p1")
          .text shouldBe "You are currently unable to access your undertaking as it has been suspended on this service."
        document
          .getElementById("undertaking-suspended-p2")
          .text shouldBe "You will have received a letter in the post about this decision. Contact the decision maker for more information."

      }
      "return the undertaking suspended page with Undertaking Member content" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        def performAction() =
          controller.showPage(false)(FakeRequest(GET, routes.UndertakingSuspendedPageController.showPage(false).url))

        val result =
          performAction()
        contentAsString(result)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document
          .getElementById("undertaking-suspended-banner-title")
          .text shouldBe "Your undertaking has been suspended"
        document
          .getElementById("undertaking-suspended-p1")
          .text shouldBe "You are currently unable to access your undertaking as it has been suspended on this service."
        document
          .getElementById("undertaking-suspended-p2")
          .text shouldBe "Contact your lead undertaking administrator for more information."
      }
    }
  }

}
