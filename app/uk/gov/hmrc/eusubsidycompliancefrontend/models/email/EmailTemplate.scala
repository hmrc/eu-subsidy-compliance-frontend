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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.email

import enumeratum._
import enumeratum.EnumEntry.LowerCamelcase

sealed trait EmailTemplate extends EnumEntry with LowerCamelcase

object EmailTemplate extends Enum[EmailTemplate] {

  case object AddMemberToBusinessEntity extends EmailTemplate
  case object AddMemberToLead extends EmailTemplate
  case object CreateUndertaking extends EmailTemplate
  case object DisableUndertakingToLead extends EmailTemplate
  case object DisableUndertakingToBusinessEntity extends EmailTemplate
  case object MemberRemoveSelfToBusinessEntity extends EmailTemplate
  case object MemberRemoveSelfToLead extends EmailTemplate
  case object PromotedOtherAsLeadToBusinessEntity extends EmailTemplate
  case object PromotedOtherAsLeadToLead extends EmailTemplate
  case object PromotedSelfToNewLead extends EmailTemplate
  case object RemoveMemberToBusinessEntity extends EmailTemplate
  case object RemoveMemberToLead extends EmailTemplate
  case object RemovedAsLeadToFormerLead extends EmailTemplate

  override val values = findValues

}

