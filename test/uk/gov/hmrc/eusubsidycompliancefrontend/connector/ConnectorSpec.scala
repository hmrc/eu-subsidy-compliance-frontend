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

import org.scalatest.matchers.should._
import org.scalatest.wordspec._
import play.api.libs.json.JsString
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error

import java.time.LocalDate
import scala.concurrent.Future

trait ConnectorSpec { this: Matchers with AnyWordSpecLike =>

  val currentDate = LocalDate.of(2021, 1, 20)

  def connectorBehaviour(
    mockResponse: Option[HttpResponse] => Unit,
    performCall: () => Future[Either[Error, HttpResponse]]
  ) = {
    "do a get http call and return the result" in {
      List(
        HttpResponse(200, "{}"),
        HttpResponse(200, JsString("hi"), Map.empty[String, Seq[String]]),
        HttpResponse(500, "{}")
      ).foreach { httpResponse =>
        withClue(s"For http response [${httpResponse.toString}]") {
          mockResponse(Some(httpResponse))

          await(performCall()) shouldBe Right(httpResponse)
        }
      }
    }

    "return an error" when {

      "the future fails" in {
        mockResponse(None)

        await(performCall()).isLeft shouldBe true
      }

    }
  }

  def connectorBehaviourWithMockTime(
    mockResponse: Option[HttpResponse] => Unit,
    performCall: () => Future[Either[Error, HttpResponse]],
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
