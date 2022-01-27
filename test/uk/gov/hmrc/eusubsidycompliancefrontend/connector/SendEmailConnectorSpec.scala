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
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.SendEmailConnectorImpl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.CommonTestData.{emailSendRequest, eori1}

import scala.concurrent.ExecutionContext.Implicits.global

class SendEmailConnectorSpec extends AnyWordSpec
  with Matchers
  with MockFactory
  with HttpSupport
  with ConnectorSpec {

  val (protocol, host, port) = ("http", "host", "123")

  val config = Configuration(
    ConfigFactory.parseString(s"""
                                 | microservice.services.email-send {
                                 |    protocol = "$protocol"
                                 |    host     = "$host"
                                 |    port     = $port
                                 |  }
                                 |""".stripMargin)
  )

  val connector = new SendEmailConnectorImpl(mockHttp,  new ServicesConfig(config))

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val responseHeaders = Map.empty[String, Seq[String]]

  "SendEmailConnectorSpec" when {
    "handling request to send  email address " must {
      val expectedUrl = s"$protocol://$host:$port/hmrc/email"

      behave like connectorBehaviour(
        mockPost(expectedUrl, Seq.empty, emailSendRequest)(_),
        () => connector.sendEmail(emailSendRequest)
      )
    }
  }


}