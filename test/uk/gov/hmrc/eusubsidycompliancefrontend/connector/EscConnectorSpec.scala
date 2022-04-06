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

import cats.implicits.catsSyntaxOptionId
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

import scala.concurrent.ExecutionContext.Implicits.global

class EscConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport with ConnectorSpec
  with ScalaFutures {

  private val (protocol, host, port) = ("http", "host", "123")

  private val config = Configuration(
    ConfigFactory.parseString(s"""
                                 | microservice.services.esc {
                                 |    protocol = "$protocol"
                                 |    host     = "$host"
                                 |    port     = $port
                                 |  }
                                 |""".stripMargin)
  )

  private val connector = new EscConnector(mockHttp, new ServicesConfig(config))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "EscConnector" when {

    "handling request to create Undertaking" must {

      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, undertaking)(_),
        () => connector.createUndertaking(undertaking)
      )

    }

    "handling request to update Undertaking" must {

      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking/update"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, undertaking)(_),
        () => connector.updateUndertaking(undertaking)
      )

    }

    // TODO - should be sufficient to use connector behaiour
    "handling request to retrieve Undertaking" must {

      val url = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking/$eori1"

      def mockGetResponse(r: HttpResponse) = mockGet(url)(r.some)

      def errorFor(status: Int) =
        ConnectorError(UpstreamErrorResponse(s"Unexpected response - got HTTP $status", status))

      "return a successful response where an undertaking exists" in {
        val response = HttpResponse(200, "{}")
        mockGetResponse(response)
        connector.retrieveUndertaking(eori1).futureValue shouldBe Right(response)
      }

      "return an unsuccessful response where an undertaking does not exist" in {
        val response = HttpResponse(404, "")
        mockGetResponse(response)
        connector.retrieveUndertaking(eori1).futureValue shouldBe Left(errorFor(NOT_FOUND))
      }

      "return an unsuccessful response where the downstream service returns a HTTP 400" in {
        val response = HttpResponse(400, "")
        mockGetResponse(response)
        connector.retrieveUndertaking(eori1).futureValue shouldBe Left(errorFor(BAD_REQUEST))
      }

      "return an unsuccessful response where the downstream service returns a HTTP 500" in {
        val response = HttpResponse(500, "")
        mockGetResponse(response)
        connector.retrieveUndertaking(eori1).futureValue shouldBe Left(errorFor(INTERNAL_SERVER_ERROR))
      }

    }

    "handling request to add member in Business Entity Undertaking" must {
      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking/member/UR123456"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, businessEntity3)(_),
        () => connector.addMember(UndertakingRef("UR123456"), businessEntity3)
      )

    }

    "handling request to remove member from Business Entity Undertaking" must {
      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking/member/remove/UR123456"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, businessEntity3)(_),
        () => connector.removeMember(UndertakingRef("UR123456"), businessEntity3)
      )

    }

    "handling request to create subsidy" must {
      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/subsidy/update"

      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, subsidyUpdate)(_),
        () => connector.createSubsidy(subsidyUpdate)
      )

    }

    "handling request to retrieve subsidy" must {

      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/subsidy/retrieve"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, subsidyRetrieve)(_),
        () => connector.retrieveSubsidy(subsidyRetrieve)
      )
    }
  }

}
