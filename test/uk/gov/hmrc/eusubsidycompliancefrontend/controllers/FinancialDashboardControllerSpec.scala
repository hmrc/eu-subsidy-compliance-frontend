/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import play.api.{Configuration, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage

class FinancialDashboardControllerSpec extends ControllerSpec with AuthSupport with JourneyStoreSupport
  with AuthAndSessionDataBehaviour with Matchers with ScalaFutures with IntegrationPatience {

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector)
  )

  override def additionalConfig = Configuration.from(Map(
    // Disable CSP n=once hashes in rendered output
    "play.filters.csp.nonce.enabled" -> false,
  ))

  "FinancialDashboardController" when {

    "getFinancialDashboard is called" must {

      "return the dashboard page for a logged in user with a valid EORI" in {
        mockAuthWithNecessaryEnrolment()

        running(fakeApplication) {
          val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard().url)

          val result = route(fakeApplication, request).get

          val page = instanceOf[FinancialDashboardPage]

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe page()(request, messages, instanceOf[AppConfig]).toString()
        }
      }

    }

  }

}
