/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.AuthConnector

class NewUndertakingCategoryIntroControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector)
  )

  private val controller = instanceOf[NewUndertakingCategoryIntroController]

  "RegulatoryChangeNotificationController" when {

    "showPage is called" must {

      "return the regulatory change notification page" in {
        inSequence {
          mockAuthWithEnrolment()
        }

        def performAction() =
          controller.showPage(FakeRequest(GET, routes.RegulatoryChangeNotificationController.showPage.url))

        val result = performAction()
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.select(".interrupt-card").hasText shouldBe true
        document.select("h1").text() should include("Before you continue")
        document.select(".govuk-button").text() should include("Manage your undertaking")
        document.select("#more-about-change-link").text() should include("More about the change")
      }

    }

    "continue is called" must {

      "redirect to account page" in {
        inSequence {
          mockAuthWithEnrolment()
        }

        def performAction() =
          controller.continue(FakeRequest(POST, routes.RegulatoryChangeNotificationController.continue.url))

        val result = performAction()
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AccountController.getAccountPage.url)
      }

    }

  }

}
