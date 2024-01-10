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

import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EuropaConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.MonthlyExchangeRate
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.MonthlyExchangeRateCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps

import java.time.{LocalDate, YearMonth}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExchangeRateService @Inject() (
  europaConnector: EuropaConnector,
  monthlyExchangeRateCache: MonthlyExchangeRateCache
)(implicit ec: ExecutionContext) {
  private def getExchangeRate(implicit hc: HeaderCarrier): Future[Seq[MonthlyExchangeRate]] =
    europaConnector.retrieveMonthlyExchangeRates

  def retrieveCachedMonthlyExchangeRate(
    date: LocalDate
  )(implicit hc: HeaderCarrier): Future[Option[MonthlyExchangeRate]] = {
    val previousMonthYear = YearMonth.from(date).minusMonths(1).atEndOfMonth()
    monthlyExchangeRateCache
      .getMonthlyExchangeRate(previousMonthYear)
      .toContext
      .orElseF {
        for {
          _ <- monthlyExchangeRateCache.deleteAll()
          _ <- populateCache
          rate <- monthlyExchangeRateCache.getMonthlyExchangeRate(previousMonthYear)
        } yield rate
      }
      .value
  }

  private def populateCache(implicit hc: HeaderCarrier): Future[Unit] =
    getExchangeRate
      .flatMap(monthlyExchangeRateCache.put)
}
