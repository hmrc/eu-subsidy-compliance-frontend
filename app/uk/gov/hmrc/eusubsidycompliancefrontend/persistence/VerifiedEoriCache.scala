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
import org.mongodb.scala.model._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.VerifiedEori
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.jatLocalDateFormat
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.HOURS
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VerifiedEoriCache @Inject() (
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[VerifiedEori](
      mongoComponent = mongoComponent,
      collectionName = "verifiedEoriCache",
      domainFormat = VerifiedEori.formats,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("createDate"),
          IndexOptions()
            .name("sessionTTL")
            .expireAfter(24, HOURS)
        )
      ),
      replaceIndexes = true,
      extraCodecs = Seq(Codecs.playFormatCodec(jatLocalDateFormat))
    ) {
  def put(verifiedEori: VerifiedEori): Future[Unit] =
    collection.insertOne(verifiedEori).toFuture().void

  def get(eori: EORI): Future[Option[VerifiedEori]] =
    collection.find(filter = Filters.equal("eori", eori)).headOption()

}
