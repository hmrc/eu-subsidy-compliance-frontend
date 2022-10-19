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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

trait EscServiceSupport { this: ControllerSpec =>

  val mockEscService = mock[EscService]

  def mockCreateUndertaking(undertaking: UndertakingCreate)(result: Either[ConnectorError, UndertakingRef]) =
    (mockEscService
      .createUndertaking(_: UndertakingCreate)(_: HeaderCarrier, _: EORI))
      .expects(undertaking, *, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockGetUndertaking(eori: EORI)(result: Future[Undertaking]) =
    (mockEscService
      .getUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockRetrieveUndertakingWithErrorResponse(
    eori: EORI
  )(result: Either[ConnectorError, Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertakingAndHandleErrors(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result.toFuture)

  def mockUpdateUndertaking(undertaking: Undertaking)(result: Either[ConnectorError, UndertakingRef]) =
    (mockEscService
      .updateUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .addMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRemoveMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .removeMember(_: UndertakingRef, _: BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockCreateSubsidy(subsidyUpdate: SubsidyUpdate)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .createSubsidy(_: SubsidyUpdate)(_: HeaderCarrier))
      .expects(subsidyUpdate, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(result: Future[UndertakingSubsidies]) =
    (mockEscService
      .retrieveSubsidies(_: SubsidyRetrieve)(_: HeaderCarrier, _: EORI))
      .expects(subsidyRetrieve, *, *)
      .returning(result)

  def mockRemoveSubsidy(reference: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .removeSubsidy(_: UndertakingRef, _: NonHmrcSubsidy)(_: HeaderCarrier))
      .expects(reference, nonHmrcSubsidy, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockDisableUndertaking(undertaking: Undertaking)(result: Either[ConnectorError, UndertakingRef]) =
    (mockEscService
      .disableUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveExchangeRate(date: LocalDate)(result: Future[ExchangeRate]) =
    (mockEscService.retrieveExchangeRate(_: LocalDate)(_: HeaderCarrier))
      .expects(date, *)
      .returning(result)

}
