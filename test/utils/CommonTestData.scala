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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types._
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.createUndertaking.{CreateUndertakingResponse, EISResponse, ResponseCommonUndertaking, ResponseDetail}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailParameters.SingleEORIEmailParameter
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BusinessEntityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EligibilityJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.NewLeadJourney.Forms.SelectNewLeadFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.UndertakingJourney.Forms.{UndertakingConfirmationFormPage, UndertakingCyaFormPage, UndertakingNameFormPage, UndertakingSectorFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._

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
  val optionalEORI = OptionalEORI("true", eori1.some)

  val undertakingRef = UndertakingRef("UR123456")
  val nonHmrcSubsidyList = List(
    NonHmrcSubsidy(
      subsidyUsageTransactionID = None,
      allocationDate = LocalDate.of(2022, 1, 1),
      submissionDate = fixedDate,
      publicAuthority = "Local Authority".some,
      traderReference = TraderRef("ABC123").some,
      nonHMRCSubsidyAmtEUR = SubsidyAmount(1234.56),
      businessEntityIdentifier = eori1.some,
      amendmentType = EisSubsidyAmendmentType("1").some
    )
  )

  val subsidyJourney = SubsidyJourney(
    publicAuthority = PublicAuthorityFormPage("Local Authority".some),
    traderRef = TraderRefFormPage(optionalTraderRef.some),
    claimAmount = ClaimAmountFormPage(SubsidyAmount(1234.56).some),
    addClaimEori = AddClaimEoriFormPage(optionalEORI.some),
    claimDate = ClaimDateFormPage(DateFormValues("1", "1", "2022").some)
  )

  val subsidyUpdate = SubsidyUpdate(
    undertakingRef,
    UndertakingSubsidyAmendment(nonHmrcSubsidyList)
  )

  val undertaking = Undertaking(
    undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021, 1, 18).some,
    List(businessEntity1, businessEntity2)
  )

  val undertaking1 = Undertaking(
    undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021, 1, 18).some,
    List(businessEntity1, businessEntity4)
  )

  val undertaking2 = Undertaking(
    undertakingRef.some,
    UndertakingName("TestUndertaking"),
    transport,
    IndustrySectorLimit(12.34).some,
    LocalDate.of(2021, 1, 18).some,
    List(businessEntity4)
  )

  val subsidyRetrieve = SubsidyRetrieve(
    undertakingRef,
    None
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
    customsWaivers = CustomsWaiversFormPage(true.some),
    willYouClaim = WillYouClaimFormPage(true.some),
    notEligible = NotEligibleFormPage(false.some),
    mainBusinessCheck = MainBusinessCheckFormPage(true.some),
    signOut = SignOutFormPage(false.some),
    acceptTerms = AcceptTermsFormPage(true.some)
  )

  val eligibilityJourneyComplete = eligibilityJourneyNotComplete.copy(
    eoriCheck = EoriCheckFormPage(true.some),
    signOutBadEori = SignOutBadEoriFormPage(false.some),
    createUndertaking = CreateUndertakingFormPage(true.some)
  )

  val undertakingJourneyComplete = UndertakingJourney(
    name = UndertakingNameFormPage("TestUndertaking".some),
    sector = UndertakingSectorFormPage(Sector(1).some),
    cya = UndertakingCyaFormPage(true.some),
    confirmation = UndertakingConfirmationFormPage(true.some)
  )

  val undertakingJourneyComplete1 = UndertakingJourney(
    name = UndertakingNameFormPage("TestUndertaking1".some),
    sector = UndertakingSectorFormPage(Sector(2).some),
    cya = UndertakingCyaFormPage(true.some),
    confirmation = UndertakingConfirmationFormPage(true.some),
    isAmend = true
  )

  val businessEntityJourney = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori1.some),
    cya = AddBusinessCyaFormPage(true.some)
  )

  val businessEntityJourney1 = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori2.some),
    cya = AddBusinessCyaFormPage(true.some)
  )
  val businessEntityJourneyLead = BusinessEntityJourney(
    addBusiness = AddBusinessFormPage(true.some),
    eori = AddEoriFormPage(eori2.some),
    cya = AddBusinessCyaFormPage(true.some),
    isLeadSelectJourney = true.some
  )

  val newLeadJourney = NewLeadJourney(selectNewLead = SelectNewLeadFormPage(eori4.some))

  val newBecomeLeadJourney = BecomeLeadJourney()

  val validEmailAddress = EmailAddress("user@test.com")
  val inValidEmailAddress = EmailAddress("invalid@email.com")
  val undeliverableEmailAddress = EmailAddress("undeliverable@address.com")

  val dateTime = Some(LocalDateTime.of(2021, 1, 9, 10, 10))

  val undeliverableEmailResponse =
    EmailAddressResponse(undeliverableEmailAddress, dateTime, Some(Undeliverable("eventid1")))
  val validEmailResponse = EmailAddressResponse(validEmailAddress, dateTime, None)
  val inValidEmailResponse = EmailAddressResponse(inValidEmailAddress, None, None)

  val undertakingCreated =
    Undertaking(None, UndertakingName("TestUndertaking"), transport, None, None, List(businessEntity5))

  val emailParameter = SingleEORIEmailParameter(eori1, undertaking.name, undertakingRef, "createUndertaking")
  val emailSendRequest = EmailSendRequest(List(EmailAddress("user@test.com")), "templateId1", emailParameter)

  val eligibilityJourney = EligibilityJourney(
    customsWaivers = CustomsWaiversFormPage(true.some),
    willYouClaim = WillYouClaimFormPage(true.some),
    notEligible = NotEligibleFormPage(true.some),
    mainBusinessCheck = MainBusinessCheckFormPage(true.some),
    signOut = SignOutFormPage(true.some),
    acceptTerms = AcceptTermsFormPage(true.some),
    eoriCheck = EoriCheckFormPage(true.some),
    signOutBadEori = SignOutBadEoriFormPage(true.some),
    createUndertaking = CreateUndertakingFormPage(true.some)
  )

  val timeNow = LocalDateTime.of(2021, 10, 9, 10, 9, 0, 0)

  val createUndertakingAuditEvent = AuditEvent.CreateUndertaking(
    "1123",
    undertakingCreated,
    EISResponse(
      CreateUndertakingResponse(
        ResponseCommonUndertaking("OK", timeNow),
        ResponseDetail(undertakingRef)
      )
    )
  )
}
