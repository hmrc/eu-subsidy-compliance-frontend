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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.util.CheckYourAnswersHelper

class CheckYourAnswersHelperSpec extends BaseSpec with Matchers {

  "CheckYourAnswersHelper" when {

    "calculateBackLink is called" should {
      "return default back link when cya not visited" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateBackLink(false, url) shouldBe url
      }

      "return cya page url when cya is visited" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateBackLink(true, url) shouldBe routes.SubsidyController.getCheckAnswers.url
      }

      "return default back link when cya is visited and use default is true" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateBackLink(
          cyaVisited = true,
          defaultBackLink = url,
          useDefault = true
        ) shouldBe url
      }
    }

    "calculateContinueLink is called" should {
      "return default continue link when cya not visited" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateContinueLink(false, url) shouldBe url
      }

      "return cya page url when cya is visited" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateContinueLink(true, url) shouldBe routes.SubsidyController.getCheckAnswers.url
      }

      "return default continue link when cya is visited and use default is true" in {
        val url = "/some-url"
        CheckYourAnswersHelper.calculateContinueLink(
          cyaVisited = true,
          defaultContinueLink = url,
          useDefault = true
        ) shouldBe url
      }
    }

  }

}
