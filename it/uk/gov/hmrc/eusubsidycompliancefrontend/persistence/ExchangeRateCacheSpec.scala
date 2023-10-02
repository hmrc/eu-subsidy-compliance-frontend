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

package uk.gov.hmrc.eusubsidycompliancefrontend.persistence

import uk.gov.hmrc.eusubsidycompliancefrontend.models.MonthlyExchangeRate
import uk.gov.hmrc.eusubsidycompliancefrontend.util.IntegrationBaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class ExchangeRateCacheSpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[MonthlyExchangeRate] {

  override protected def repository = new MonthlyExchangeRateCache(mongoComponent)

  private val exchangeRate1 = MonthlyExchangeRate("GBP", "EUR", BigDecimal(0.867), "01/09/2023", "30/09/2023")
  private val exchangeRate2 = MonthlyExchangeRate("GBP", "EUR", BigDecimal(0.807), "01/08/2023", "31/08/2023")
  private val exchangeRateSeq = Seq(exchangeRate1, exchangeRate2)

  "ExchangeRateCache" should {

    "return None when the cache is empty" in {
      repository.getMonthlyExchangeRate("30/09/2023").futureValue shouldBe None
    }

    "return None when there is no matching item in the cache" in {
      repository.put(exchangeRateSeq).futureValue shouldBe ()
      repository.getMonthlyExchangeRate("01/09/2058").futureValue shouldBe None
    }

    "clear the repository when drop is called" in {
      repository.put(exchangeRateSeq).futureValue
      repository.drop.futureValue
      repository.getMonthlyExchangeRate(exchangeRate1.dateEnd).futureValue shouldBe None
    }

    "return the item when present in the cache" in {
      repository.put(exchangeRateSeq).futureValue shouldBe ()
      repository.getMonthlyExchangeRate(exchangeRate1.dateEnd).futureValue shouldBe Some(exchangeRate1)
    }

  }

}
