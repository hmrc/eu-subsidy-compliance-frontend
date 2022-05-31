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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.audit

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.UndertakingCreate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityAddeed.BusinessDetailsAdded
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoteItself.BusinessEntityPromoteItselfDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoted.LeadPromoteDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityUpdated.BusinessDetailsUpdated
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{CreateUndertakingResponse, EISResponse, ResponseCommonUndertaking, ResponseDetail}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyAmount, SubsidyRef, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney

import java.time.{LocalDate, LocalDateTime}

sealed trait AuditEvent {

  val auditType: String

  val transactionName: String

}

object AuditEvent {

  final case class TermsAndConditionsAccepted(
    eori: EORI
  ) extends AuditEvent {
    override val auditType: String = "TermsAndConditionsAcceptedDetails"
    override val transactionName: String = "terms-and-conditions-accepted"
  }

  object TermsAndConditionsAccepted {
    implicit val writes: Writes[TermsAndConditionsAccepted] = Json.writes
  }

  final case class CreateUndertaking(
    ggDetails: String,
    eisRequest: UndertakingCreate,
    eisResponse: EISResponse
  ) extends AuditEvent {
    override val auditType: String = "CreateUndertakingEIS"
    override val transactionName: String = "CreateUndertakingEIS"
  }

  object CreateUndertaking {

    def apply(
      ggCredId: String,
      ref: UndertakingRef,
      undertaking: UndertakingCreate,
      timeNow: LocalDateTime
    ): CreateUndertaking = {
      val eisResponse = EISResponse(
        CreateUndertakingResponse(
          ResponseCommonUndertaking("OK", timeNow),
          ResponseDetail(ref)
        )
      )
      AuditEvent.CreateUndertaking(ggCredId, undertaking, eisResponse)
    } //Do not delete
    implicit val writes: Writes[CreateUndertaking] = Json.writes
  }

  final case class BusinessEntityAdded(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    leadEORI: EORI,
    detailsAdded: BusinessDetailsAdded
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityAdded"
    override val transactionName: String = "BusinessEntityAdded"
  }

  object BusinessEntityAdded {

    def apply(ref: UndertakingRef, ggDetails: String, leadEORI: EORI, beEORI: EORI): BusinessEntityAdded =
      AuditEvent.BusinessEntityAdded(ref, ggDetails, leadEORI, BusinessDetailsAdded(beEORI))
    implicit val writes: Writes[BusinessEntityAdded] = Json.writes

  }

  final case class BusinessEntityRemoved(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    leadEori: EORI,
    removedEori: EORI
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityRemoved"
    override val transactionName: String = "BusinessEntityRemoved"
  }
  object BusinessEntityRemoved {
    implicit val writes: Writes[BusinessEntityRemoved] = Json.writes
  }

  final case class BusinessEntityRemovedSelf(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    leadEori: EORI,
    removedEori: EORI
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityRemovedSelf"
    override val transactionName: String = "BusinessEntityRemovedSelf"
  }
  object BusinessEntityRemovedSelf {
    implicit val writes: Writes[BusinessEntityRemovedSelf] = Json.writes
  }

  final case class BusinessEntityUpdated(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    leadEori: EORI,
    detailsUpdated: BusinessDetailsUpdated
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityUpdated"
    override val transactionName: String = "BusinessEntityUpdated"
  }

  object BusinessEntityUpdated {

    def apply(
      undertakingReference: UndertakingRef,
      ggDetails: String,
      leadEori: EORI,
      updatedEORI: EORI
    ): BusinessEntityUpdated =
      AuditEvent.BusinessEntityUpdated(undertakingReference, ggDetails, leadEori, BusinessDetailsUpdated(updatedEORI))
    implicit val writes: Writes[BusinessEntityUpdated] = Json.writes

  }

  final case class BusinessEntityPromoted(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    details: LeadPromoteDetails
  ) extends AuditEvent {
    override val auditType: String = "LeadPromotesBusinessEntity"
    override val transactionName: String = "LeadPromotesBusinessEntity"
  }

  object BusinessEntityPromoted {
    def apply(
      undertakingReference: UndertakingRef,
      ggDetails: String,
      leadEORI: EORI,
      promotedEori: EORI
    ): BusinessEntityPromoted =
      AuditEvent.BusinessEntityPromoted(undertakingReference, ggDetails, LeadPromoteDetails(leadEORI, promotedEori))
    implicit val writes: Writes[BusinessEntityPromoted] = Json.writes
  }

  final case class BusinessEntityPromotedSelf(
    undertakingReference: UndertakingRef,
    ggDetails: String,
    details: BusinessEntityPromoteItselfDetails
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityPromotesSelf"
    override val transactionName: String = "BusinessEntityPromotesSelf"
  }

  object BusinessEntityPromotedSelf {
    def apply(
      undertakingReference: UndertakingRef,
      ggDetails: String,
      oldEORI: EORI,
      newEORI: EORI
    ): BusinessEntityPromotedSelf =
      AuditEvent.BusinessEntityPromotedSelf(
        undertakingReference,
        ggDetails,
        BusinessEntityPromoteItselfDetails(oldEORI, newEORI)
      )
    implicit val writes: Writes[BusinessEntityPromotedSelf] = Json.writes
  }

