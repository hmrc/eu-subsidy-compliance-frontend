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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityAddeed.BusinessDetailsAdded
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoteItself.BusinessEntityPromoteItselfDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityPromoted.LeadPromoteDetails
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.businessEntityUpdated.BusinessDetailsUpdated
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{CreateUndertakingResponse, EISResponse, ResponseCommonUndertaking, ResponseDetail}

import java.time.LocalDateTime

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
    leadEORI: EORI,
    removedEORI: EORI
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

}
