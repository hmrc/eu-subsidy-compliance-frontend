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

package uk.gov.hmrc.eusubsidycompliancefrontend.cache

import uk.gov.hmrc.eusubsidycompliancefrontend.cache.ExchangeRateCache.DefaultCacheTtl
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.cache.{CacheIdType, MongoCacheRepository}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

case class YearAndMonth(year: Int, month: Int) {
  override def toString: String = f"$year%04d$month%02d"
}

object YearAndMonth {
  def fromDate(d: LocalDate): YearAndMonth = YearAndMonth(d.getYear, d.getMonthValue)
}

object YearAndMonthIdType extends CacheIdType[YearAndMonth] {
  override def run: YearAndMonth => String = _.toString
}

@Singleton
class ExchangeRateCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
  extends MongoCacheRepository[YearAndMonth](
    mongoComponent = mongoComponent,
    collectionName = "exchangeRateCache",
    ttl = DefaultCacheTtl,
    timestampSupport = new CurrentTimestampSupport,
    cacheIdType = YearAndMonthIdType
  )
    {


}

object ExchangeRateCache {
  val DefaultCacheTtl: FiniteDuration = 30 days
}
