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
import org.jsoup.nodes.Document
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.http.Status
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
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, undertaking, undertaking3, undertaking4, undertakingBalance, undertakingRef, undertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class FinancialDashboardControllerSpec
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
      // Disable CSP nonce hashes in rendered output
      "play.filters.csp.nonce.enabled" -> false
    )
  )

  override lazy val fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure(additionalConfig)
      .overrides(overrideBindings: _*)
      .build()

  "FinancialDashboardController" when {

    "getFinancialDashboard is called" must {

      "return the dashboard page for a logged in user with a valid EORI" in {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)

        val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard.url)
        val result = route(app, request).get
        val page = instanceOf[FinancialDashboardPage]
        val industrySectorKey = "General trade"

        val summaryData = FinancialDashboardSummary
          .fromUndertakingSubsidies(
            undertaking = undertaking,
            subsidies = undertakingSubsidies,
            balance = undertakingBalance.some,
            today = fakeTimeProvider.today
          )

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe page(summaryData, "generalTrade")(request, messages, instanceOf[AppConfig])
          .toString()

        val data = contentAsString(result)
        val document = Jsoup.parse(data)
        document.getElementById("undertaking-balance-heading").text shouldBe "Undertaking balance for 6 April 2023 to 5 April 2026"
        document.getElementById("undertaking-balance-value").text shouldBe "€123.45"

        verifyInsetText(document)

      }

      "return the dashboard page for a logged in user with a valid EORI - with scp08 issues" in {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(None.toFuture)

        val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard.url)
        val result = route(app, request).get
        status(result) shouldBe Status.OK

        val data = contentAsString(result)
        val document = Jsoup.parse(data)
        document.getElementById("undertaking-balance-heading").text shouldBe "Undertaking balance for 6 April 2023 to 5 April 2026"
        document
          .getElementById("undertaking-balance-value")
          .text shouldBe "€0.00"

        verifyScp08Warning(document)
      }

  }

    "display sector cap as General trade on financial Dashboard Page" in {
      inSequence {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(undertaking3.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)
      }

      val request = FakeRequest(GET, routes.FinancialDashboardController.getFinancialDashboard.url)
      val result = route(app, request).get
      val page = instanceOf[FinancialDashboardPage]
      val industrySectorKey = "generalTrade"

      val summaryData = FinancialDashboardSummary
        .fromUndertakingSubsidies(
          undertaking = undertaking4,
          subsidies = undertakingSubsidies,
          balance = undertakingBalance.some,
          today = fakeTimeProvider.today
        )

      status(result) shouldBe Status.OK
      val data = contentAsString(result)
      data shouldBe page(summaryData, industrySectorKey)(request, messages, instanceOf[AppConfig]).toString()
      val document = Jsoup.parse(data)
      verifyAgricultureInsetText(document)
    }

  }

    def verifyInsetText(document: Document): Unit = {
      document
        .getElementById("dashboard-inset-text")
        .text() shouldBe "Customs subsidies (Customs Duty waivers) claims can take up to 24 hours to update here."
    }

    def verifyAgricultureInsetText(document: Document): Unit = {
      document.getElementById("govuk-notification-banner-title").text shouldBe "Important"
      document
        .getElementById("dashboard-inset-text")
        .text() shouldBe "Customs subsidies (Customs Duty waivers) claims can take up to 24 hours to update here."
    }

    def verifyScp08Warning(document: Document): Unit = {
      document
        .getElementById("scp08-warning")
        .text() shouldBe "! Warning Your 'Undertaking balance', 'Total claimed' and 'Customs subsidies (Customs Duty waivers)' amounts in the first section may show temporary differences to your own records. They may take up to 24 hours to be amended here, so keeping a record of any payments you have received is advised."
    }

}
