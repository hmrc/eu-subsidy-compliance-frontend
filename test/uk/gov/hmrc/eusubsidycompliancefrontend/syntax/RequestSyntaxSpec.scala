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

package uk.gov.hmrc.eusubsidycompliancefrontend.syntax

import org.scalatest.wordspec.AnyWordSpec
import RequestSyntax._
import org.scalatest.matchers.should.Matchers
import play.api.http.HeaderNames.{HOST, REFERER}
import play.api.test.FakeRequest
import play.api.test.Helpers.GET

class RequestSyntaxSpec extends AnyWordSpec with Matchers {

  private val RequestPath = "/some-resource"
  private val LocalHostAndPort = "localhost:12345"

  private val relativeRequest = FakeRequest(GET, RequestPath)
  private val localRequest = relativeRequest.withHeaders(HOST -> LocalHostAndPort)
  private val nonLocalRequest = relativeRequest.withHeaders(HOST -> "www.example.com")

  "RequestSyntax" when {

    "isFrom is called" should {

      "return true if the specified URL matches that on the request referer" in {
        relativeRequest.withHeaders(REFERER -> RequestPath).isFrom(RequestPath) shouldBe true
      }

      "return false if the specified URL does not match that on the request referer" in {
        relativeRequest.isFrom("/another-resource") shouldBe false
      }

    }

    "isLocal is called" should {

      "return true if the request URI is to localhost" in {
        localRequest.isLocal shouldBe true
      }

      "return false if the request URI is not to localhost" in {
        nonLocalRequest.isLocal shouldBe false
      }

    }

    "toRedirectTarget is called" should {

      "return an absolute URL for the request target if no path is specified and the request is to localhost" in {
        localRequest.toRedirectTarget shouldBe s"http://$LocalHostAndPort$RequestPath"
      }

      "return a relative URL for the request target if no path is specified and the request is not to localhost" in {
        nonLocalRequest.toRedirectTarget shouldBe RequestPath
      }

      "return an absolute URL for the specified path if the request is to localhost" in {
        localRequest.toRedirectTarget("/foo") shouldBe s"http://$LocalHostAndPort/foo"
      }

      "return a relative URL for the specified path if the request is not to localhost" in {
        nonLocalRequest.toRedirectTarget("/foo") shouldBe "/foo"
      }

    }

  }

}
