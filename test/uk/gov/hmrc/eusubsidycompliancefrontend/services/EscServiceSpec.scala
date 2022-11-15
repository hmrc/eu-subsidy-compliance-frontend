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
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{ExchangeRateCache, RemovedSubsidyRepository, UndertakingCache, YearAndMonth}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyController
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class EscServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures with IntegrationPatience {

  private val mockEscConnector = mock[EscConnector]
  private val mockUndertakingCache = mock[UndertakingCache]
  private val mockExchangeRateCache = mock[ExchangeRateCache]
  private val mockRemovedSubsidyRepository = mock[RemovedSubsidyRepository]

  private val service: EscService = new EscService(
    mockEscConnector,
    mockUndertakingCache,
    mockExchangeRateCache,
    mockRemovedSubsidyRepository,
  )

  private def mockCreateUndertaking(undertaking: UndertakingCreate)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    when(mockEscConnector.createUndertaking(argEq(undertaking))(any()))
      .thenReturn(result.toFuture)

  private def mockUpdateUndertaking(undertaking: Undertaking)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    when(mockEscConnector.updateUndertaking(argEq(undertaking))(any()))
      .thenReturn(result.toFuture)

  private def mockRetrieveUndertaking(eori: EORI)(result: Either[ConnectorError, HttpResponse]) =
    when(mockEscConnector.retrieveUndertaking(argEq(eori))(any()))
      .thenReturn(result.toFuture)

  private def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    when(mockEscConnector.addMember(argEq(undertakingRef), argEq(businessEntity))(any()))
      .thenReturn(result.toFuture)

  private def mockRemoveMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    when(mockEscConnector.removeMember(argEq(undertakingRef), argEq(businessEntity))(any()))
      .thenReturn(result.toFuture)

  private def mockCreateSubsidy(subsidyUpdate: SubsidyUpdate)(
    result: Either[ConnectorError, HttpResponse]
  ) =
    when(mockEscConnector.createSubsidy(argEq(subsidyUpdate))(any()))
      .thenReturn(result.toFuture)

  private def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(result: Either[ConnectorError, HttpResponse]) =
    when(mockEscConnector.retrieveSubsidy(argEq(subsidyRetrieve))(any()))
      .thenReturn(result.toFuture)

  private def mockRemoveSubsidy(
    undertakingRef: UndertakingRef,
    nonHmrcSubsidy: NonHmrcSubsidy
  )(result: Either[ConnectorError, HttpResponse]) =
    when(mockEscConnector.removeSubsidy(argEq(undertakingRef), argEq(nonHmrcSubsidy))(any()))
      .thenReturn(result.toFuture)

  private def mockCachePut[A](eori: EORI, in: A)(result: Either[Exception, A]) =
    when(mockUndertakingCache.put(argEq(eori), argEq(in))(any()))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockCacheGet[A : ClassTag](eori: EORI)(result: Either[Exception, Option[A]]) =
    when(mockUndertakingCache.get[A](argEq(eori))(any(), any()))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockCacheDeleteUndertaking(ref: UndertakingRef)(result: Either[Exception, Unit]) =
    when(mockUndertakingCache.deleteUndertaking(argEq(ref)))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockCacheDeleteUndertakingSubsidies(ref: UndertakingRef)(result: Either[Exception, Unit]) =
    when(mockUndertakingCache.deleteUndertakingSubsidies(argEq(ref)))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockRetrieveExchangeRate(date: LocalDate)(result: Either[ConnectorError, HttpResponse]) =
    when(mockEscConnector.retrieveExchangeRate(argEq(date))(any()))
      .thenReturn(result.toFuture)

  private def mockExchangeRateCacheGet[A : ClassTag](key: YearAndMonth)(result: Either[Exception, Option[A]]) =
    when(mockExchangeRateCache.get[A](argEq(key))(any(), any()))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockExchangeRateCachePut[A](key: YearAndMonth, in: A)(result: Either[Exception, A]) =
    when(mockExchangeRateCache.put[A](argEq(key), argEq(in))(any(), any()))
      .thenReturn(result.fold(Future.failed, _.toFuture))

  private def mockAddRemovedSubsidy(eori: EORI, subsidy: NonHmrcSubsidy) =
    when(mockRemovedSubsidyRepository.add(argEq(eori), argEq(subsidy)))
      .thenReturn(().toFuture)

  private def mockGetAllRemovedSubsidies(eori: EORI)(subsidies: Seq[NonHmrcSubsidy]) =
    when(mockRemovedSubsidyRepository.getAll(argEq(eori)))
      .thenReturn(subsidies.toFuture)

  private val undertakingRefJson = Json.toJson(undertakingRef)
  private val undertakingJson: JsValue = Json.toJson(undertaking)
  private val undertakingSubsidiesJson = Json.toJson(undertakingSubsidies)
  private val exchangeRateJson = Json.toJson(exchangeRate)

  private val emptyHeaders = Map.empty[String, Seq[String]]

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val e: EORI = CommonTestData.eori1

  "EscService" when {

    "handling request to create an undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockCreateUndertaking(writeableUndertaking)(Left(ConnectorError("")))
          service.createUndertaking(writeableUndertaking).failed.futureValue shouldBe a[RuntimeException]
        }

        "the http response doesn't come back with status 201(created)" in {
          mockCreateUndertaking(writeableUndertaking)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          service.createUndertaking(writeableUndertaking).failed.futureValue shouldBe a[RuntimeException]
        }

        "there is no json in the response" in {
          mockCreateUndertaking(writeableUndertaking)(Right(HttpResponse(OK, "hi")))
          service.createUndertaking(writeableUndertaking).failed.futureValue shouldBe a[RuntimeException]
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockCreateUndertaking(writeableUndertaking)(Right(HttpResponse(OK, json, emptyHeaders)))
          service.createUndertaking(writeableUndertaking).failed.futureValue shouldBe a[RuntimeException]
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCreateUndertaking(writeableUndertaking)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockCachePut(eori1, writeableUndertaking.toUndertakingWithRef(undertakingRef))(Right(undertaking))
          service.createUndertaking(writeableUndertaking).futureValue shouldBe undertakingRef
        }
      }
    }

    "handling request to update an undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockUpdateUndertaking(undertaking)(Left(ConnectorError("")))
          service.updateUndertaking(undertaking).failed.futureValue shouldBe a[RuntimeException]
        }

        "the http response doesn't come back with status 201(created)" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          val result = service.updateUndertaking(undertaking)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "there is no json in the response" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, "hi")))
          service.updateUndertaking(undertaking).failed.futureValue shouldBe a[RuntimeException]
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, json, emptyHeaders)))
          service.updateUndertaking(undertaking).failed.futureValue shouldBe a[RuntimeException]
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockUpdateUndertaking(undertaking)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockCacheDeleteUndertaking(undertakingRef)(Right(()))
          service.updateUndertaking(undertaking).futureValue shouldBe undertakingRef
        }
      }
    }

    "handling request to retrieve an undertaking" must {

      "return an error" when {

        "there is no json in the response, with status OK" in {
          mockCacheGet[Undertaking](eori1)(Right(None))
          mockRetrieveUndertaking(eori1)(Right(HttpResponse(OK, "hi")))
          service.retrieveUndertaking(eori1).failed.futureValue shouldBe a[RuntimeException]
        }

      }

      "return successfully" when {

        "the http call succeeds the body of the response can be parsed" when {

          "the undertaking is present in the cache" in {
            mockCacheGet[Undertaking](eori1)(Right(undertaking.some))
            mockCachePut(eori1, undertaking)(Right(undertaking))
            service.retrieveUndertaking(eori1).futureValue shouldBe undertaking.some
          }

          "http response status is 200 and response can be parsed" in {
            mockCacheGet[Undertaking](eori1)(Right(None))
            mockRetrieveUndertaking(eori1)(Right(HttpResponse(OK, undertakingJson, emptyHeaders)))
            mockCachePut(eori1, undertaking)(Right(undertaking))
            service.retrieveUndertaking(eori1).futureValue shouldBe undertaking.some
          }

          "http response status is 404 and response body is empty" in {
            mockCacheGet[Undertaking](eori1)(Right(None))
            mockRetrieveUndertaking(eori1)(
              Left(ConnectorError(UpstreamErrorResponse("Unexpected response - got HTTP 404", NOT_FOUND)))
            )
            service.retrieveUndertaking(eori1).futureValue shouldBe None
          }

        }

        "return an error" when {

          "http response status is 406 and response body is parsed" in {
            val ex = UpstreamErrorResponse("Unexpected response - got HTTP 406", NOT_ACCEPTABLE)
            mockCacheGet[Undertaking](eori1)(Right(None))
            mockRetrieveUndertaking(eori1)(Left(ConnectorError(ex)))
            service.retrieveUndertaking(eori1).failed.futureValue shouldBe a[ConnectorError]
          }
        }

      }
    }

    "handling request to add member in a Business Entity undertaking" must {

      "return an error" when {

        "the http call fails" in {
          mockAddMember(undertakingRef, businessEntity3)(Left(ConnectorError("")))
          service.addMember(undertakingRef, businessEntity3).failed.futureValue shouldBe a[RuntimeException]
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockAddMember(undertakingRef, businessEntity3)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          service.addMember(undertakingRef, businessEntity3).failed.futureValue shouldBe a[RuntimeException]
        }

        "there is no json in the response" in {
          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, "hi")))
          service.addMember(undertakingRef, businessEntity3).failed.futureValue shouldBe a[RuntimeException]
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")

          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, json, emptyHeaders)))
          service.addMember(undertakingRef, businessEntity3).failed.futureValue shouldBe a[RuntimeException]
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockAddMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockCacheDeleteUndertaking(undertakingRef)(Right(()))
          val result = service.addMember(undertakingRef, businessEntity3)
          result.futureValue shouldBe undertakingRef
        }
      }
    }

    "handling request to remove member from a Business Entity undertaking" must {

      "return an error" when {

        def isError: Assertion = {
          val result = service.removeMember(undertakingRef, businessEntity3)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "the http call fails" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Left(ConnectorError("")))
          isError
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRemoveMember(undertakingRef, businessEntity3)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          isError
        }

        "there is no json in the response" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, "hi")))
          isError
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockRemoveMember(undertakingRef, businessEntity3)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockCacheDeleteUndertaking(undertakingRef)(Right(()))
          mockCacheDeleteUndertakingSubsidies(undertakingRef)(Right(()))
          service.removeMember(undertakingRef, businessEntity3).futureValue shouldBe undertakingRef
        }
      }
    }

    "handling request to create subsidy" must {

      val subsidyUpdate = SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, LocalDate.now())

      "return an error" when {

        def isError: Assertion = {
          val result = service.createSubsidy(subsidyUpdate)
          result.failed.futureValue shouldBe a[RuntimeException]
        }

        "the http call fails" in {
          mockCreateSubsidy(subsidyUpdate)(Left(ConnectorError("")))
          isError
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockCreateSubsidy(subsidyUpdate)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          isError
        }

        "there is no json in the response" in {
          mockCreateSubsidy(subsidyUpdate)(Right(HttpResponse(OK, "hi")))
          isError
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockCreateSubsidy(subsidyUpdate)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCreateSubsidy(subsidyUpdate)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockCacheDeleteUndertakingSubsidies(undertakingRef)(Right(()))
          service.createSubsidy(subsidyUpdate).futureValue shouldBe undertakingRef
        }
      }
    }

    "handling request to retrieve subsidy" must {

      "return an error" when {

        def isError: Assertion = {
          service.retrieveAllSubsidies(undertakingRef).failed.futureValue shouldBe a[RuntimeException]
        }

        "the http call fails" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Left(ConnectorError("")))
          isError
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders)))
          isError
        }

        "there is no json in the response" in {
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, "hi")))
          isError
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError
        }

      }

      "return successfully" when {

        "the undertaking subsidies are present in the cache" in {
          mockCacheGet[UndertakingSubsidies](eori1)(Right(undertakingSubsidies.some))
          mockCachePut(eori1, undertakingSubsidies)(Right(undertakingSubsidies))
          service.retrieveAllSubsidies(undertakingRef).futureValue shouldBe undertakingSubsidies
        }

        "the http call succeeds and the body of the response can be parsed" in {
          mockCacheGet[UndertakingSubsidies](eori1)(Right(Option.empty))
          mockRetrieveSubsidy(subsidyRetrieve)(Right(HttpResponse(OK, undertakingSubsidiesJson, emptyHeaders)))
          mockGetAllRemovedSubsidies(eori1)(Seq.empty)
          mockCachePut(eori1, undertakingSubsidies)(Right(undertakingSubsidies))
          service.retrieveAllSubsidies(undertakingRef).futureValue shouldBe undertakingSubsidies
        }
      }

    }

    "handling request to remove subsidy" must {

      "return an error" when {

        def isError: Assertion = {
          service.removeSubsidy(undertakingRef, nonHmrcSubsidy).failed.futureValue shouldBe a[RuntimeException]
        }

        "the http call fails" in {
          mockRemoveSubsidy(undertakingRef, nonHmrcSubsidy)(Left(ConnectorError("")))
          isError
        }

        "the http response doesn't come back with status 200(OK)" in {
          mockRemoveSubsidy(undertakingRef, nonHmrcSubsidy)(
            Right(HttpResponse(BAD_REQUEST, undertakingRefJson, emptyHeaders))
          )
          isError
        }

        "there is no json in the response" in {
          mockRemoveSubsidy(undertakingRef, nonHmrcSubsidy)(Right(HttpResponse(OK, "hi")))
          isError
        }

        "the json in the response can't be parsed" in {
          val json = Json.parse("""{ "a" : 1 }""")
          mockRemoveSubsidy(undertakingRef, nonHmrcSubsidy)(Right(HttpResponse(OK, json, emptyHeaders)))
          isError
        }

      }

      "return successfully" when {

        "the http call succeeds and the body of the response can be parsed" in {
          mockCacheGet[UndertakingSubsidies](eori1)(Right(Option.empty))
          mockRemoveSubsidy(undertakingRef, nonHmrcSubsidy)(Right(HttpResponse(OK, undertakingRefJson, emptyHeaders)))
          mockAddRemovedSubsidy(eori1, nonHmrcSubsidy.copy(removed = Some(true)))
          mockCacheDeleteUndertakingSubsidies(undertakingRef)(Right(()))
          val result = service.removeSubsidy(undertakingRef, nonHmrcSubsidy)
          result.futureValue shouldBe undertakingRef
        }
      }

    }

    "handling request to retrieve exchange rate" must {

      "return an error" when {

        "no cached item is present and the http call fails" in {
          mockExchangeRateCacheGet(YearAndMonth.fromDate(fixedDate))(Right(None))
          mockRetrieveExchangeRate(fixedDate)(Left(ConnectorError("Error")))
          service.retrieveExchangeRate(fixedDate).failed.futureValue shouldBe a[RuntimeException]
        }

        "no cached item is present and the http response is not successful" in {
          mockExchangeRateCacheGet(YearAndMonth.fromDate(fixedDate))(Right(None))
          mockRetrieveExchangeRate(fixedDate)(Right(HttpResponse(BAD_REQUEST, exchangeRateJson, emptyHeaders)))
          service.retrieveExchangeRate(fixedDate).failed.futureValue shouldBe a[RuntimeException]
        }

        "no cached item is present and the response body could not be parsed" in {
          mockExchangeRateCacheGet(YearAndMonth.fromDate(fixedDate))(Right(None))
          mockRetrieveExchangeRate(fixedDate)(Right(HttpResponse(OK, "This is not valid json", emptyHeaders)))
          service.retrieveExchangeRate(fixedDate).failed.futureValue shouldBe a[RuntimeException]
        }

      }

      "return successfully" when {

        "no cached item is present and the http call succeeds and the body of the response can be parsed" in {
          mockExchangeRateCacheGet(YearAndMonth.fromDate(fixedDate))(Right(None))
          mockRetrieveExchangeRate(fixedDate)(Right(HttpResponse(OK, exchangeRateJson, emptyHeaders)))
          mockExchangeRateCachePut(YearAndMonth.fromDate(fixedDate), exchangeRate)(Right(exchangeRate))
          service.retrieveExchangeRate(fixedDate).futureValue shouldBe exchangeRate
        }

        "an item is present in the cache" in {
          mockExchangeRateCacheGet(YearAndMonth.fromDate(fixedDate))(Right(exchangeRate.some))
          service.retrieveExchangeRate(fixedDate).futureValue shouldBe exchangeRate
        }

      }

    }

  }

}
