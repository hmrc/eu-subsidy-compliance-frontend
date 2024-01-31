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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.MongoCacheRepository
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import cats.implicits.toFunctorOps

@Singleton
class RemovedSubsidyRepository @Inject() (mongoComponent: MongoComponent)(implicit ec: ExecutionContext) {

  private val timestampSupport = new CurrentTimestampSupport

  protected[persistence] val repository = new MongoCacheRepository[EORI](
    mongoComponent = mongoComponent,
    collectionName = "removedSubsidies",
    ttl = 365 * 3 days,
    timestampSupport = timestampSupport,
    cacheIdType = EoriIdType
  )

  def drop(): Future[Unit] = {
    repository.collection.drop().toFuture().void
  }

}

object RemovedSubsidyRepository {
  // We must store removed subsidies for 3 years.
  val DefaultTtl: FiniteDuration = 365 * 3 days
}
