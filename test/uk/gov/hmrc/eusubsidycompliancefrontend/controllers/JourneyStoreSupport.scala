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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Store

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait JourneyStoreSupport {this: MockFactory =>

   val mockJourneyStore = mock[Store]

   def mockGet[A](eori: EORI)(result: Either[Error, Option[A]])(implicit ec: ExecutionContext) =
      (mockJourneyStore
        .get(_:ClassTag[Any], _: EORI, _: Reads[Any]))
        .expects(*, eori, *)
        .returning(result.fold(_ => Future.failed(sys.error("update failed")),Future.successful(_)))


   def mockPut[A](input: A, eori: EORI)(result: Either[Error, A])(implicit ec: ExecutionContext) =
      (mockJourneyStore
        .put(_:A)(_: EORI, _: Writes[A]))
        .expects(input, eori, *)
        .returning(result.fold(_ => Future.failed(sys.error("update failed")),Future.successful(_)))

   def update[A](f: Option[A] => Option[A], eori: EORI)(result: Either[Error, A])(implicit ec: ExecutionContext) =
      (mockJourneyStore
        .update(_: Option[A] => Option[A])(_:ClassTag[A], _: EORI, _: Format[A]))
        .expects(f, *, eori, * )
        .returning(result.fold(_ => Future.failed(sys.error("update failed")),Future.successful(_)))


}
