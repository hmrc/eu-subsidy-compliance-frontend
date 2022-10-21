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

import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, Updates}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.NonHmrcSubsidy
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.{CacheItem, MongoCacheRepository}
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent, MongoUtils}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class RemovedSubsidyRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext) {

  private val SubsidiesArrayFieldName = "nonHmrcSubsidies"

  private val timestampSupport = new CurrentTimestampSupport

  protected[cache] val repository = new MongoCacheRepository[EORI](
    mongoComponent = mongoComponent,
    collectionName = "removedSubsidies",
    ttl = 365 * 3 days,
    timestampSupport = timestampSupport,
    cacheIdType = EoriIdType
  )

  def add(eori: EORI, subsidy: NonHmrcSubsidy): Future[Unit] =
    MongoUtils.retryOnDuplicateKey(retries = 3) {
      val id        = EoriIdType.run(eori)
      val timestamp = timestampSupport.timestamp()
      repository.collection
        .findOneAndUpdate(
          filter = Filters.equal("_id", id),
          update = Updates.combine(
            Updates.push(s"data.$SubsidiesArrayFieldName", Codecs.toBson(subsidy)),
            Updates.set("modifiedDetails.lastUpdated", timestamp),
            Updates.setOnInsert("_id", id),
            Updates.setOnInsert("modifiedDetails.createdAt", timestamp),
          ),
          options = FindOneAndUpdateOptions()
            .upsert(true)
        )
        .toFuture()
        .map(_ => ())
    }

  def getAll(eori: EORI): Future[Seq[NonHmrcSubsidy]] = {
    repository
      .collection
      .find(Filters.equal("_id", eori))
      .headOption()
      .map { thing: Option[CacheItem] =>
        thing.fold(Seq.empty[NonHmrcSubsidy]) { item =>
          (item.data \ SubsidiesArrayFieldName).as[Seq[NonHmrcSubsidy]]
        }
      }
  }

}

object RemovedSubsidyRepository {
  // We must store removed subsidies for 3 years.
  val DefaultTtl: FiniteDuration = 365 * 3 days
}
