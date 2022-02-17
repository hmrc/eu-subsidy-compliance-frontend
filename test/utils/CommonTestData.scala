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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.transport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EisSubsidyAmendmentType, IndustrySectorLimit, PhoneNumber, Sector, SubsidyAmount, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.SingleEORIEmailParameter
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.services._

import java.time.{LocalDate, LocalDateTime}

object CommonTestData {
  val currentDate = LocalDate.of(2021, 1, 20)

  val eori1 = EORI("GB123456789012")
  val eori2 = EORI("GB123456789013")
  val eori3 = EORI("GB123456789014")
  val eori4 = EORI("GB123456789010")

  val contactDetails = ContactDetails(PhoneNumber("111").some, None).some
  val contactDetails1 = ContactDetails(PhoneNumber("1121").some, None).some
  val contactDetails2 = ContactDetails(PhoneNumber("222").some, PhoneNumber("333").some).some

  val businessEntity1 = BusinessEntity(EORI(eori1), true, None)
  val businessEntity2 = BusinessEntity(EORI(eori2), true, None)
  val businessEntity3 = BusinessEntity(EORI(eori3), true, None)
  val businessEntity4 = BusinessEntity(EORI(eori4), false, contactDetails1)
  val businessEntity5 = BusinessEntity(EORI(eori1), true, contactDetails)

  val optionalTraderRef = OptionalTraderRef("true", TraderRef("ABC123").some)
  val optionalEORI = OptionalEORI("true", eori1.some)


  val undertakingRef = UndertakingRef("UR123456")
  val nonHmrcSubsidyList = List(NonHmrcSubsidy(
    subsidyUsageTransactionID = None,
    allocationDate = LocalDate.of(2022, 1, 1),
    submissionDate = currentDate,
    publicAuthority = "Local Authority".some,
    traderReference = TraderRef("ABC123").some,
    nonHMRCSubsidyAmtEUR = SubsidyAmount(1234.56),
    businessEntityIdentifier = eori1.some,
    amendmentType = EisSubsidyAmendmentType("1").some
  ))

  val subsidyJourney = SubsidyJourney(
    publicAuthority = FormPage("add-claim-public-authority", "Local Authority".some),
    traderRef = FormPage("add-claim-reference", optionalTraderRef.some),
    claimAmount = FormPage("add-claim-date", SubsidyAmount(1234.56).some),
    addClaimEori = FormPage("add-claim-eori", optionalEORI.some),
    claimDate = FormPage("add-claim-date", DateFormValues("1", "1", "2022").some)
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

  val undertaking1 = Undertaking(undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021,1,18).some,
    List(businessEntity1, businessEntity4))

  val undertaking2 = Undertaking(undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021,1,18).some,
    List(businessEntity4))

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



  val undertakingJourneyComplete =  UndertakingJourney(
    name = FormPage("undertaking-name", "TestUndertaking".some),
    sector = FormPage("sector",Sector(1).some),
    contact = FormPage("contact", contactDetails),
    cya = FormPage("check-your-answers", true.some),
    confirmation = FormPage("confirmation", true.some)
  )

  val undertakingJourneyComplete1 =  UndertakingJourney(
    name = FormPage("undertaking-name", "TestUndertaking1".some),
    sector = FormPage("sector",Sector(2).some),
    contact = FormPage("contact", contactDetails1),
    cya = FormPage("check-your-answers", true.some),
    confirmation = FormPage("confirmation", true.some),
    isAmend = true.some
  )

  val businessEntityJourney = BusinessEntityJourney(
    addBusiness = FormPage("add-member", true.some),
    eori = FormPage("add-business-entity-eori", eori1.some),
    contact = FormPage("add-business-entity-contact", contactDetails),
    cya= FormPage("check-your-answers-businesses", true.some)
  )

  val businessEntityJourney1 = BusinessEntityJourney(
    addBusiness = FormPage("add-member", true.some),
    eori = FormPage("add-business-entity-eori", eori2.some),
    contact = FormPage("add-business-entity-contact", contactDetails),
    cya= FormPage("check-your-answers-businesses", true.some)
  )
  val businessEntityJourneyLead = BusinessEntityJourney(
    addBusiness = FormPage("add-member", true.some),
    eori = FormPage("add-business-entity-eori", eori2.some),
    contact = FormPage("add-business-entity-contact", contactDetails),
    cya= FormPage("check-your-answers-businesses", true.some),
    isLeadSelectJourney = true.some
  )

  val newLeadJourney = NewLeadJourney(selectNewLead = FormPage("select-new-lead", eori4.some))

  val validEmailAddress = EmailAddress("user@test.com")
  val inValidEmailAddress = EmailAddress("invalid@email.com")
  val undeliverableEmailAddress = EmailAddress("undeliverable@address.com")

  val dateTime = Some(LocalDateTime.of(2021, 1, 9, 10, 10))

  val undeliverableEmailResponse = EmailAddressResponse(undeliverableEmailAddress,
    dateTime,
    Some(Undeliverable("eventid1"))
  )
  val validEmailResponse = EmailAddressResponse(validEmailAddress, dateTime, None)
  val inValidEmailResponse = EmailAddressResponse(inValidEmailAddress, None, None)


  val undertakingCreated = Undertaking(None,
    UndertakingName("TestUndertaking"),
    transport,
    None, None,
    List(businessEntity5))

  val emailParameter   = SingleEORIEmailParameter(eori1, undertaking.name, undertakingRef, "undertaking Created by Lead EORI")
  val emailSendRequest = EmailSendRequest(List(EmailAddress("user@test.com")), "templateId1", emailParameter)

}
