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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{NonHmrcSubsidy, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityAddeed.BusinessDetailsAdded
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoteItself.BusinessEntityPromoteItselfDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoted.LeadPromoteDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityUpdated.BusinessDetailsUpdated
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyAmount, SubsidyRef, TraderRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{CreateUndertakingResponse, EISResponse, ResponseCommonUndertaking, ResponseDetail}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Form
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

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
    eisRequest: Undertaking,
    eisResponse: EISResponse
  ) extends AuditEvent {
    override val auditType: String = "CreateUndertakingEIS"
    override val transactionName: String = "CreateUndertakingEIS"
  }

  object CreateUndertaking {

    def apply(
      ggCredId: String,
      ref: UndertakingRef,
      undertaking: Undertaking,
      timeNow: LocalDateTime
    ): CreateUndertaking = {
      val eisResponse = EISResponse(
        CreateUndertakingResponse(
          ResponseCommonUndertaking("OK", timeNow),
          ResponseDetail(ref)
        )
      )
      AuditEvent.CreateUndertaking(ggCredId, undertaking, eisResponse)
    }
    import uk.gov.hmrc.eusubsidycompliancefrontend.models.json.digital.undertakingFormat //Do not delete
    implicit val writes: Writes[CreateUndertaking] = Json.writes
  }

  final case class BusinessEntityAdded(
    ggDetails: String,
    leadEORI: EORI,
    detailsAdded: BusinessDetailsAdded
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityAdded"
    override val transactionName: String = "BusinessEntityAdded"
  }

  object BusinessEntityAdded {

    def apply(ggDetails: String, leadEORI: EORI, beEORI: EORI): BusinessEntityAdded =
      AuditEvent.BusinessEntityAdded(ggDetails, leadEORI, BusinessDetailsAdded(beEORI))
    implicit val writes: Writes[BusinessEntityAdded] = Json.writes

  }

  final case class BusinessEntityRemoved(
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

  final case class BusinessEntityUpdated(
    ggDetails: String,
    leadEori: EORI,
    detailsUpdated: BusinessDetailsUpdated
  ) extends AuditEvent {
    override val auditType: String = "BusinessEntityUpdated"
    override val transactionName: String = "BusinessEntityUpdated"
  }

  object BusinessEntityUpdated {

    def apply(ggDetails: String, leadEori: EORI, updatedEORI: EORI): BusinessEntityUpdated =
      AuditEvent.BusinessEntityUpdated(ggDetails, leadEori, BusinessDetailsUpdated(updatedEORI))
    implicit val writes: Writes[BusinessEntityUpdated] = Json.writes

  }

  final case class BusinessEntityPromoted(ggDetails: String, details: LeadPromoteDetails) extends AuditEvent {
    override val auditType: String = "LeadPromotesBusinessEntity"
    override val transactionName: String = "LeadPromotesBusinessEntity"
  }

  object BusinessEntityPromoted {
    def apply(ggDetails: String, leadEORI: EORI, promotedEori: EORI): BusinessEntityPromoted =
      AuditEvent.BusinessEntityPromoted(ggDetails, LeadPromoteDetails(leadEORI, promotedEori))
    implicit val writes: Writes[BusinessEntityPromoted] = Json.writes
  }

  final case class BusinessEntityPromotedSelf(ggDetails: String, details: BusinessEntityPromoteItselfDetails)
      extends AuditEvent {
    override val auditType: String = "BusinessEntityPromotesSelf"
    override val transactionName: String = "BusinessEntityPromotesSelf"
  }

  object BusinessEntityPromotedSelf {
    def apply(ggDetails: String, oldEORI: EORI, newEORI: EORI): BusinessEntityPromotedSelf =
      AuditEvent.BusinessEntityPromotedSelf(ggDetails, BusinessEntityPromoteItselfDetails(oldEORI, newEORI))
    implicit val writes: Writes[BusinessEntityPromotedSelf] = Json.writes
  }

  final case class NonCustomsSubsidyAdded(
    ggDetails: String,
    leadEori: EORI,
    undertakingIdentifier: UndertakingRef,
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
      timeProvider: TimeProvider
    ): NonCustomsSubsidyAdded =
      AuditEvent.NonCustomsSubsidyAdded(
        ggDetails = ggDetails,
        leadEori = leadEori,
        undertakingIdentifier = undertakingRef,
        allocationDate = subsidyJourney.claimDate.value
          .map(_.toLocalDate)
          .getOrElse(sys.error("No claimdate on SubsidyJourney")),
        submissionDate = timeProvider.today,
        publicAuthority = subsidyJourney.publicAuthority.value.fold(sys.error("public Authority missing"))(Some(_)),
        traderReference =
          subsidyJourney.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
        nonHMRCSubsidyAmtEUR =
          SubsidyAmount(subsidyJourney.claimAmount.value.getOrElse(sys.error("claimAmount missing from journey"))),
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
    undertakingIdentifier: UndertakingRef
  ) extends AuditEvent {
    override val auditType: String = "NonCustomsSubsidyRemoved"
    override val transactionName: String = "NonCustomsSubsidyRemoved"
  }

  object NonCustomsSubsidyRemoved {
    implicit val writes: Writes[NonCustomsSubsidyRemoved] = Json.writes
  }

  final case class NonCustomsSubsidyUpdated(
    ggDetails: String,
    undertakingIdentifier: UndertakingRef,
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
      timeProvider: TimeProvider
    ): NonCustomsSubsidyUpdated =
      AuditEvent.NonCustomsSubsidyUpdated(
        ggDetails = ggDetails,
        undertakingIdentifier = undertakingRef,
        allocationDate = subsidyJourney.claimDate.value
          .map(_.toLocalDate)
          .getOrElse(sys.error("No claimdate on SubsidyJourney")),
        submissionDate = timeProvider.today,
        publicAuthority = subsidyJourney.publicAuthority.value.fold(sys.error("public Authority missing"))(Some(_)),
        traderReference =
          subsidyJourney.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
        nonHMRCSubsidyAmtEUR =
          SubsidyAmount(subsidyJourney.claimAmount.value.getOrElse(sys.error("claimAmount missing from journey"))),
        businessEntityIdentifier =
          subsidyJourney.addClaimEori.value.fold(sys.error("eori value missing"))(optionalEORI =>
            optionalEORI.value.map(EORI(_))
          ),
        subsidyUsageTransactionId = subsidyJourney.existingTransactionId
      )

    implicit val writes: Writes[NonCustomsSubsidyUpdated] = Json.writes
  }
}
