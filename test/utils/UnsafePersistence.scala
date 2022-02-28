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

package utils

import play.api.libs.json._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Store

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class UnsafePersistence extends Store {

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private var store: Map[EORI, JsValue] = Map.empty

  override def get[A : ClassTag](implicit eori: EORI, reads: Reads[A]): Future[Option[A]] =
    Future.successful(store.get(eori).flatMap(x => Json.fromJson[A](x).asOpt))

  override def put[A](in: A)(implicit eori: EORI, writes: Writes[A]): Future[A] = {
    store = store + (eori -> Json.toJson(in))
    Future.successful(in)
  }

  override def update[A : ClassTag](
    f: Option[A] => Option[A]
  )(implicit
    eori: EORI,
    format: Format[A]
  ): Future[A] =
    get
      .map(f)
      .flatMap(x => x.fold(throw new IllegalStateException("trying to update non-existent model"))(put(_)))

}
