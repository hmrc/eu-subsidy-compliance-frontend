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

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.Helpers.ACCEPT
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EuropaConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.MonthlyExchangeRate
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.LocalDate

class EuropaConnectorSpec
    extends ConnectorSpec
    with Matchers
    with AnyWordSpecLike
    with MockFactory
    with HttpSupport
    with ScalaFutures {
  private val connector = new EuropaConnector(mockHttp)
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "EuropaConnectorSpec" when {

    "sent request for exchange rates" should {
      "return exchange rate data" in {
        val url = "https://ec.europa.eu/budg/inforeuro/api/public/currencies/gbp"
        val expectedHeader = Seq(ACCEPT -> "application/vnd.sdmx.data+json;version=1.0.0-wd")
        val response = Seq(MonthlyExchangeRate("GBP", "EUR", BigDecimal(0.8592), "01/09/2023", "30/09/2023"))
        mockGet(url, expectedHeader)(
          Some(response)
        )
        connector.retrieveMonthlyExchangeRates.futureValue shouldBe response

      }
      "return future failed if endpoint is down" in {
        an[Throwable] shouldBe thrownBy(connector.retrieveMonthlyExchangeRates.futureValue)
      }
    }
  }

}
