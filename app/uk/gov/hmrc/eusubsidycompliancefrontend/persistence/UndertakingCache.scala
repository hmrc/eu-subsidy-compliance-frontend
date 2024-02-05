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

import cats.implicits.toFunctorOps
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, Updates}
import play.api.Logging
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.PersistenceHelpers.dataKeyForType
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.UndertakingCache._
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
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
      cacheIdType = EoriIdType,
      replaceIndexes = true,
      extraIndexes = Seq(
        undertakingCacheIndex(undertakingReference, "undertakingReference"),
        undertakingCacheIndex(undertakingSubsidiesIdentifier, "undertakingSubsidiesIdentifier"),
        undertakingCacheIndex(industrySectorLimit, "sectorLimit")
      )
    )
    with Logging {

  def get[A : ClassTag : Reads](eori: EORI): Future[Option[A]] = {
    logged {
      super.get[A](eori)(dataKeyForType[A])
    }(
      preMessage = s"UndertakingCache.get is being called for EORI '$eori'",
      successMessage = s"UndertakingCache.get returned %s for  EORI '$eori'",
      errorMessage = s"UndertakingCache.get failed EORI '$eori'"
    )
  }

  private def logged[A](
    call: Future[A]
  )(preMessage: String, successMessage: String, errorMessage: String)(implicit classTag: ClassTag[A]) = {
    val forMessage = s" (for $classTag)"
    logger.info(preMessage + forMessage)

    call.failed.foreach(error => logger.error(s"$errorMessage - (${error.getMessage}) $forMessage", error))

    call.map { result =>
      logger.info(successMessage.format(result))
      result
    }
  }

  def put[A : ClassTag : Writes](eori: EORI, in: A): Future[A] = {
    logged {
      super.put[A](eori)(DataKey(in.getClass.getSimpleName), in).as(in)
    }(
      preMessage = s"UndertakingCache.put is setting $in into EURO:$eori",
      successMessage = s"UndertakingCache.put succeeded in setting $in into EURO:$eori",
      errorMessage = s"UndertakingCache.put failed in setting $in into EURO:$eori"
    )
  }

  def deleteUndertaking(ref: UndertakingRef): Future[Unit] = {
    logged {
      collection
        .updateMany(
          filter = Filters.equal(undertakingReference, ref),
          update = Updates.unset("data.Undertaking")
        )
        .toFuture()
        .void
    }(
      preMessage = s"UndertakingCache.deleteUndertaking is deleting UndertakingRef:$ref",
      successMessage = s"UndertakingCache.deleteUndertaking succeeded in deleting UndertakingRef:$ref",
      errorMessage = s"UndertakingCache.deleteUndertaking failed in deleting UndertakingRef:$ref"
    )
  }

  def deleteUndertakingSubsidies(ref: UndertakingRef): Future[Unit] = {
    logged {
      collection
        .updateMany(
          filter = Filters.equal(undertakingSubsidiesIdentifier, ref),
          update = Updates.unset("data.UndertakingSubsidies")
        )
        .toFuture()
        .void
    }(
      preMessage = s"UndertakingCache.deleteUndertakingSubsidies is deleting UndertakingRef:$ref",
      successMessage = s"UndertakingCache.deleteUndertakingSubsidies succeeded in deleting UndertakingRef:$ref",
      errorMessage = s"UndertakingCache.deleteUndertakingSubsidies failed in deleting UndertakingRef:$ref"
    )
  }
}

object UndertakingCache {
  val DefaultCacheTtl: FiniteDuration = 1 hours

  private val undertakingReference = "data.Undertaking.reference"
  private val undertakingSubsidiesIdentifier = "data.UndertakingSubsidies.undertakingIdentifier"
  private val industrySectorLimit = "data.Undertaking.industrySectorLimit"

  private def undertakingCacheIndex(field: String, name: String): IndexModel =
    IndexModel(
      Indexes.ascending(field),
      IndexOptions().background(false).name(name).sparse(false).unique(false)
    )

}
