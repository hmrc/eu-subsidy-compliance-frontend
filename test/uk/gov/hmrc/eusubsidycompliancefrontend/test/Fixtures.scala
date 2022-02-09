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

package uk.gov.hmrc.eusubsidycompliancefrontend.test

import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, HmrcSubsidy, NonHmrcSubsidy, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{DeclarationID, EORI, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, TaxType, TraderRef, UndertakingName, UndertakingRef}

import java.time.{Instant, ZoneId}

object Fixtures {

  val eori = EORI("GB123456789012")
  val fixedInstant = Instant.parse("2022-01-01T12:00:00Z")

  val undertakingReference = UndertakingRef("SomeUndertakingReference")
  val undertakingName = UndertakingName("SomeUndertakingName")
  val sector = Sector.other
  val industrySectorLimit = IndustrySectorLimit(BigDecimal(200000.00))
  val date = fixedInstant.atZone(ZoneId.of("Europe/London")).toLocalDate
  val subsidyAmount = SubsidyAmount(BigDecimal(123.45))

  val undertaking = Undertaking(
    Some(undertakingReference),
    undertakingName,
    sector,
    Some(industrySectorLimit),
    Some(date),
    List(BusinessEntity(eori, leadEORI = true, None))
  )

  val subsidyRef = SubsidyRef("ABC12345")
  val declarationId = DeclarationID("12345")
  val traderRef = TraderRef("SomeTraderReference")
  val taxType = TaxType("1")
  val publicAuthority = "SomePublicAuthority"

  val hmrcSubsidy = HmrcSubsidy(
    declarationID = declarationId,
    issueDate = Some(date),
    acceptanceDate = date,
    declarantEORI = eori,
    consigneeEORI = eori,
    taxType = Some(taxType),
    amount = Some(subsidyAmount),
    tradersOwnRefUCR = Some(traderRef)
  )

  val nonHmrcSubsidy = NonHmrcSubsidy(
    subsidyUsageTransactionID = Some(subsidyRef),
    allocationDate = date,
    submissionDate = date,
    publicAuthority = Some(publicAuthority),
    traderReference = Some(traderRef),
    nonHMRCSubsidyAmtEUR = subsidyAmount,
    businessEntityIdentifier = Some(eori),
    amendmentType = None,
  )

  val undertakingSubsidies = UndertakingSubsidies(
    undertakingIdentifier = undertakingReference,
    nonHMRCSubsidyTotalEUR = subsidyAmount,
    nonHMRCSubsidyTotalGBP = subsidyAmount,
    hmrcSubsidyTotalEUR = subsidyAmount,
    hmrcSubsidyTotalGBP = subsidyAmount,
    nonHMRCSubsidyUsage = List(nonHmrcSubsidy),
    hmrcSubsidyUsage = List(hmrcSubsidy)
  )

}
