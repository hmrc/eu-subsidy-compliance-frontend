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
        document.select("#confirmationSecondParaId").text() shouldBe
          "You can now submit reports of non-customs subsidy payments, or no payments in your undertaking."
      }

      "have the correct third paragraph" in {
        document.select("#confirmation-p3").text() shouldBe "You can also add businesses to your undertaking " +
          "using the Add and remove businesses link in the ‘Undertaking administration’ section of the undertaking."
      }

      "have an exit survey" which {

        "has the correct heading" in {
          document.select("#exit-survey > h2").text() shouldBe "Before you go"
        }

        "has the correct first paragraph" in {
          document.select("#exit-survey > p:nth-of-type(1)").text() shouldBe
            "Your feedback helps us make our service better."
        }

        "has the correct third paragraph" in {
          document.select("#exit-survey > p:nth-of-type(3)").text() shouldBe
            "Take a short survey to share your feedback on this service."
        }

        "has the correct link" in {
          document.select("#exit-survey > p:nth-of-type(3) > a").attr("href") shouldBe appConfig.exitSurveyUrl
        }
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
