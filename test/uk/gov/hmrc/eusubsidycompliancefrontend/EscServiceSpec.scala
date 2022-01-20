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

package uk.gov.hmrc.eusubsidycompliancefrontend

import cats.implicits.catsSyntaxOptionId
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, SubsidyRetrieve, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscServiceImpl, SubsidyJourney}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EscServiceSpec extends AnyWordSpec with Matchers with MockFactory {
  val mockEscConnector = mock[EscConnector]
  val service = new EscServiceImpl(mockEscConnector)

  def mockCreateUndertaking(undertaking: Undertaking)(result: Either[models.Error, HttpResponse]) =
    (mockEscConnector
      .createUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(Future.successful(result))

  def mockRetreiveUndertaking(eori: EORI)(result: Either[Error, HttpResponse]) =
    (mockEscConnector
      .retrieveUndertaking(_:EORI)(_:HeaderCarrier))
      .expects(eori, *)
      .returning(Future.successful(result))

  def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(result: Either[Error, HttpResponse]) =
    (mockEscConnector
      .addMember(_: UndertakingRef, _:BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(Future.successful(result))

  def mockRemoveMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(result: Either[Error, HttpResponse]) =
    (mockEscConnector
      .removeMember(_: UndertakingRef, _:BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(Future.successful(result))

 def mockCreateSubsidy(undertakingRef: UndertakingRef, journey: SubsidyJourney)(result: Either[Error, HttpResponse]) = {
   (mockEscConnector
     .createSubsidy(_: UndertakingRef, _: SubsidyJourney)(_: HeaderCarrier))
     .expects(undertakingRef, journey, *)
     .returning(Future.successful(result))
 }

  def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(result: Either[Error, HttpResponse]) =
    (mockEscConnector
      .retrieveSubsidy(_: SubsidyRetrieve)(_: HeaderCarrier))
      .expects(subsidyRetrieve, *)
      .returning(Future.successful(result))

  val undertakingRefJson = Json.toJson(undertakingRef)
  val undertakingJson = Json.toJson(undertaking)
  val businessEntityJson = Json.toJson(businessEntity1)
  val undertakingSubsidiesJson = Json.toJson(undertakingSubsidies)

  val emptyHeaders = Map.empty[String, Seq[String]]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "EscServiceSpec" when {

    "handling request to create an undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockCreateUndertaking(undertaking)(Left(Error("")))
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
          await(result) shouldBe(undertakingRef)
        }
      }
    }

    "handling request to retrieve  an undertaking" must {

      "return an error" when {

        "the http response doesn't come back with status 200(created)" in {
          mockRetreiveUndertaking(eori1)(Right(HttpResponse(ACCEPTED, undertakingJson, emptyHeaders)))
          val result = service.retrieveUndertaking(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "there is no json in the response, with status OK" in {
          mockRetreiveUndertaking(eori1)(Right(HttpResponse(OK, "hi")))
          val result = service.retrieveUndertaking(eori1)
          assertThrows[RuntimeException](await(result))
        }

        "the json in the response can't be parsed, with status 200(OK)" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockRetreiveUndertaking(eori1)(Right(HttpResponse(OK, json, emptyHeaders)))
          val result = service.retrieveUndertaking(eori1)
          assertThrows[RuntimeException](await(result))
        }


      }

      "return successfully" when {

        "the http call succeeds the body of the response can be parsed" when {

          "http response status is 200 and response can be parsed" in {
            mockRetreiveUndertaking(eori1)(Right(HttpResponse(OK, undertakingJson, emptyHeaders)))
            val result = service.retrieveUndertaking(eori1)
            await(result) shouldBe(undertaking.some)
          }

          "http response status is 404 and response body is not there" in {
            mockRetreiveUndertaking(eori1)(Right(HttpResponse(NOT_FOUND, " ")))
            val result = service.retrieveUndertaking(eori1)
            await(result) shouldBe(None)
          }

        }
      }
    }

    "handling request to add member in a Business Entity undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockAddMember(undertakingRef, businessEntity3)(Left(Error("")))
          val result = service.addMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
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
          await(result) shouldBe(undertakingRef)
        }
      }
    }

    "handling request to remove member from a Business Entity undertaking" must {

      "return an error" when {

        def isError() = {
          val result = service.removeMember(undertakingRef, businessEntity3)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Left(Error("")))
          isError()
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
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
          await(result) shouldBe(undertakingRef)
        }
      }
    }

    "handling request to create subsidy" must {

      "return an error" when {

        def isError() = {
          val result = service.createSubsidy(undertakingRef, subsidyJourney)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockCreateSubsidy(undertakingRef, subsidyJourney)(Left(Error("")))
          isError()
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockCreateSubsidy(undertakingRef, subsidyJourney)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          isError()
        }

        "there is no json in the response" in {
          mockCreateSubsidy(undertakingRef, subsidyJourney)(Right(HttpResponse(OK, "hi")))
          isError()
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockCreateSubsidy(undertakingRef, subsidyJourney)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError()
        }


      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCreateSubsidy(undertakingRef, subsidyJourney)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          val result = service.createSubsidy(undertakingRef, subsidyJourney)
          await(result) shouldBe(undertakingRef)
        }
      }
    }

    "handling request to retrieve subsidy" must {

      "return an error" when {

        def isError() = {
          val result = service.retrieveSubsidy(subsidyRetrieve)
          assertThrows[RuntimeException](await(result))
        }

        "the http call fails" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Left(Error("")))
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
          await(result) shouldBe(undertakingSubsidies)
        }
      }
    }


  }

}
