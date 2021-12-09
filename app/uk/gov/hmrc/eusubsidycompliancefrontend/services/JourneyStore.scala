/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object EoriIdType extends CacheIdType[EORI]{
  def run: EORI => EORI = identity
}

@Singleton
class JourneyStore @Inject()(
  mongoComponent  : MongoComponent,
  configuration   : Configuration
)(implicit ec: ExecutionContext
) extends MongoCacheRepository[EORI](
  mongoComponent   = mongoComponent,
  collectionName   = "journeyStore",
  ttl              = configuration.get[FiniteDuration]("mongodb.journeyStore.expireAfter"),
  timestampSupport = new CurrentTimestampSupport,
  cacheIdType      = EoriIdType
) with Store {

  override def get[A: ClassTag](implicit eori: EORI, reads: Reads[A]): Future[Option[A]] = {
    val modelType = implicitly[ClassTag[A]].runtimeClass.getSimpleName
    get[A](eori)(DataKey(modelType))
  }

  override def put[A](in: A)(implicit eori: EORI, writes: Writes[A]): Future[A] =
    put[A](eori)(DataKey(in.getClass.getSimpleName), in).map(_=> in)
}
