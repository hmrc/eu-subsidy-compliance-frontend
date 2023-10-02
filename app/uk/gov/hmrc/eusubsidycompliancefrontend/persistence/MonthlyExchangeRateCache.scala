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
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.eusubsidycompliancefrontend.models.MonthlyExchangeRate
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent
import org.mongodb.scala.model._
import play.api.libs.json.Writes

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DAYS
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class MonthlyExchangeRateCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MonthlyExchangeRate](
      mongoComponent = mongoComponent,
      collectionName = "exchangeRateMonthlyCache",
      domainFormat = MonthlyExchangeRate.formats,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("dateEnd"),
          IndexOptions()
            .name("sessionTTL")
            .expireAfter(365L, DAYS)
        )
      )
    ) {
  def put(data: Seq[MonthlyExchangeRate]): Future[Unit] = {
    collection.insertMany(data).toFuture().map { _ =>
      ()
    }
  }
  def drop: Future[Unit] = {
    collection.drop().toFuture().map { _ =>
      ()
    }
  }
  def getMonthlyExchangeRate(date: String): Future[Option[MonthlyExchangeRate]] = {
    collection.find(filter = Filters.equal("dateEnd", date)).headOption()
  }
}
