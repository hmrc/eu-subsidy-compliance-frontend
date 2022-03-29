package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ConnectorError, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

trait UndertakingOpsSupport { this: ControllerSpec =>

  val mockEscService = mock[EscService]
  def mockCreateUndertaking(undertaking: Undertaking)(result: Either[ConnectorError, UndertakingRef]) =
    (mockEscService
      .createUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockRetrieveUndertakingWithErrorResponse(
    eori: EORI
  )(result: Either[UpstreamErrorResponse, Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertakingWithErrorResponse(_: EORI)(_: HeaderCarrier))
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

  def mockCreateSubsidy(reference: UndertakingRef, subsidyUpdate: SubsidyUpdate)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .createSubsidy(_: UndertakingRef, _: SubsidyUpdate)(_: HeaderCarrier))
      .expects(reference, subsidyUpdate, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(result: Future[UndertakingSubsidies]) =
    (mockEscService
      .retrieveSubsidy(_: SubsidyRetrieve)(_: HeaderCarrier))
      .expects(subsidyRetrieve, *)
      .returning(result)

  def mockRemoveSubsidy(reference: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(
    result: Either[ConnectorError, UndertakingRef]
  ) =
    (mockEscService
      .removeSubsidy(_: UndertakingRef, _: NonHmrcSubsidy)(_: HeaderCarrier))
      .expects(reference, nonHmrcSubsidy, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))
}
