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

import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnectorImpl
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.CommonTestData._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class EscConnectorSpec extends AnyWordSpec with Matchers with MockFactory with HttpSupport with ConnectorSpec {

  val (protocol, host, port) = ("http", "host", "123")

  val config = Configuration(
    ConfigFactory.parseString(s"""
                                 | microservice.services.esc {
                                 |    protocol = "$protocol"
                                 |    host     = "$host"
                                 |    port     = $port
                                 |  }
                                 |""".stripMargin)
  )

  val mockTimeProvider = mock[TimeProvider]

  val connector = new EscConnectorImpl(mockHttp, new ServicesConfig(config), mockTimeProvider)

  private def mockTimeProviderToday(today: LocalDate) =
    (mockTimeProvider.today _).expects().returning(today)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val responseHeaders            = Map.empty[String, Seq[String]]

  "EscConnectorSpec" when {

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

    "handling request to retrieve Undertaking" must {
      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking/$eori1"
      behave like connectorBehaviour(
        mockGet(expectedUrl)(_),
        () => connector.retrieveUndertaking(eori1)
      )

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
      behave like connectorBehaviourWithMockTime(
        mockPost(expectedUrl, Seq.empty, subsidyUpdate)(_),
        () => connector.createSubsidy(undertakingRef, subsidyJourney),
        mockTimeProviderToday
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
