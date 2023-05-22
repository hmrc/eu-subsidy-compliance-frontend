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

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.{Filters, IndexOptions, Indexes, Updates}
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.PersistenceHelpers.dataKeyForType
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.UndertakingCache.DefaultCacheTtl
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag

@Singleton
class UndertakingCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository[EORI](
      mongoComponent = mongoComponent,
      collectionName = "undertakingCache",
      ttl = DefaultCacheTtl,
      timestampSupport = new CurrentTimestampSupport,
      cacheIdType = EoriIdType
    ) {

  private val UndertakingReference = "data.Undertaking.reference"
  private val UndertakingSubsidiesIdentifier = "data.UndertakingSubsidies.undertakingIdentifier"

  // Ensure additional indexes for undertaking and undertaking subsidies deletion are present.
  private lazy val indexedCollection: Future[MongoCollection[CacheItem]] =
    for {
      _ <- collection
        .createIndex(
          Indexes.ascending(UndertakingReference),
          IndexOptions()
            .background(false)
            .name("undertakingReference")
            .sparse(false)
            .unique(false)
        )
        .headOption()
      _ <- collection
        .createIndex(
          Indexes.ascending(UndertakingSubsidiesIdentifier),
          IndexOptions()
            .background(false)
            .name("undertakingSubsidiesIdentifier")
            .sparse(false)
            .unique(false)
        )
        .headOption()
    } yield collection

  def get[A : ClassTag](eori: EORI)(implicit reads: Reads[A]): Future[Option[A]] =
    indexedCollection.flatMap { _ =>
      super
        .get[A](eori)(dataKeyForType[A])
    }

  def put[A](eori: EORI, in: A)(implicit writes: Writes[A]): Future[A] =
    indexedCollection.flatMap { _ =>
      super
        .put[A](eori)(DataKey(in.getClass.getSimpleName), in)
        .map(_ => in)
    }

  def deleteUndertaking(ref: UndertakingRef): Future[Unit] =
    indexedCollection.flatMap { c =>
      c.updateMany(
        filter = Filters.equal(UndertakingReference, ref),
        update = Updates.unset("data.Undertaking")
      ).toFuture()
        .map(_ => ())
    }

  def deleteUndertakingSubsidies(ref: UndertakingRef): Future[Unit] =
    indexedCollection.flatMap { c =>
      c.updateMany(
        filter = Filters.equal(UndertakingSubsidiesIdentifier, ref),
        update = Updates.unset("data.UndertakingSubsidies")
      ).toFuture()
        .map(_ => ())
    }

}

object UndertakingCache {
  val DefaultCacheTtl: FiniteDuration = 24 hours
}
