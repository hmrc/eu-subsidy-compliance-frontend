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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsEmpty}
import play.api.{Application, Configuration, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailVerificationService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary

import scala.concurrent.Future

class FinancialDashboardWithSCP08ControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with EscServiceSupport
    with GuiceOneAppPerSuite {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[Store].toInstance(mockJourneyStore),
    inject.bind[EmailVerificationService].toInstance(mockEmailVerificationService),
    inject.bind[EscService].toInstance(mockEscService),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  override def additionalConfig = Configuration.from(
    Map(
      // Disable CSP n=once hashes in rendered output
      "play.filters.csp.nonce.enabled" -> false,
      "features.scp08-enabled" -> true
    )
  )

  override lazy val fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure(additionalConfig)
      .overrides(overrideBindings: _*)
      .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
      .build()

  "FinancialDashboardController" when {

    "getFinancialDashboard is called" must {

      "return the dashboard page for a logged in user with a valid EORI" in {
        inSequence {
          mockAuthWithEnrolmentAndNoEmailVerification(eori1)
          mockRetrieveUndertaking(eori1)(undertaking3.some.toFuture)
          mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
            undertakingSubsidies.toFuture
          )
          mockGetUndertakingBalance(eori1)(Future.successful(undertakingBalance))
        }

        val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard.url)
        val result = route(app, request).get
        val page = instanceOf[FinancialDashboardPage]

        val summaryData = FinancialDashboardSummary
          .fromUndertakingSubsidies(
            undertaking = undertaking3,
            subsidies = undertakingSubsidies,
            balance = Some(undertakingBalance),
            today = fakeTimeProvider.today
          )

        status(result) shouldBe Status.OK
        val data = contentAsString(result)
        data shouldBe page(summaryData)(request, messages, instanceOf[AppConfig]).toString()
        val document = Jsoup.parse(data)
        document.getElementById("SectorCapId").text() shouldBe "Sector cap (Agriculture)"
        document.getElementById("undertaking-balance-heading").text shouldBe "Undertaking balance"
        document.getElementById("undertaking-balance-value").text shouldBe "€123.45"
        document.getElementById("allowance-remaining-heading") shouldBe null
        document.getElementById("allowance-remaining-value") shouldBe null

      }

    }

  }

}
