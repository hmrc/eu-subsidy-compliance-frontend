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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.SubsidyRetrieve
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{undertaking, undertakingSubsidies}

import java.time.LocalDate

class FinancialDashboardControllerSpec extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  private val mockEscService = mock[EscService]

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[Store].toInstance(mockJourneyStore),
    inject.bind[EscService].toInstance(mockEscService),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider),
  )

  override def additionalConfig = Configuration.from(Map(
    // Disable CSP n=once hashes in rendered output
    "play.filters.csp.nonce.enabled" -> false,
  ))

  "FinancialDashboardController" when {

    "getFinancialDashboard is called" must {

      "return the dashboard page for a logged in user with a valid EORI" in {
        mockAuthWithNecessaryEnrolment()
        mockGet(eori)(Right(Some(undertaking)))

        (mockEscService.retrieveSubsidy(_: SubsidyRetrieve)(_: HeaderCarrier))
          .expects(*, *)
          .returning(undertakingSubsidies.toFuture)

        running(fakeApplication) {
          val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard().url)

          val result = route(fakeApplication, request).get

          val page = instanceOf[FinancialDashboardPage]

          val summaryData = FinancialDashboardSummary
            .fromUndertakingSubsidies(
              undertaking = undertaking,
              subsidies = undertakingSubsidies,
              startDate = LocalDate.parse("2019-04-06"),
              endDate = fakeTimeProvider.today
            )

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe page(summaryData)(request, messages, instanceOf[AppConfig]).toString()
        }
      }

    }

  }

}
