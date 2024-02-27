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
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import play.api.inject
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.Unverified
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class UnverifiedEmailControllerSpec
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

  "UnverifiedEmailController" when {
    "unverifiedEmail is called" must {
      "return the unverified email page" in {
        mockAuthWithEnrolmentAndNoEmailVerification(eori1)

        running(fakeApplication) {
          val request = FakeRequest(GET, routes.UnverifiedEmailController.unverifiedEmail.url)

          val result = route(fakeApplication, request).get

          status(result) shouldBe Status.OK

          val document = Jsoup.parse(contentAsString(result))

          document.title shouldBe "Your email address needs verifying - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
          document.getElementById("unverifiedEmail-h1").text shouldBe "Your email address needs verifying"
          document
            .getElementById("unverifiedEmail-p1")
            .text shouldBe "To complete tasks in your undertaking, re-enter the email address you would like to use so that we can verify it."
          document.getElementById("unverifiedEmail-link").text shouldBe "Re-enter your email address"
          document.getElementById("unverifiedEmail-link").attr("href") shouldBe routes.UndertakingEmailController
            .getAddEmailForVerification(Unverified)
            .url

        }
      }

    }

  }

}
