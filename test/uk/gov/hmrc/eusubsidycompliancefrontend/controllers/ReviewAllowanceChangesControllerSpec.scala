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
import play.api.test.Helpers.{GET, await, contentAsString, defaultAwaitTimeout, redirectLocation, route, status, writeableOf_AnyContentAsEmpty}
import play.api.{Application, Configuration, inject}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailVerificationService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, manuallySuspendedUndertaking, undertaking, undertaking3, undertakingBalance, undertakingRef, undertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ReviewAllowanceChangesPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ReviewAllowanceChangesControllerSpec
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

      "return the Review Allowance Changes Page for a logged in user with a valid EORI" in {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)

        val request = FakeRequest(GET, routes.ReviewAllowanceChangesController.showPage.url)
        val result = route(app, request).get
        val page = instanceOf[ReviewAllowanceChangesPage]

        val summaryData = FinancialDashboardSummary
          .fromUndertakingSubsidies(
            undertaking = undertaking,
            subsidies = undertakingSubsidies,
            balance = undertakingBalance.some,
            today = fakeTimeProvider.today
          )

        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe page(summaryData)(request, messages, instanceOf[AppConfig]).toString()

        val data = contentAsString(result)
        val document = Jsoup.parse(data)

        verifyInsetText(document)

      }

    }

    "display sector cap as agriculture on Review Allowance Changes Page" in {
      inSequence {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(undertaking3.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)
      }

      val request = FakeRequest(GET, routes.ReviewAllowanceChangesController.showPage.url)
      val result = route(app, request).get
      val page = instanceOf[ReviewAllowanceChangesPage]

      val summaryData = FinancialDashboardSummary
        .fromUndertakingSubsidies(
          undertaking = undertaking3,
          subsidies = undertakingSubsidies,
          balance = undertakingBalance.some,
          today = fakeTimeProvider.today
        )

      status(result) shouldBe Status.OK
      val data = contentAsString(result)
      data shouldBe page(summaryData)(request, messages, instanceOf[AppConfig]).toString()
      val document = Jsoup.parse(data)

      verifyInsetText(document)
    }
    "redirect to suspended page when undertaking is manually suspended" in {
      inSequence {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(manuallySuspendedUndertaking.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)
      }

      val request = FakeRequest(GET, routes.ReviewAllowanceChangesController.showPage.url)
      val result = route(app, request).get

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.UndertakingSuspendedPageController.showPage(true).url)
    }

    "throw IllegalStateException when undertaking is not found" in {
      mockAuthWithEnrolmentAndNoEmailVerification(eori1)
      mockRetrieveUndertaking(eori1)(None.toFuture)

      val request = FakeRequest(GET, routes.ReviewAllowanceChangesController.showPage.url)

      val exception = intercept[IllegalStateException] {
        await(route(app, request).get)
      }

      exception.getMessage should include("Undertaking for EORI")
    }
      assertThrows[IllegalStateException] {
        await(controller.showPage(request))
      }
    }
    "redirect to the undertaking suspended page when the undertaking is manually suspended" in {
      inSequence {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)
        mockRetrieveUndertaking(eori1)(manuallySuspendedUndertaking.some.toFuture)
        mockRetrieveSubsidiesForDateRange(undertakingRef, fakeTimeProvider.today.toSearchRange)(
          undertakingSubsidies.toFuture
        )
        mockGetUndertakingBalance(eori1)(undertakingBalance.some.toFuture)
      }
      val controller = instanceOf[ReviewAllowanceChangesController]
      val request = FakeRequest(GET, routes.ReviewAllowanceChangesController.showPage.url)
      val result = controller.showPage(request)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(
        routes.UndertakingSuspendedPageController
          .showPage(manuallySuspendedUndertaking.isLeadEORI(eori1))
          .url
      )
    }
  }

  def verifyInsetText(document: Document): Unit = {
    document
      .getElementById("reviewAllowanceChanges-p2")
      .text() shouldBe "Your allowance is still 300,000 euros, but it is now calculated over a rolling 3-year period, instead of 3 tax years."
  }

  def verifyAgricultureInsetText(document: Document): Unit = {
    document
      .getElementById("reviewAllowanceChanges-p2")
      .text() shouldBe "Your allowance is still 50,000 euros, but it is now calculated over a rolling 3-year period, instead of 3 tax years."
  }
}
