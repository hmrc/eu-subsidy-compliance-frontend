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
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.RetrieveEmailConnectorImpl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.CommonTestData.{eori1}

import scala.concurrent.ExecutionContext.Implicits.global

class RetrieveEmailConnectorSpec  extends AnyWordSpec
  with Matchers
  with MockFactory
  with HttpSupport
  with ConnectorSpec {

  val (protocol, host, port) = ("http", "host", "123")

  val config = Configuration(
    ConfigFactory.parseString(s"""
                                 | microservice.services.cds {
                                 |    protocol = "$protocol"
                                 |    host     = "$host"
                                 |    port     = $port
                                 |  }
                                 |""".stripMargin)
  )

  val connector = new RetrieveEmailConnectorImpl(mockHttp,  new ServicesConfig(config))

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val responseHeaders = Map.empty[String, Seq[String]]

  "RetrieveEmailConnectorSpec" when {
     "handling request to retrieve email address by eori" must {

       val expectedUrl = s"$protocol://$host:$port/customs-data-store/eori/${eori1.toString}/verified-email"
       behave like connectorBehaviour(
         mockGet(expectedUrl)(_),
         () => connector.retrieveEmailByEORI(eori1)
       )
     }
  }


}
