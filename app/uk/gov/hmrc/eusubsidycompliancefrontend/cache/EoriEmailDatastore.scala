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

import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import org.mongodb.scala.result.UpdateResult
import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore.DefaultCacheTtl
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.Helpers.dataKeyForType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.EmailVerificationState
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.reflect.ClassTag


@Singleton
class EoriEmailDatastore @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
  extends MongoCacheRepository[EORI](
    mongoComponent = mongoComponent,
    collectionName = "eoriEmailStore",
    ttl = DefaultCacheTtl,
    timestampSupport = new CurrentTimestampSupport,
    cacheIdType = EoriIdType
  ) {


  def get[A : ClassTag](key: EORI)(implicit reads: Reads[A]): Future[Option[A]] =
    super.get[A](key)(dataKeyForType[A])

  def put[A : ClassTag](key: EORI, in: A)(implicit writes: Writes[A]): Future[A] =
    super
      .put[A](key)(dataKeyForType[A], in)
      .map(_ => in)

  def verifyEmail(key: EORI) = {
    val timestamp = Instant.now()
    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", key),
        update = Updates.combine(
          Updates.set("data.verified", true),
          Updates.set("modifiedDetails.lastUpdated", timestamp),
          Updates.set("modifiedDetails.createdAt", timestamp)
      ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
  }

  def addVerificationRequest(key: EORI, email: String, pendingVerificationId: String) = {
    val timestamp = Instant.now()
    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", key),
        update = Updates.combine(
          Updates.set("data.email", email),
          Updates.set("data.pendingVerificationId", pendingVerificationId),
          Updates.set("data.verified", false),
          Updates.set("modifiedDetails.lastUpdated", timestamp),
          Updates.set("modifiedDetails.createdAt", timestamp)
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
  }

  def verifyVerificationRequest(key: EORI, pendingVerificationCode: String): Future[UpdateResult] = {
    val timestamp = Instant.now()
    collection
      .updateOne(
        filter = Filters.and(
          Filters.equal("_id", key),
          Filters.equal("data.pendingVerificationId", pendingVerificationCode)
        ),
          update = Updates.combine(
            Updates.set("data.verified", true),
            Updates.set("modifiedDetails.lastUpdated", timestamp)
        )
      ).toFuture()
  }

  def put(eori: EORI, state: EmailVerificationState) ={
    val timestamp = Instant.now()
    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", eori),
        update = Updates.combine(
          Updates.set("data.email", state.email),
          Updates.set("data.pendingVerificationId", state.pendingVerificationId.getOrElse("")),
          Updates.set("data.verified", state.verified.getOrElse(false)),
          Updates.set("modifiedDetails.lastUpdated", timestamp),
          Updates.set("modifiedDetails.createdAt", timestamp)
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
  }

  def getEmailVerification(key: EORI) = {
    collection
      .find(
        filter = Filters.and(
          Filters.equal("_id", key),
          Filters.equal("data.verified", true),
        )
      )
      .headOption()
      .map(e => {
        e.flatMap(cache => cache.data.asOpt[EmailVerificationState])
      }
    )
  }
}

object EoriEmailDatastore {
  val DefaultCacheTtl: FiniteDuration = 365 days
}
