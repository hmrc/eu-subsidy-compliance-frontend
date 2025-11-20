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

package uk.gov.hmrc.eusubsidycompliancefrontend.connectors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, client}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailConnectorImplSpec extends AnyWordSpec with Matchers {

  private object TestConnector extends EmailConnector {
    override protected val http: client.HttpClientV2 = null

    def testMakeRequest(request: client.HttpClientV2 => Future[HttpResponse]) =
      makeRequest(request)
  }

  "EmailConnector" should {
    "return Right for 200" in {
      val result = await(TestConnector.testMakeRequest(_ => Future.successful(HttpResponse(OK, ""))))
      result.isRight shouldBe true
    }

    "return Right for 404" in {
      val result = await(TestConnector.testMakeRequest(_ => Future.successful(HttpResponse(NOT_FOUND, ""))))
      result.isRight shouldBe true
    }

    "return Left for 500" in {
      val result = await(TestConnector.testMakeRequest(_ => Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))))
      result.isLeft shouldBe true
    }

    "return Left on exception" in {
      val result = await(TestConnector.testMakeRequest(_ => Future.failed(new RuntimeException("boom"))))
      result.isLeft shouldBe true
    }
  }
}
