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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.SubsidyUpdate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class EscConnectorSpec
    extends AnyWordSpec
    with Matchers
    with MockFactory
    with HttpSupport
    with ConnectorSpec
    with ScalaFutures {

  private val (protocol, host, port) = ("http", "host", "123")
  private val baseUrl = s"$protocol://$host:$port/eu-subsidy-compliance"

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
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/undertaking", Seq.empty, undertaking)(_),
        () => connector.createUndertaking(writeableUndertaking)
      )
    }

    "handling request to update Undertaking" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/undertaking/update", Seq.empty, undertaking)(_),
        () => connector.updateUndertaking(undertaking)
      )
    }

    "handling request to retrieve Undertaking" must {
      behave like connectorBehaviour(
        mockGet(s"$baseUrl/undertaking/$eori1")(_),
        () => connector.retrieveUndertaking(eori1)
      )
    }

    "handling request to add member in Business Entity Undertaking" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/undertaking/member/UR123456", Seq.empty, businessEntity3)(_),
        () => connector.addMember(UndertakingRef("UR123456"), businessEntity3)
      )
    }

    "handling request to remove member from Business Entity Undertaking" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/undertaking/member/remove/UR123456", Seq.empty, businessEntity3)(_),
        () => connector.removeMember(UndertakingRef("UR123456"), businessEntity3)
      )
    }

    "handling request to create subsidy" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/subsidy/update", Seq.empty, subsidyUpdate)(_),
        () => connector.createSubsidy(subsidyUpdate)
      )
    }

    "handling request to remove subsidy" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/subsidy/update", Seq.empty, SubsidyUpdate.forDelete(undertakingRef, nonHmrcSubsidy))(_),
        () => connector.removeSubsidy(undertakingRef, nonHmrcSubsidy)
      )
    }

    "handling request to retrieve subsidy" must {
      behave like connectorBehaviour(
        mockPost(s"$baseUrl/subsidy/retrieve", Seq.empty, subsidyRetrieve)(_),
        () => connector.retrieveSubsidy(subsidyRetrieve)
      )
    }
  }

}