  final case class NonCustomsSubsidyAdded(
    ggDetails: String,
    leadEori: EORI,
    undertakingReference: UndertakingRef,
    allocationDate: LocalDate,
    submissionDate: LocalDate,
    publicAuthority: Option[String],
    traderReference: Option[TraderRef],
    nonHMRCSubsidyAmtEUR: SubsidyAmount,
    businessEntityIdentifier: Option[EORI],
    subsidyUsageTransactionId: Option[SubsidyRef]
  ) extends AuditEvent {
    override val auditType: String = "NonCustomsSubsidyAdded"
    override val transactionName: String = "NonCustomsSubsidyAdded"
  }

  object NonCustomsSubsidyAdded {
    def apply(
      ggDetails: String,
      leadEori: EORI,
      undertakingRef: UndertakingRef,
      subsidyJourney: SubsidyJourney,
      currentDate: LocalDate
    ): NonCustomsSubsidyAdded =
      AuditEvent.NonCustomsSubsidyAdded(
        ggDetails = ggDetails,
        leadEori = leadEori,
        undertakingReference = undertakingRef,
        allocationDate = subsidyJourney.claimDate.value
          .map(_.toLocalDate)
          .getOrElse(sys.error("No claimdate on SubsidyJourney")),
        submissionDate = currentDate,
        publicAuthority = subsidyJourney.publicAuthority.value.fold(sys.error("public Authority missing"))(Some(_)),
        traderReference =
          subsidyJourney.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
        nonHMRCSubsidyAmtEUR =
          SubsidyAmount(subsidyJourney.claimAmount.value.getOrElse(sys.error("claimAmount is missing"))),
        businessEntityIdentifier =
          subsidyJourney.addClaimEori.value.fold(sys.error("eori value missing"))(optionalEORI =>
            optionalEORI.value.map(EORI(_))
          ),
        subsidyUsageTransactionId = subsidyJourney.existingTransactionId
      )

    implicit val writes: Writes[NonCustomsSubsidyAdded] = Json.writes
  }

  final case class NonCustomsSubsidyRemoved(
    ggDetails: String,
    undertakingReference: UndertakingRef
  ) extends AuditEvent {
    override val auditType: String = "NonCustomsSubsidyRemoved"
    override val transactionName: String = "NonCustomsSubsidyRemoved"
  }

  object NonCustomsSubsidyRemoved {
    implicit val writes: Writes[NonCustomsSubsidyRemoved] = Json.writes
  }

  final case class NonCustomsSubsidyUpdated(
    ggDetails: String,
    undertakingReference: UndertakingRef,
    allocationDate: LocalDate,
    submissionDate: LocalDate,
    publicAuthority: Option[String],
    traderReference: Option[TraderRef],
    nonHMRCSubsidyAmtEUR: SubsidyAmount,
    businessEntityIdentifier: Option[EORI],
    subsidyUsageTransactionId: Option[SubsidyRef]
  ) extends AuditEvent {
    override val auditType: String = "NonCustomsSubsidyUpdated"
    override val transactionName: String = "NonCustomsSubsidyUpdated"
  }

  object NonCustomsSubsidyUpdated {
    def apply(
      ggDetails: String,
      undertakingRef: UndertakingRef,
      subsidyJourney: SubsidyJourney,
      currentDate: LocalDate
    ): NonCustomsSubsidyUpdated =
      AuditEvent.NonCustomsSubsidyUpdated(
        ggDetails = ggDetails,
        undertakingReference = undertakingRef,
        allocationDate = subsidyJourney.claimDate.value
          .map(_.toLocalDate)
          .getOrElse(sys.error("No claimdate on SubsidyJourney")),
        submissionDate = currentDate,
        publicAuthority = subsidyJourney.publicAuthority.value.fold(sys.error("public Authority missing"))(Some(_)),
        traderReference =
          subsidyJourney.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
        nonHMRCSubsidyAmtEUR =
          SubsidyAmount(subsidyJourney.claimAmount.value.getOrElse(sys.error("claimAmount is missing"))),
        businessEntityIdentifier =
          subsidyJourney.addClaimEori.value.fold(sys.error("eori value missing"))(optionalEORI =>
            optionalEORI.value.map(EORI(_))
          ),
        subsidyUsageTransactionId = subsidyJourney.existingTransactionId
      )

    implicit val writes: Writes[NonCustomsSubsidyUpdated] = Json.writes
  }

  final case class NonCustomsSubsidyNilReturn(
    ggDetails: String,
    leadEori: EORI,
    undertakingReference: UndertakingRef,
    nilSubmissionDate: LocalDate
  ) extends AuditEvent {
    override val auditType: String = "NonCustomsSubsidyNilReturn"
    override val transactionName: String = "NonCustomsSubsidyNilReturn"
  }

  object NonCustomsSubsidyNilReturn {
    implicit val writes: Writes[NonCustomsSubsidyNilReturn] = Json.writes
  }

  final case class UndertakingUpdated(
    ggDetails: String,
    leadEori: EORI,
    undertakingReference: UndertakingRef,
    undertakingName: UndertakingName,
    industrySector: Sector
  ) extends AuditEvent {
    override val auditType: String = "UndertakingUpdated"
    override val transactionName: String = "UndertakingUpdated"
  }
  object UndertakingUpdated {
    implicit val writes: Writes[UndertakingUpdated] = Json.writes
  }

  final case class UndertakingDisabled(
    ggDetails: String,
    undertakingReference: UndertakingRef,
    disablementStartDate: LocalDate
  ) extends AuditEvent {
    override val auditType: String = "UndertakingDisabled"
    override val transactionName: String = "UndertakingDisabled"
  }

  object UndertakingDisabled {
    implicit val writes: Writes[UndertakingDisabled] = Json.writes
  }
}
