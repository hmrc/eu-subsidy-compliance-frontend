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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler3, CallHandler4}
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future
import scala.reflect.ClassTag

trait JourneyStoreSupport { this: MockFactory =>

  object journeyStoreSupport {
    val mockJourneyStore: Store = mock[Store]

    def mockGet[A](
      eori: EORI
    )(result: Either[ConnectorError, Option[A]]): CallHandler3[ClassTag[A], EORI, Reads[A], Future[Option[A]]] =
      (mockJourneyStore
        .get(_: ClassTag[A], _: EORI, _: Reads[A]))
        .expects(*, eori, *)
        .returning(result.fold(Future.failed, _.toFuture))

    def mockGetOrCreate[A](
      eori: EORI
    )(result: Either[ConnectorError, A]): CallHandler4[A, ClassTag[A], EORI, Format[A], Future[A]] =
      (mockJourneyStore
        .getOrCreate(_: A)(_: ClassTag[A], _: EORI, _: Format[A]))
        .expects(*, *, eori, *)
        .returning(result.fold(Future.failed, _.toFuture))

    def mockPut[A](input: A, eori: EORI)(
      result: Either[ConnectorError, A]
    ): CallHandler3[A, EORI, Writes[A], Future[A]] =
      (mockJourneyStore
        .put(_: A)(_: EORI, _: Writes[A]))
        .expects(input, eori, *)
        .returning(result.fold(Future.failed, _.toFuture))

    def mockUpdate[A](
      eori: EORI
    )(result: Either[ConnectorError, A]): CallHandler4[A => A, ClassTag[A], EORI, Format[A], Future[A]] =
      (mockJourneyStore
        .update(_: A => A)(_: ClassTag[A], _: EORI, _: Format[A]))
        .expects(*, *, eori, *)
        .returning(result.fold(Future.failed, _.toFuture))

    def mockDelete[A](eori: EORI)(result: Either[ConnectorError, Unit]): CallHandler2[ClassTag[A], EORI, Future[Unit]] =
      (mockJourneyStore
        .delete(_: ClassTag[A], _: EORI))
        .expects(*, eori)
        .returning(result.fold(Future.failed, _.toFuture))

    def mockDeleteAll(eori: EORI)(result: Either[ConnectorError, Unit]): CallHandler1[EORI, Future[Unit]] =
      (mockJourneyStore
        .deleteAll(_: EORI))
        .expects(eori)
        .returning(result.fold(Future.failed, _.toFuture))
  }

}
