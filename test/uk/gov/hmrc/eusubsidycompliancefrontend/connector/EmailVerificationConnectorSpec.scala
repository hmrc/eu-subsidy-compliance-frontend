/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Configuration
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailVerificationStatusResponse, VerificationStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class EmailVerificationConnectorSpec
    extends BaseSpec
    with Matchers
    with MockFactory
    with HttpSupport
    with ConnectorSpec
    with ScalaFutures {

  private val (protocol, host, port) = ("http", "host", "123")
  private val baseUrl = s"$protocol://$host:$port/email-verification"

  private val config = Configuration(
    ConfigFactory.parseString(s"""
                                 | microservice.services.email-verification {
                                 |    protocol = "$protocol"
                                 |    host     = "$host"
                                 |    port     = $port
                                 |  }
                                 |""".stripMargin)
  )

  private val connector = new EmailVerificationConnector(mockHttp, new ServicesConfig(config))

  "EmailVerificationConnector" when {

    "verify email" when {
      behave like connectorBehaviour(
        mockPost(url"$baseUrl/verify-email", emailVerificationRequest)(_),
        () => connector.verifyEmail(emailVerificationRequest)
      )
    }

    "getVerificationStatus for logged in user" in {
      val credId = "credId"
      val response = EmailVerificationStatusResponse(
        List(VerificationStatus(emailAddress = "email@dr.com", verified = true, locked = false))
      )
      mockGet2(url"$baseUrl/verification-status/$credId")(Some(response))
      connector.getVerificationStatus(credId).map(_ shouldBe response)
    }
  }
}
