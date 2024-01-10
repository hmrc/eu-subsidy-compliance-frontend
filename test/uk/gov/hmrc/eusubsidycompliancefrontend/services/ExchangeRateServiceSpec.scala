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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EuropaConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.MonthlyExchangeRateCache
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.exchangeRate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, YearMonth}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ExchangeRateServiceSpec extends BaseSpec with OptionValues with Matchers with MockitoSugar with ScalaFutures {
  private val mockMonthlyExchangeRateCache = mock[MonthlyExchangeRateCache]
  private val mockEuropaConnector = mock[EuropaConnector]

  val exchangeRateService = new ExchangeRateService(mockEuropaConnector, mockMonthlyExchangeRateCache)
  val localDate: LocalDate = exchangeRate.dateEnd
  val previousMonthYear: LocalDate = YearMonth.from(localDate).minusMonths(1).atEndOfMonth()

  "handling request to retrieve exchange rate" must {
    "return an exception" when {

      "no cached item is present and the http response is not successful" in {
        val exception = new RuntimeException("Europa endpoint failed")
        when(mockMonthlyExchangeRateCache.getMonthlyExchangeRate(previousMonthYear)).thenReturn(Future.successful(None))
        when(mockMonthlyExchangeRateCache.deleteAll()).thenReturn(Future.unit)
        when(mockEuropaConnector.retrieveMonthlyExchangeRates(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(exception))

        exchangeRateService
          .retrieveCachedMonthlyExchangeRate(exchangeRate.dateEnd)
          .failed
          .futureValue shouldBe exception
      }

    }

    "return successfully" when {

      "no cached item is present and the http call succeeds and the body of the response can be parsed" in {
        when(mockMonthlyExchangeRateCache.getMonthlyExchangeRate(previousMonthYear))
          .thenReturn(Future.successful(None))
        when(mockMonthlyExchangeRateCache.deleteAll())
          .thenReturn(Future.unit)
        when(mockEuropaConnector.retrieveMonthlyExchangeRates(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Seq(exchangeRate)))
        when(mockMonthlyExchangeRateCache.put(Seq(exchangeRate)))
          .thenReturn(Future.unit)
        when(mockMonthlyExchangeRateCache.getMonthlyExchangeRate(previousMonthYear))
          .thenReturn(Future.successful(Some(exchangeRate)))
        exchangeRateService
          .retrieveCachedMonthlyExchangeRate(exchangeRate.dateEnd)
          .futureValue
          .value shouldBe exchangeRate

      }

      "an item is present in the cache" in {
        when(mockMonthlyExchangeRateCache.getMonthlyExchangeRate(previousMonthYear))
          .thenReturn(Future.successful(Some(exchangeRate)))

        exchangeRateService
          .retrieveCachedMonthlyExchangeRate(exchangeRate.dateEnd)
          .futureValue
          .value shouldBe exchangeRate

      }

    }

  }

}
