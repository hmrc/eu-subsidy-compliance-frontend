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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{EISRequest, EISResponse}

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
    import uk.gov.hmrc.eusubsidycompliancefrontend.models.json.digital.undertakingFormat
    implicit val writes: Writes[CreateUndertaking] = Json.writes
  }

}
