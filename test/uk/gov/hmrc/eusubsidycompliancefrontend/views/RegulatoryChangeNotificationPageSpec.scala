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

package uk.gov.hmrc.eusubsidycompliancefrontend.views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.PlaySupport
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.RegulatoryChangeNotificationPage

class RegulatoryChangeNotificationPageSpec extends PlaySupport {

  "The Regulatory Change Notification page" should {

    val view: RegulatoryChangeNotificationPage = fakeApplication.injector.instanceOf[RegulatoryChangeNotificationPage]

    val document: Document = Jsoup.parse(
      view()(FakeRequest(), messages, appConfig).body
    )

    "have the correct title" in {
      document.title shouldBe
        "Before you continue - Report and manage your allowance for Customs Duty waiver claims - GOV.UK"
    }

    "have the correct heading" in {
      document.select("h1").text() shouldBe "Before you continue"
    }

    "have the Manage your undertaking button" in {
      document.select(".govuk-button").text() shouldBe "Manage your undertaking"
    }

    "have the More about the change link" in {
      document.select("#more-about-change-link").text() shouldBe "More about the change"
    }

  }

}
