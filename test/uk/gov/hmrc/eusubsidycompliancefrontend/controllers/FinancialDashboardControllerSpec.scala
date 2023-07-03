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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import play.api.{Configuration, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailVerificationService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, undertaking, undertakingRef, undertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary

class FinancialDashboardControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with EscServiceSupport {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(authSupport.mockAuthConnector),
    inject.bind[Store].toInstance(journeyStoreSupport.mockJourneyStore),
    inject.bind[EmailVerificationService].toInstance(authSupport.mockEmailVerificationService),
    inject.bind[EscService].toInstance(escServiceSupport.mockEscService),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  override def additionalConfig = Configuration.from(
    Map(
      // Disable CSP n=once hashes in rendered output
      "play.filters.csp.nonce.enabled" -> false
    )
  )

  "FinancialDashboardController" when {

    "getFinancialDashboard is called" must {

      "return the dashboard page for a logged in user with a valid EORI" in {
        authAndSessionDataBehaviour.mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        escServiceSupport.mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        escServiceSupport.mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )

        running(fakeApplication) {
          val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard.url)

          val result = route(fakeApplication, request).get

          val page = instanceOf[FinancialDashboardPage]

          val summaryData = FinancialDashboardSummary
            .fromUndertakingSubsidies(
              undertaking = undertaking,
              subsidies = undertakingSubsidies,
              today = fakeTimeProvider.today
            )

          status(result) shouldBe Status.OK
          contentAsString(result) shouldBe page(summaryData)(request, messages, instanceOf[AppConfig]).toString()
        }
      }

    }

  }

}
