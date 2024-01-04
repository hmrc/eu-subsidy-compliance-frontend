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
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.PersistenceHelpers.dataKeyForType
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.UndertakingCache.DefaultCacheTtl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent, MongoUtils}

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
    )
    with TracedLogging {

  private lazy val undertakingReference = "data.Undertaking.reference"
  private lazy val undertakingSubsidiesIdentifier = "data.UndertakingSubsidies.undertakingIdentifier"
  private lazy val industrySectorLimit = "data.Undertaking.industrySectorLimit"
  lazy val allIndexes: Seq[IndexModel] = indexes ++ Seq(
    IndexModel(
      Indexes.ascending(undertakingReference),
      IndexOptions().background(false).name("undertakingReference").sparse(false).unique(false)
    ),
    IndexModel(
      Indexes.ascending(undertakingSubsidiesIdentifier),
      IndexOptions().background(false).name("undertakingSubsidiesIdentifier").sparse(false).unique(false)
    ),
    IndexModel(
      Indexes.ascending(industrySectorLimit),
      IndexOptions().background(false).name("sectorLimit").sparse(false).unique(false)
    )
  )

  override def ensureIndexes(): Future[Seq[String]] = {
    MongoUtils.ensureIndexes(collection, allIndexes, replaceIndexes = true)
  }

  def get[A : ClassTag](eori: EORI)(implicit reads: Reads[A], headerCarrier: HeaderCarrier): Future[Option[A]] = {
    logged {
      super.get[A](eori)(dataKeyForType[A])
    }(
      preMessage = s"UndertakingCache.get is being called for EORI '$eori'",
      successMessage = s"UndertakingCache.get returned %s for  EORI '$eori'",
      errorMessage = s"UndertakingCache.get failed EORI '$eori'"
    )
  }

  private def logged[A](call: Future[A])(preMessage: String, successMessage: String, errorMessage: String)(implicit
    classTag: ClassTag[A],
    headerCarrier: HeaderCarrier
  ) = {
    val forMessage = s" (for $classTag)"
    logger.info(preMessage + forMessage)

    call.failed.foreach(error => logger.error(errorMessage + s"(${error.getMessage})" + forMessage, error))

    call.map { result =>
      logger.info(successMessage.format(result))
      result
    }
  }

  def put[A](eori: EORI, in: A)(implicit
    writes: Writes[A],
    classTag: ClassTag[A],
    headerCarrier: HeaderCarrier
  ): Future[A] = {
    logged {
      super.put[A](eori)(DataKey(in.getClass.getSimpleName), in).as(in)
    }(
      preMessage = s"UndertakingCache.put is setting $in into EURO:$eori",
      successMessage = s"UndertakingCache.put succeeded in setting $in into EURO:$eori",
      errorMessage = s"UndertakingCache.put failed in setting $in into EURO:$eori"
    )
  }

  def deleteUndertaking(ref: UndertakingRef)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
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

  def deleteUndertakingSubsidies(ref: UndertakingRef)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
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
  val DefaultCacheTtl: FiniteDuration = 24 hours
}
