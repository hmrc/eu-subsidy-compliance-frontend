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

package uk.gov.hmrc.eusubsidycompliancefrontend.test

import cats.implicits.catsSyntaxOptionId
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{CreateUndertakingResponse, EISResponse, ResponseCommonUndertaking, ResponseDetail}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailParameters, EmailSendRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.{agriculture, transport}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingStatus._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{DeclarationID, EORI, EisSubsidyAmendmentType, IndustrySectorLimit, Sector, SubsidyAmount, SubsidyRef, TaxType, TraderRef, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps

import java.time.{LocalDate, LocalDateTime}

object CommonTestData {

  val fixedDate = LocalDate.of(2021, 1, 20)

  val eori1 = EORI("GB123456789012")
  val eori2 = EORI("GB123456789013")
  val eori3 = EORI("GB123456789014")
  val eori4 = EORI("GB123456789010")

  val businessEntity1 = BusinessEntity(EORI(eori1), leadEORI = true)
  val businessEntity2 = BusinessEntity(EORI(eori2), leadEORI = true)
  val businessEntity3 = BusinessEntity(EORI(eori3), leadEORI = true)
  val businessEntity4 = BusinessEntity(EORI(eori4), leadEORI = false)
  val businessEntity5 = BusinessEntity(EORI(eori1), leadEORI = true)

  val optionalTraderRef = OptionalTraderRef("true", TraderRef("ABC123").some)
  val optionalEORI = OptionalClaimEori("true", eori1.some)

  val undertakingRef = UndertakingRef("UR123456")

  val subsidyAmount = SubsidyAmount(BigDecimal(123.45))
  val nonHmrcSubsidyAmount = SubsidyAmount(BigDecimal(543.21))

  val declarationId = DeclarationID("12345")

  val traderRef = TraderRef("SomeTraderReference")

  val hmrcSubsidy = HmrcSubsidy(
    declarationID = declarationId,
    issueDate = Some(fixedDate),
    acceptanceDate = fixedDate,
    declarantEORI = eori1,
    consigneeEORI = eori3,
    taxType = Some(TaxType("1")),
    hmrcSubsidyAmtGBP = Some(subsidyAmount),
    hmrcSubsidyAmtEUR = Some(subsidyAmount),
    tradersOwnRefUCR = Some(traderRef)
  )

  val nonHmrcSubsidy = NonHmrcSubsidy(
    subsidyUsageTransactionId = Some(SubsidyRef("AB12345")),
    allocationDate = LocalDate.of(2022, 1, 1),
    submissionDate = fixedDate,
    publicAuthority = "Local Authority".some,
    traderReference = TraderRef("ABC123").some,
    nonHMRCSubsidyAmtEUR = nonHmrcSubsidyAmount,
    businessEntityIdentifier = eori1.some,
    amendmentType = EisSubsidyAmendmentType("1").some
  )

  val nonHmrcSubsidyList = List(nonHmrcSubsidy)

  val nonHmrcSubsidyList1 = nonHmrcSubsidyList.map(_.copy(subsidyUsageTransactionId = SubsidyRef("TID1234").some))

  val undertakingSubsidies = UndertakingSubsidies(
    undertakingIdentifier = undertakingRef,
    nonHMRCSubsidyTotalEUR = nonHmrcSubsidyAmount,
    nonHMRCSubsidyTotalGBP = nonHmrcSubsidyAmount,
    hmrcSubsidyTotalEUR = subsidyAmount,
    hmrcSubsidyTotalGBP = subsidyAmount,
    nonHMRCSubsidyUsage = List(nonHmrcSubsidy),
    hmrcSubsidyUsage = List(hmrcSubsidy)
  )

  val emptyUndertakingSubsidies = undertakingSubsidies.copy(
    nonHMRCSubsidyTotalEUR = SubsidyAmount(0),
    nonHMRCSubsidyTotalGBP = SubsidyAmount(0),
    hmrcSubsidyTotalEUR = SubsidyAmount(0),
    hmrcSubsidyTotalGBP = SubsidyAmount(0),
    nonHMRCSubsidyUsage = List.empty,
    hmrcSubsidyUsage = List.empty
  )

  val claimAmountPounds = ClaimAmount(GBP, subsidyAmount.toString())
  val claimAmountEuros = ClaimAmount(EUR, subsidyAmount.toString())

  val claimDate = LocalDate.of(2022, 1, 1)
  val claimDate2 = LocalDate.of(2022, 10, 9)

  val subsidyJourney = SubsidyJourney(
    publicAuthority = PublicAuthorityFormPage("Local Authority".some),
    traderRef = TraderRefFormPage(optionalTraderRef.some),
    claimAmount = ClaimAmountFormPage(claimAmountEuros.some),
    addClaimEori = AddClaimEoriFormPage(optionalEORI.some),
    claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
  )

  val subsidyUpdate = SubsidyUpdate(
    undertakingRef,
    UndertakingSubsidyAmendment(nonHmrcSubsidyList)
  )

  val undertaking = Undertaking(
    undertakingRef,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34),
    LocalDate.of(2021, 1, 18).some,
    Some(active),
    List(businessEntity1, businessEntity2)
  )

  val writeableUndertaking = UndertakingCreate(
    UndertakingName("TestUndertaking"),
    transport,
    List(businessEntity1)
  )

  val undertaking1 = Undertaking(
    undertakingRef,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34),
    LocalDate.of(2021, 1, 18).some,
    Some(active),
    List(businessEntity1, businessEntity4)
  )

  val undertaking2 = Undertaking(
    undertakingRef,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34),
    LocalDate.of(2021, 1, 18).some,
    Some(active),
    List(businessEntity4)
  )

  val undertaking3 = Undertaking(
    undertakingRef,
    UndertakingName("TestUndertaking"),
    agriculture,
    IndustrySectorLimit(32.34),
    Some(LocalDate.of(2021, 1, 18)),
    Some(active),
    List(businessEntity1, businessEntity2)
  )

  val subsidyRetrieve = SubsidyRetrieve(
    undertakingRef,
    None
  )

  def subsidyRetrieveForDate(d: LocalDate) =
    subsidyRetrieve.copy(
      inDateRange = Some(
        (
          d.toEarliestTaxYearStart,
          d
        )
      )
    )

  val subsidyRetrieveForFixedDate = subsidyRetrieve.copy(
    inDateRange = Some((LocalDate.of(2018, 4, 6), LocalDate.of(2021, 1, 20)))
  )

  val undertakingSubsidies1 = undertakingSubsidies.copy(nonHMRCSubsidyUsage = nonHmrcSubsidyList1)

  val eligibilityJourneyNotComplete = EligibilityJourney(
    doYouClaim = DoYouClaimFormPage(true.some),
    willYouClaim = WillYouClaimFormPage(true.some)
  )

  val eligibilityJourneyComplete = eligibilityJourneyNotComplete.copy(
    doYouClaim = DoYouClaimFormPage(true.some),
    eoriCheck = EoriCheckFormPage(true.some)
  )

  val undertakingJourneyComplete = UndertakingJourney(
    about = AboutUndertakingFormPage("TestUndertaking".some),
    sector = UndertakingSectorFormPage(Sector(1).some),
    hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
    addBusiness = UndertakingAddBusinessFormPage(false.some),
    cya = UndertakingCyaFormPage(true.some),
    confirmation = UndertakingConfirmationFormPage(true.some)
  )

  val undertakingJourneyComplete1 = UndertakingJourney(
    about = AboutUndertakingFormPage("TestUndertaking1".some),
    sector = UndertakingSectorFormPage(Sector(2).some),
    cya = UndertakingCyaFormPage(true.some),
    confirmation = UndertakingConfirmationFormPage(true.some),
    isAmend = true
  )

  val businessEntityJourney = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori1.some)
  )

  val businessEntityJourney1 = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori2.some)
  )
  val businessEntityJourneyLead = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori2.some),
    isLeadSelectJourney = true.some
  )

  val newBecomeLeadJourney = BecomeLeadJourney()

  val validEmailAddress = EmailAddress("user@test.com")
  val inValidEmailAddress = EmailAddress("invalid@email.com")
  val undeliverableEmailAddress = EmailAddress("undeliverable@address.com")

  val dateTime = LocalDateTime.of(2021, 1, 9, 10, 10)

  val undeliverableEmailResponse =
    EmailAddressResponse(undeliverableEmailAddress, dateTime.some, Some(Undeliverable("eventid1")))
  val validEmailResponse = EmailAddressResponse(validEmailAddress, dateTime.some, None)
  val unverifiedEmailResponse = EmailAddressResponse(validEmailAddress, None, None)
  val inValidEmailResponse = EmailAddressResponse(inValidEmailAddress, None, Some(Undeliverable("foo")))

  val undertakingCreated =
    UndertakingCreate(UndertakingName("TestUndertaking"), transport, List(businessEntity5))

  val singleEoriEmailParameters = EmailParameters(eori1, None, undertaking.name, None)
  val singleEoriWithDateEmailParameters = EmailParameters(eori1, None, undertaking.name, dateTime.toString.some)
  val doubleEoriEmailParameters = EmailParameters(eori1, eori2.some, undertaking.name, None)
  val doubleEoriWithDateEmailParameters = EmailParameters(eori1, eori2.some, undertaking.name, dateTime.toString.some)

  val emailSendRequest = EmailSendRequest(List(EmailAddress("user@test.com")), "templateId1", singleEoriEmailParameters)

  val eligibilityJourney = EligibilityJourney(
    doYouClaim = DoYouClaimFormPage(true.some),
    willYouClaim = WillYouClaimFormPage(true.some),
    notEligible = NotEligibleFormPage(true.some),
    eoriCheck = EoriCheckFormPage(true.some),
    signOutBadEori = SignOutBadEoriFormPage(true.some)
  )

  val timeNow = LocalDateTime.of(2021, 10, 9, 10, 9, 0, 0)

  def createUndertakingAuditEvent(sectorLimit: IndustrySectorLimit) = AuditEvent.CreateUndertaking(
    "1123",
    UndertakingCreateWithSectorLimit(undertakingCreated, sectorLimit),
    EISResponse(
      CreateUndertakingResponse(
        ResponseCommonUndertaking("OK", timeNow),
        ResponseDetail(undertakingRef)
      )
    )
  )

  val businessEntityAddedEvent = AuditEvent.BusinessEntityAdded(undertakingRef, "1123", eori1, eori2)
  val businessEntityUpdatedEvent = AuditEvent.BusinessEntityUpdated(undertakingRef, "1123", eori1, eori2)

  val exchangeRate = MonthlyExchangeRate("EUR", "GBP", BigDecimal(0.891), "01/01/2023", "31/01/2023")

  val emailVerificationRequest = EmailVerificationRequest(
    "credId",
    "continueUrl",
    "origin",
    None,
    "accessibilityStatement",
    None,
    None,
    None,
    None
  )

  val industrySectorLimit = IndustrySectorLimit(20000)

  val undertakingBalance = UndertakingBalance(
    undertakingIdentifier = UndertakingRef("some-ref"),
    nonHMRCSubsidyAllocationEUR = None,
    hmrcSubsidyAllocationEUR = None,
    industrySectorLimit = industrySectorLimit,
    availableBalanceEUR = subsidyAmount,
    availableBalanceGBP = subsidyAmount,
    conversionRate = SubsidyAmount(1.2),
    nationalCapBalanceEUR = industrySectorLimit
  )

  val subsidyJourneyWithCyaVisited = subsidyJourney.copy(
    cya = CyaFormPage(Some(true))
  )

  val submittedSubsidyJourney = subsidyJourney.copy(
    cya = CyaFormPage(Some(true)),
    submitted = Some(true)
  )

}
