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

package uk.gov.hmrc.eusubsidycompliancefrontend.journeys

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.Uri

class FormPageSpec extends AnyWordSpec with Matchers {

  case class TestFormPage(value: Journey.Form[String], uri: Uri) extends FormPage[String]

  "FormPage" should {

    "redirect to the correct URI" in {
      val formPage = TestFormPage(Some("test"), "/test-uri")

      val result = formPage.redirect

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/test-uri")
    }

    "return true for isCurrentPage when request URI matches" in {
      implicit val request = FakeRequest("GET", "/test-uri")
      val formPage = TestFormPage(Some("test"), "/test-uri")

      formPage.isCurrentPage shouldBe true
    }

    "return false for isCurrentPage when request URI does not match" in {
      implicit val request = FakeRequest("GET", "/other-uri")
      val formPage = TestFormPage(Some("test"), "/test-uri")

      formPage.isCurrentPage shouldBe false
    }
  }
}
