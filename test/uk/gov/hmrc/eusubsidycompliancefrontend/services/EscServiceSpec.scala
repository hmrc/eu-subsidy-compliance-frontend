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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.catsSyntaxOptionId
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.NOT_ACCEPTABLE
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyController
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.CommonTestData._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EscServiceSpec extends AnyWordSpec with Matchers with MockFactory {
  private val mockEscConnector: EscConnector = mock[EscConnector]
  private val service: EscService = new EscService(mockEscConnector)

  private def mockCreateUndertaking(undertaking: Undertaking)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .createUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(Future.successful(result))

  private def mockUpdateUndertaking(undertaking: Undertaking)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .updateUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(Future.successful(result))

  private def mockRetrieveUndertaking(eori: EORI)(result: Either[ConnectorError, HttpResponse]) =
    (mockEscConnector
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(Future.successful(result))

  private def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .addMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(Future.successful(result))

  private def mockRemoveMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .removeMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(Future.successful(result))

  private def mockCreateSubsidy(undertakingRef: UndertakingRef, subsidyUpdate: SubsidyUpdate)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .createSubsidy(_: UndertakingRef, _: SubsidyUpdate)(_: HeaderCarrier))
      .expects(undertakingRef, subsidyUpdate, *)
      .returning(Future.successful(result))

  private def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    (mockEscConnector
      .retrieveSubsidy(_: SubsidyRetrieve)(_: HeaderCarrier))
      .expects(subsidyRetrieve, *)
      .returning(Future.successful(result))

  private val undertakingRefJson = Json.toJson(undertakingRef)
  private val undertakingJson: JsValue = Json.toJson(undertaking)
  private val upstreamErrorResponse = Json.parse(s"""
                                                     |{
                                                     |"message" : "Invalid EORI",
                                                     |"statusCode" : 406
                                                     |}
                                                     |""".stripMargin)

  private val undertakingSubsidiesJson = Json.toJson(undertakingSubsidies)

  private val emptyHeaders = Map.empty[String, Seq[String]]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "EscServiceSpec" when {

    "handling request to create an undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockCreateUndertaking(undertaking)(Left(ConnectorError("")))
          val result = service.createUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "the http response doesn't come back with status 201(created)" in {
          mockCreateUndertaking(undertaking)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          val result = service.createUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "there is no json in the response" in {
          mockCreateUndertaking(undertaking)(Right(HttpResponse(OK, "hi")))
          val result = service.createUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockCreateUndertaking(undertaking)(Right(HttpResponse(OK, json, emptyHeaders)))
          val result = service.createUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCreateUndertaking(undertaking)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.createUndertaking(undertaking)
          await(result) shouldBe undertakingRef
        }
      }
    }

    "handling request to update an undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockUpdateUndertaking(undertaking)(Left(ConnectorError("")))
          val result = service.updateUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "the http response doesn't come back with status 201(created)" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          val result = service.updateUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "there is no json in the response" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, "hi")))
          val result = service.updateUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, json, emptyHeaders)))
          val result = service.updateUndertaking(undertaking)
          assertThrows[RuntimeException](await(result))
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.updateUndertaking(undertaking)
          await(result) shouldBe undertakingRef
        }
      }
    }

    "handling request to retrieve  an undertaking" must {

      "return an error" when {

        "there is no json in the response, with status OK" in {
          mockRetrieveUndertaking(eori1)(Right(HttpResponse(OK, "hi")))
          val result = service.retrieveUndertaking(eori1)
          assertThrows[RuntimeException](await(result))
        }

      }

      "return successfully" when {

        "the http call succeeds the body of the response can be parsed" when {

          "http response status is 200 and response can be parsed" in {
            mockRetrieveUndertaking(eori1)(Right(HttpResponse(OK, undertakingJson, emptyHeaders)))
            val result = service.retrieveUndertaking(eori1)
            await(result) shouldBe undertaking.some
          }

          "http response status is 404 and response body is not there" in {
            mockRetrieveUndertaking(eori1)(Right(HttpResponse(NOT_FOUND, " ")))
            val result = service.retrieveUndertaking(eori1)
            await(result) shouldBe None
          }

          "http response status is 406 and response body is parsed" in {
            mockRetrieveUndertaking(eori1)(Right(HttpResponse(NOT_ACCEPTABLE, upstreamErrorResponse, emptyHeaders)))
            val result = service.retrieveUndertaking(eori1)
            await(result) shouldBe None
          }

        }
      }
    }

    "handling request to add member in a Business Entity undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockAddMember(undertakingRef, businessEntity3)(Left(ConnectorError("")))
          val result = service.addMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockAddMember(undertakingRef, businessEntity3)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          val result = service.addMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "there is no json in the response" in {
          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, "hi")))
          val result = service.addMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, json, emptyHeaders)))
          val result = service.addMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.addMember(undertakingRef, businessEntity3)
          await(result) shouldBe undertakingRef
        }
      }
    }

    "handling request to remove member from a Business Entity undertaking" must {

      "return an error" when {

        def isError(): Assertion = {
          val result = service.removeMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Left(ConnectorError("")))
          isError()
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRemoveMember(undertakingRef, businessEntity3)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          isError()
        }

        "there is no json in the response" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, "hi")))
          isError()
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError()
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.removeMember(undertakingRef, businessEntity3)
          await(result) shouldBe undertakingRef
        }
      }
    }

    "handling request to create subsidy" must {

      val subsidyUpdate = SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, LocalDate.now())

      "return an error" when {

        def isError(): Assertion = {
          val result = service.createSubsidy(undertakingRef, subsidyUpdate)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockCreateSubsidy(undertakingRef, subsidyUpdate)(Left(ConnectorError("")))
          isError()
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockCreateSubsidy(undertakingRef, subsidyUpdate)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          isError()
        }

        "there is no json in the response" in {
          mockCreateSubsidy(undertakingRef, subsidyUpdate)(Right(HttpResponse(OK, "hi")))
          isError()
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockCreateSubsidy(undertakingRef, subsidyUpdate)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError()
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCreateSubsidy(undertakingRef, subsidyUpdate)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.createSubsidy(undertakingRef, subsidyUpdate)
          await(result) shouldBe undertakingRef
        }
      }
    }

    "handling request to retrieve subsidy" must {

      "return an error" when {

        def isError(): Assertion = {
          val result = service.retrieveSubsidy(subsidyRetrieve)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Left(ConnectorError("")))
          isError()
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          isError()
        }

        "there is no json in the response" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, "hi")))
          isError()
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError()
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, undertakingSubsidiesJson, emptyHeaders)))
          val result = service.retrieveSubsidy(subsidyRetrieve)
          await(result) shouldBe undertakingSubsidies
        }
      }
    }

  }

}
