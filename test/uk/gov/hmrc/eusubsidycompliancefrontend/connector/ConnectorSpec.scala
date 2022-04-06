/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.connector

import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should._
import org.scalatest.wordspec._
import play.api.libs.json.JsString
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate
import scala.concurrent.Future

trait ConnectorSpec { this: Matchers with AnyWordSpecLike =>

  val currentDate = LocalDate.of(2021, 1, 20)

  def connectorBehaviour[A](
    mockResponse: Option[HttpResponse] => Unit,
    performCall: () => Future[Either[A, HttpResponse]]
  ) = {

    "do a get http call and return the result" in {
      List(
        // TODO - cover range of 200 responses?
        HttpResponse(200, "{}"),
        HttpResponse(200, JsString("hi"), Map.empty[String, Seq[String]]),
      ).foreach { httpResponse =>
        withClue(s"For http response [${httpResponse.toString}]") {
          mockResponse(Some(httpResponse))
          performCall().futureValue shouldBe Right(httpResponse)
        }
      }
    }

    // TODO - any value in inspecting the actual value inside the Left?
    "return an error" when {

      "the server returns a 4xx response" in {
        mockResponse(Some(HttpResponse(404, "")))
        performCall().futureValue.isLeft shouldBe true
      }

      "the server returns a 5xx response" in {
        mockResponse(Some(HttpResponse(500, "")))
        performCall().futureValue.isLeft shouldBe true
      }

      "the future fails" in {
        mockResponse(None)
        performCall().futureValue.isLeft shouldBe true
      }

    }
  }

  // TODO - review usages of this method
  def connectorBehaviourWithMockTime(
    mockResponse: Option[HttpResponse] => Unit,
    performCall: () => Future[Either[ConnectorError, HttpResponse]],
    mockTimeResponse: LocalDate => Unit
  ) = {
    "do a get http call and return the result" in {

      List(
        HttpResponse(200, "{}"),
        HttpResponse(200, JsString("hi"), Map.empty[String, Seq[String]]),
        HttpResponse(500, "{}")
      ).foreach { httpResponse =>
        withClue(s"For http response [${httpResponse.toString}]") {

          mockTimeResponse(currentDate)
          mockResponse(Some(httpResponse))

          await(performCall()) shouldBe Right(httpResponse)
        }
      }
    }

    "return an error" when {

      "the future fails" in {

        mockTimeResponse(currentDate)
        mockResponse(None)

        await(performCall()).isLeft shouldBe true
      }

    }
  }

}
