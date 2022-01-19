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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnectorImpl
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.transport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, IndustrySectorLimit, UndertakingName, UndertakingRef}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class EscConnectorSpec
  extends AnyWordSpec
    with Matchers
    with MockFactory with HttpSupport with ConnectorSpec {

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

  val connector = new EscConnectorImpl(mockHttp,  new ServicesConfig(config))

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val responseHeaders = Map.empty[String, Seq[String]]

  val eori1 = EORI("GB123456789012")
  val eori2 = EORI("GB123456789013")
  val eori3 = EORI("GB123456789014")

  val businessEntity1 = BusinessEntity(EORI(eori1), true, None)
  val businessEntity2 = BusinessEntity(EORI(eori2), true, None)
  val businessEntity3 = BusinessEntity(EORI(eori3), true, None)

  val undertaking = Undertaking(UndertakingRef("UR123456").some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021,1,18).some,
    List(businessEntity1, businessEntity2))

  "EscConnectorSpec" when {

    "handling request to create Undertaking" must {
      val expectedUrl = s"$protocol://$host:$port/eu-subsidy-compliance/undertaking"
      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, undertaking)(_),
        () => connector.createUndertaking(undertaking)
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

  }

}
