/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.PlaySupport
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmationPage

class ConfirmationPageSpec extends PlaySupport {

  "The Confirmation page" when {

    "intentToAddBusiness is set to true" should {

      val view: ConfirmationPage = fakeApplication.injector.instanceOf[ConfirmationPage]

      val document: Document = Jsoup.parse(
        view(UndertakingRef("UR123456"), EORI("GB922456789077"), intentToAddBusiness = true)(
          FakeRequest(),
          messages,
          appConfig
        ).body
      )

      "have the correct title" in {
        document.title shouldBe
          "Undertaking registered - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
      }

      "have the correct heading" in {
        document.select("h1").text() shouldBe "Undertaking registered"
      }

      "have the correct first paragraph" in {
        document.select("#confirmationFirstParaId").text() shouldBe "We have sent you a confirmation email."
      }

      "have the correct second paragraph" in {
        document.select("p.govuk-body.tight-paragraph").text() shouldBe
          "You can now:"
      }

      "have the correct third paragraph" in {
        val bullets = document.select("ul.govuk-list.govuk-list--bullet.tight-bullet-list li").eachText()
        bullets.size() shouldBe 2
        bullets.get(0) shouldBe "submit reports"
        bullets.get(1) shouldBe "add businesses to your undertaking"
      }
    }

    "intentToAddBusiness is set to false" should {

      val view: ConfirmationPage = fakeApplication.injector.instanceOf[ConfirmationPage]

      val document: Document = Jsoup.parse(
        view(UndertakingRef("UR123456"), EORI("GB922456789077"), intentToAddBusiness = false)(
          FakeRequest(),
          messages,
          appConfig
        ).body
      )

      "not display the third paragraph" in {
        document.select("#confirmation-p3").size() shouldBe 0
      }
    }
  }
}
