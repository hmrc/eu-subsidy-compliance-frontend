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

package uk.gov.hmrc.eusubsidycompliancefrontend.service

import cats.implicits.catsSyntaxOptionId
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.RetrieveEmailConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.RetrieveEmailServiceImpl
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.CommonTestData._


import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RetrieveEmailServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val mockRetrieveEmailConnector = mock[RetrieveEmailConnector]

  def mockRetrieveEmail(eori: EORI)(result: Either[Error, HttpResponse]) = {
    (mockRetrieveEmailConnector
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(Future.successful(result))
  }

  val emptyHeaders = Map.empty[String, Seq[String]]

  val service = new RetrieveEmailServiceImpl(mockRetrieveEmailConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val validEmailResponseJson= Json.toJson(validEmailResponse)
  val inValidEmailResponseJson= Json.toJson(inValidEmailResponse)
  val undeliverableResponseJson = Json.toJson(undeliverableEmailResponse)


  "RetrieveEmailServiceSpec" when {

    "handling request to retrieve email by eori" must {

      "return an error" when {

        "the http call fails" in {
          mockRetrieveEmail(eori1)(Left(Error("")))
          val result = service.retrieveEmailByEORI(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "the http response doesn't come back with status 200(OK) or 404" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(BAD_REQUEST, validEmailResponseJson, emptyHeaders)))
          val result = service.retrieveEmailByEORI(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "there is no json in the response" in {
          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, "hi")))
          val result = service.retrieveEmailByEORI(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockRetrieveEmail(eori1)(Right(HttpResponse(OK, json, emptyHeaders)))
          val result = service.retrieveEmailByEORI(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "return successfully" when {


          "the http call return with 200 and valid email address response" in {
            mockRetrieveEmail(eori1)(Right(HttpResponse(OK, validEmailResponseJson, emptyHeaders)))
            val result = service.retrieveEmailByEORI(eori1)
            await(result) shouldBe(validEmailAddress.some)
          }

          "the http call return with 404 " in {
            mockRetrieveEmail(eori1)(Right(HttpResponse(NOT_FOUND," ")))
            val result = service.retrieveEmailByEORI(eori1)
            await(result) shouldBe(None)
          }

          "the http call return with 200 but the email is Undeliverable " in {
            mockRetrieveEmail(eori1)(Right(HttpResponse(OK, undeliverableResponseJson, emptyHeaders)))
            val result = service.retrieveEmailByEORI(eori1)
            await(result) shouldBe(None)
          }

          "the http call return with 200 but the email is invalid " in {
            mockRetrieveEmail(eori1)(Right(HttpResponse(OK, inValidEmailResponseJson, emptyHeaders)))
            val result = service.retrieveEmailByEORI(eori1)
            await(result) shouldBe(None)
          }
        }

      }
    }
  }

}
