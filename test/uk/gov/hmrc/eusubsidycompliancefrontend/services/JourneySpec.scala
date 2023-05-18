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

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.{GET, redirectLocation, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{FormPage, Journey}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.Journey.{Form, Uri}

class JourneySpec extends AnyWordSpecLike with Matchers with MockFactory with ScalaFutures with DefaultAwaitTimeout {

  private val formPage1 = new FormPage[String] {
    override val value: Form[String] = Some("Foo")
    override def uri: Uri = "/foo"
  }

  private val formPage2 = new FormPage[String] {
    override val value: Form[String] = Some("Bar")
    override def uri: Uri = "/bar"
  }

  private val formPage3 = new FormPage[String] {
    override val value: Form[String] = Some("Baz")
    override def uri: Uri = "/baz"
  }

  private val emptyFormPage = new FormPage[String] {
    override val value: Form[String] = None
    override def uri: Uri = "/blah"
  }

  private val underTest = new Journey {
    override def steps: Array[FormPage[_]] = Array(
      formPage1,
      formPage2,
      formPage3
    )
  }

  private val incompleteJourney = new Journey {
    override def steps: Array[FormPage[_]] = Array(
      formPage1,
      formPage2,
      emptyFormPage
    )
  }

  "Journey" when {

    "previous is called" should {

      "return the previous index where there is a preceeding form page" in {
        underTest.previous(requestWithUri("/bar")) shouldBe "/foo"
      }

      "throw illegal state exception if there is no previous page" in {
        an[IllegalStateException] should be thrownBy underTest.previous(requestWithUri("/foo"))
      }

    }

    "next is called" should {

      "return the next index where there is a following form page" in {
        val result = underTest.next(requestWithUri("/foo"))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/bar")
      }

      "do something where this is no following page" in {
        an[IllegalStateException] should be thrownBy underTest.next(requestWithUri("/baz")).value
      }

    }

    "isComplete is called" should {

      "return true if the journey is complete" in {
        underTest.isComplete shouldBe true
      }

      "return false if the journey is not complete" in {
        incompleteJourney.isComplete shouldBe false
      }

    }

    "firstEmpty is called" should {

      implicit val request: FakeRequest[AnyContent] = requestWithUri("/somewhere")

      "return None if the journey is complete" in {
        underTest.firstEmpty shouldBe None
      }

      "return the first empty for page if the journey is not complete" in {
        incompleteJourney.firstEmpty should contain(Redirect(emptyFormPage.uri).withSession(request.session))
      }

    }

  }

  private def requestWithUri(u: String) = FakeRequest(GET, u)

}
