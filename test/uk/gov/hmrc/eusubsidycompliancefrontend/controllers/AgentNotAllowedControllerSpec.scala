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
import play.api.http.Status
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class AgentNotAllowedControllerSpec
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

  private val controller = instanceOf[AgentNotAllowedController]

  "AgentNotAllowedController" when {
    "showPage is called" must {
      "return the access denied for agents page" in {
        val result = controller.showPage(FakeRequest(GET, routes.Scp08MaintenancePageController.showPage.url))

        status(result) shouldBe Status.OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("accessDeniedForAgents-h1").text shouldBe "You cannot access this service"
        document
          .getElementById("accessDeniedForAgents-p1")
          .text shouldBe "You will not be able to use this service because you’re logged in with an agent services account Government Gateway user ID."
        document
          .getElementById("accessDeniedForAgents-p2")
          .text shouldBe "If your client needs to register an undertaking to report non-customs subsidy payments or no payments, they must do it themselves using their own Government Gateway account."
        document
          .getElementById("accessDeniedForAgents-p3")
          .text shouldBe "If they do not have a Government Gateway user ID, they can create one when they start to register."
        val link = document
          .getElementById("accessDeniedForAgents-link")
        link.text shouldBe "Find out more about using this service"
        link.attr(
          "href"
        ) shouldBe "https://www.gov.uk/guidance/report-payments-and-view-your-allowance-for-non-customs-state-aid-and-customs-duty-waiver-claims"

      }

    }

  }

}
