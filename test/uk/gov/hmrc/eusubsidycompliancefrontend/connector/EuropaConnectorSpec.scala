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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EuropaConnector
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.IntegrationPatience

import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.WiremockSupport

class EuropaConnectorSpec
    extends AnyWordSpecLike
    with Matchers
    with WiremockSupport
    with ScalaFutures
    with IntegrationPatience {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val requestUrl =
    s"https://ec.europa.eu/budg/inforeuro/api/public/currencies/gbp"

  private val validResponse =
    """[
      |  {
      |    "currencyIso": "GBP",
      |    "refCurrencyIso": "EUR",
      |    "amount": 0.86333,
      |    "dateStart": "01/10/2023",
      |    "dateEnd": "31/10/2023"
      |  },
      |  {
      |    "currencyIso": "GBP",
      |    "refCurrencyIso": "EUR",
      |    "amount": 0.8592,
      |    "dateStart": "01/09/2023",
      |    "dateEnd": "30/09/2023"
      |  },
      |  {
      |    "currencyIso": "GBP",
      |    "refCurrencyIso": "EUR",
      |    "amount": 0.8556,
      |    "dateStart": "01/08/2023",
      |    "dateEnd": "31/08/2023"
      |  },
      |]""".stripMargin

  "EuropaConnector" when {

    "an exchange rate request is made" should {
      "return a successful response for a valid response from the europa API" in {
        givenEuropaReturns(200, requestUrl)
      }

    }

  }

  private def configuredApplication: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.europa.host" -> "localhost",
        "microservice.services.europa.port" -> server.port()
      )
      .build()

  private def givenEuropaReturns(status: Int, url: String): Unit =
    server.stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

}
