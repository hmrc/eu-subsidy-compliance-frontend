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

package uk.gov.hmrc.eusubsidycompliancefrontend.persistence

import org.mongodb.scala.model.Filters
import play.api.Configuration
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.JourneyStore.DefaultCacheTtl
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.PersistenceHelpers.dataKeyForType
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag

@Singleton
class JourneyStore @Inject() (
  mongoComponent: MongoComponent,
  configuration: Configuration
)(implicit ec: ExecutionContext)
    extends MongoCacheRepository[EORI](
      mongoComponent = mongoComponent,
      collectionName = "journeyStore",
      ttl = configuration
        .getOptional[FiniteDuration]("mongodb.journeyStore.expireAfter")
        .getOrElse(DefaultCacheTtl),
      timestampSupport = new CurrentTimestampSupport,
      cacheIdType = EoriIdType
    )
    with Store {

  override def get[A : ClassTag](implicit eori: EORI, reads: Reads[A]): Future[Option[A]] =
    get[A](eori)(dataKeyForType[A])

  override def put[A](in: A)(implicit eori: EORI, writes: Writes[A]): Future[A] =
    put[A](eori)(DataKey(in.getClass.getSimpleName), in).map(_ => in)

  override def getOrCreate[A : ClassTag](default: A)(implicit eori: EORI, format: Format[A]): Future[A] =
    get[A].toContext
      .getOrElseF(put(default))

  override def delete[A : ClassTag](implicit eori: EORI): Future[Unit] =
    delete[A](eori)(dataKeyForType[A])

  override def deleteAll(implicit eori: EORI): Future[Unit] =
    collection
      .findOneAndDelete(Filters.equal("_id", EoriIdType.run(eori)))
      .toFuture()
      .map(_ => ())

  override def update[A: ClassTag](f: A => A)(implicit eori: EORI, format: Format[A]): Future[A] =
    get.toContext
      .map(f)
      .foldF(throw new IllegalStateException("trying to update non-existent model"))(put(_))

}

object JourneyStore {
  val DefaultCacheTtl: FiniteDuration = 24 hours
}