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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future
import scala.reflect.ClassTag

trait JourneyStoreSupport { this: MockFactory =>

  val mockJourneyStore: Store = mock[Store]

  def mockGet[A](eori: EORI)(result: Either[ConnectorError, Option[A]]) =
    (mockJourneyStore
      .get(_: ClassTag[A], _: EORI, _: Reads[A]))
      .expects(*, eori, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockGetOrCreate[A](eori: EORI)(result: Either[ConnectorError, A]) =
    (mockJourneyStore
      .getOrCreate(_: A)(_: ClassTag[A], _: EORI, _: Format[A]))
      .expects(*, *, eori, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockPut[A](input: A, eori: EORI)(result: Either[ConnectorError, A]) =
    (mockJourneyStore
      .put(_: A)(_: EORI, _: Writes[A]))
      .expects(input, eori, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockUpdate[A](f: A => A, eori: EORI)(result: Either[ConnectorError, A]) =
    (mockJourneyStore
      .update(_: A => A)(_: ClassTag[A], _: EORI, _: Format[A]))
      .expects(*, *, eori, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockDelete[A](eori: EORI)(result: Either[ConnectorError, Unit]) =
    (mockJourneyStore
      .delete(_: ClassTag[A], _: EORI))
      .expects(*, eori)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockDeleteAll(eori: EORI)(result: Either[ConnectorError, Unit]) =
    (mockJourneyStore
      .deleteAll(_: EORI))
      .expects(eori)
      .returning(result.fold(Future.failed, _.toFuture))

}
