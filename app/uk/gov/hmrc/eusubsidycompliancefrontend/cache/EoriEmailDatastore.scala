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
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore.DefaultCacheTtl
import uk.gov.hmrc.eusubsidycompliancefrontend.models.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.cache.{CacheItem, MongoCacheRepository}
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps


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


  def verifyEmail(key: EORI): Future[CacheItem] = {
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

  def addVerificationRequest(key: EORI, email: String, verificationId: String): Future[Option[VerifiedEmail]] = {
    val timestamp = Instant.now()

    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", key),
        update = Updates.combine(
          Updates.set("data.email", email),
          Updates.set("data.verificationId", verificationId),
          Updates.set("data.verified", false),
          Updates.set("modifiedDetails.lastUpdated", timestamp),
          Updates.set("modifiedDetails.createdAt", timestamp)
        ),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .headOption()
      .map(e => {
        e.flatMap(cache => cache.data.asOpt[VerifiedEmail])
      })
  }

  def approveVerificationRequest(key: EORI, verificationId: String): Future[UpdateResult] = {
    val timestamp = Instant.now()
    collection
      .updateOne(
        filter = Filters.and(
          Filters.equal("_id", key),
          Filters.equal("data.verificationId", verificationId)
        ),
          update = Updates.combine(
            Updates.set("data.verified", true),
            Updates.set("modifiedDetails.lastUpdated", timestamp)
        )
      ).toFuture()
  }

  def put(eori: EORI, state: VerifiedEmail) ={
    val timestamp = Instant.now()
    collection
      .findOneAndUpdate(
        filter = Filters.equal("_id", eori),
        update = Updates.combine(
          Updates.set("data.email", state.email),
          Updates.set("data.verificationId", state.verificationId),
          Updates.set("data.verified", state.verified),
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
      .map(e => e.flatMap(cache => cache.data.asOpt[VerifiedEmail])
    )
  }
}

object EoriEmailDatastore {
  val DefaultCacheTtl: FiniteDuration = 365 days
}
