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

import com.mongodb.WriteConcern
import org.mongodb.scala.model.Filters
import play.api.Configuration
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.UndertakingCache.DefaultCacheTtl
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag

object EoriIdType extends CacheIdType[EORI] {
  override def run: EORI => EORI = identity
}

// TODO - got access to the collection so there may be a way forward here
@Singleton
class UndertakingCache @Inject() (
  mongoComponent: MongoComponent,
  configuration: Configuration
)(implicit ec: ExecutionContext) {

  private lazy val cache = new MongoCacheRepository[EORI](
    mongoComponent = mongoComponent,
    collectionName = "undertakingCache",
    ttl = DefaultCacheTtl,
    timestampSupport = new CurrentTimestampSupport,
    cacheIdType = EoriIdType
  )

  def get[A : ClassTag](eori: EORI)(implicit reads: Reads[A]): Future[Option[A]] = {
    println(s"Undertaking cache GET: $eori")
    cache
      .get[A](eori)(dataKeyForType[A])
  }

  def put[A](eori: EORI, in: A)(implicit writes: Writes[A]): Future[A] = {
    println(s"Undertaking cache PUT: $eori")
    cache
      .put[A](eori)(DataKey(in.getClass.getSimpleName), in)
      .map(_ => in)
  }

  def deleteUndertaking(ref: UndertakingRef): Future[Unit] = {
    println(s"Undertaking cache DELETE: $ref")
    cache
      .collection
      .withWriteConcern(WriteConcern.ACKNOWLEDGED)
      .deleteMany(Filters.equal("data.Undertaking.reference", ref))
      .toFuture()
      .map(_ => ())
  }

  private def dataKeyForType[A](implicit ct: ClassTag[A]) = DataKey[A](ct.runtimeClass.getSimpleName)

}

object UndertakingCache {
  val DefaultCacheTtl: FiniteDuration = 24 hours
}