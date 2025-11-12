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
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class UndertakingInvalidSectorSuspendedPageControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2026)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider),
    inject.bind[Store].toInstance(mockJourneyStore)
  )

  private val controller = instanceOf[UndertakingInvalidSectorSuspendedPageController]

  "UndertakingInvalidSectorSuspendedPageController" when {
    "showPage is called" should {
      "render the suspended page when session has a suspensionCode" in {
        inSequence { mockAuthWithEnrolment() }

        val result =
          controller.showPage(
            FakeRequest(GET, routes.UndertakingInvalidSectorSuspendedPageController.showPage.url)
              .withSession("suspensionCode" -> "SUSP-01")
          )

        status(result) shouldBe OK
        val doc = Jsoup.parse(contentAsString(result))
        doc.select("h1").text() shouldBe "You cannot use this service until you update your undertaking information"
        doc.select("button.govuk-button").text() should include("Continue")
      }

      "redirect to account page when session is missing suspensionCode" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.showPage(
            FakeRequest(GET, routes.UndertakingInvalidSectorSuspendedPageController.showPage.url)
          )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.AccountController.getAccountPage.url)
      }
    }

    "continue is called" should {
      "save a fresh journey in Store and redirect to the NACE intro page" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockPut[UndertakingJourney](UndertakingJourney(), eori1)(Right(UndertakingJourney()))
          mockUpdate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
        }

        val result =
          controller.continue(
            FakeRequest(POST, routes.UndertakingInvalidSectorSuspendedPageController.continue.url)
          )
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.NaceUndertakingCategoryIntroController.showPage.url)
      }
    }

  }
}
