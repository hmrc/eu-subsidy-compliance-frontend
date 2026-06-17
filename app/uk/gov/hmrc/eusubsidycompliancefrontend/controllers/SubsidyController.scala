/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.*
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EisSubsidyAmendmentType.EisSubsidyAmendmentType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EisSubsidyAmendmentType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyAmount.SubsidyAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyAmount
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TraderRef.TraderRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TraderRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef.UndertakingRef

import java.time.LocalDate

object SubsidyController {
  def toSubsidyUpdate(
    journey: SubsidyJourney,
    undertakingRef: UndertakingRef,
    currentDate: LocalDate
  ): SubsidyUpdate =
    SubsidyUpdate(
      undertakingIdentifier = undertakingRef,
      UndertakingSubsidyAmendment(
        List(
          NonHmrcSubsidy(
            subsidyUsageTransactionId = journey.existingTransactionId,
            allocationDate = journey.claimDate.value
              .map(_.toLocalDate)
              .getOrElse(throw new IllegalStateException("No claimdate on SubsidyJourney")),
            submissionDate = currentDate,
            publicAuthority = Some(journey.publicAuthority.value.get),
            traderReference = journey.traderRef.value.fold(sys.error("Trader ref missing"))(_.value.map(TraderRef(_))),
            nonHMRCSubsidyAmtEUR =
              if (journey.claimAmountIsInEuros)
                SubsidyAmount(journey.getClaimAmount.getOrElse(sys.error("Claim amount Missing")))
              else
                SubsidyAmount(
                  journey.getConvertedClaimAmount.getOrElse(sys.error("Converted claim amount Missing"))
                ),
            businessEntityIdentifier = journey.addClaimEori.value
              .fold(sys.error("eori value missing"))(optionalClaimEori => optionalClaimEori.value.map(EORI(_))),
            amendmentType = journey.existingTransactionId
              .fold(Some(EisSubsidyAmendmentType("1")))(_ => Some(EisSubsidyAmendmentType("2")))
          )
        )
      )
    )
}
