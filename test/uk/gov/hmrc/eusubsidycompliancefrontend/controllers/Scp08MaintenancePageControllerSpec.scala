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
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.Unverified
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import scala.concurrent.ExecutionContext.Implicits.global

class Scp08MaintenancePageControllerSpec
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

  private val controller = instanceOf[Scp08MaintenancePageController]

  "Scp08MaintenancePageController" when {
    "showPage is called" must {
      "return the scp 08 maintenance page" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result = controller.showPage(FakeRequest(GET, routes.Scp08MaintenancePageController.showPage.url))

        status(result) shouldBe Status.OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("scp08-banner-title").text shouldBe "There is a problem with the service"
        document
          .getElementById("scp08-banner-p1")
          .text shouldBe "Please be aware we are currently experiencing some technical difficulties with the Customs Duty Waiver Scheme service, causing some traders to see a miscalculation of the undertaking balance. We are working urgently to resolve but if you are still within balance on the system and your records you can continue to submit supplementary declarations as normal and we will rectify the online balance. We apologise for any inconvenience caused."
        document
          .getElementById("scp08-banner-p2")
          .text shouldBe "If you are experiencing issues preventing you from submitting your supplementary declaration then please contact - customs.duty-waivers@hmrc.gov.uk"
      }
    }

  }

}