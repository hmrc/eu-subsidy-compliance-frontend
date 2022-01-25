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

package utils

import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ContactDetails, NonHmrcSubsidy, SubsidyRetrieve, SubsidyUpdate, Undertaking, UndertakingSubsidies, UndertakingSubsidyAmendment}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.transport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, IndustrySectorLimit, PhoneNumber, Sector, SubsidyAmount, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, FormPage, SubsidyJourney, UndertakingJourney}

import java.time.LocalDate

object CommonTestData {
  val currentDate = LocalDate.of(2021, 1, 20)

  val eori1 = EORI("GB123456789012")
  val eori2 = EORI("GB123456789013")
  val eori3 = EORI("GB123456789014")
  val eori4 = EORI("GB123456789010")

  val businessEntity1 = BusinessEntity(EORI(eori1), true, None)
  val businessEntity2 = BusinessEntity(EORI(eori2), true, None)
  val businessEntity3 = BusinessEntity(EORI(eori3), true, None)


  val undertakingRef = UndertakingRef("UR123456")
  val nonHmrcSubsidyList = List(NonHmrcSubsidy(
    subsidyUsageTransactionId = None,
    allocationDate = currentDate,
    submissionDate = currentDate,
    publicAuthority = "Local Authority".some,
    traderReference = TraderRef("ABC123").some,
    nonHMRCSubsidyAmtEUR = SubsidyAmount(1234.56),
    businessEntityIdentifier = eori1.some,
    amendmentType = EisSubsidyAmendmentType("1").some
  ))

  val subsidyJourney = SubsidyJourney(
    publicAuthority = FormPage("add-claim-public-authority", "Local Authority".some),
    traderRef = FormPage("add-claim-reference", TraderRef("ABC123").some.some),
    claimAmount = FormPage("add-claim-date", SubsidyAmount(1234.56).some),
    addClaimEori = FormPage("add-claim-eori", eori1.some.some)
  )


  val subsidyUpdate = SubsidyUpdate(
    undertakingRef,
    UndertakingSubsidyAmendment(nonHmrcSubsidyList)

  )

  val undertaking = Undertaking(undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021,1,18).some,
    List(businessEntity1, businessEntity2))

  val subsidyRetrieve = SubsidyRetrieve(
    undertakingRef, None
  )

  val undertakingSubsidies = UndertakingSubsidies(
    undertakingRef,
    SubsidyAmount(1234.56),
    SubsidyAmount(1234.56),
    SubsidyAmount(1234.56),
    SubsidyAmount(1234.56),
    nonHmrcSubsidyList,
    List.empty
  )

  val eligibilityJourneyNotComplete = EligibilityJourney(
    customsWaivers = FormPage("do-you-claim-customs-waivers", true.some),
    willYouClaim = FormPage("will-you-claim-customs-waivers", true.some),
    notEligible = FormPage("not-eligible", false.some),
    mainBusinessCheck= FormPage("main-business-check", true.some),
    signOut= FormPage("not-eligible-to-lead", false.some),
    acceptTerms = FormPage("terms-conditions", true.some)
  )

  val eligibilityJourneyComplete = eligibilityJourneyNotComplete.copy(
  eoriCheck = FormPage("eoricheck", true.some),
  signOutBadEori = FormPage("incorrect-eori", false.some),
  createUndertaking = FormPage("create-undertaking", true.some)
  )

  val contactDetails = ContactDetails(PhoneNumber("111").some, None).some

  val undertakingJourneyComplete =  UndertakingJourney(
    name = FormPage("undertaking-name", "TestUndertaking".some),
    sector = FormPage("sector",Sector(1).some),
    contact = FormPage("contact", contactDetails),
    cya = FormPage("check-your-answers", true.some),
    confirmation = FormPage("confirmation", true.some)
  )

  val businessEntityJourney = BusinessEntityJourney(
    addBusiness = FormPage("add-member", true.some),
    eori = FormPage("add-business-entity-eori", eori1.some),
    contact = FormPage("add-business-entity-contact", contactDetails),
    cya= FormPage("check-your-answers-businesses", true.some)
  )
}
