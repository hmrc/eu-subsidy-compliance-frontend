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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.catsSyntaxOptionId
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney.FormPages.{AddBusinessFormPage, AddEoriFormPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, eori2}

class BusinessEntityJourneySpec extends BaseSpec with Matchers {

  "BusinessEntityJourney" should {

    "return an updated instance with the specified eori when setEori is called" in {
      BusinessEntityJourney().setEori(eori1) shouldBe BusinessEntityJourney(eori = AddEoriFormPage(eori1.some))
    }

    "return an updated instance with the specified boolean value when setAddBusiness is called" in {
      BusinessEntityJourney().setAddBusiness(true) shouldBe
        BusinessEntityJourney(addBusiness = AddBusinessFormPage(true.some))
    }

    "return correct steps array" in {
      val journey = BusinessEntityJourney()
      journey.steps should have length 2
      journey.steps(0) shouldBe journey.addBusiness
      journey.steps(1) shouldBe journey.eori
    }

    "return false for isAmend when oldEORI is None" in {
      BusinessEntityJourney().isAmend shouldBe false
    }

    "return true for isAmend when oldEORI is defined" in {
      BusinessEntityJourney(oldEORI = eori1.some).isAmend shouldBe true
    }

    "return false for onLeadSelectJourney when isLeadSelectJourney is None" in {
      BusinessEntityJourney().onLeadSelectJourney shouldBe false
    }

    "return false for onLeadSelectJourney when isLeadSelectJourney is Some(false)" in {
      BusinessEntityJourney(isLeadSelectJourney = Some(false)).onLeadSelectJourney shouldBe false
    }

    "return true for onLeadSelectJourney when isLeadSelectJourney is Some(true)" in {
      BusinessEntityJourney(isLeadSelectJourney = Some(true)).onLeadSelectJourney shouldBe true
    }

    "store oldEORI when setEori is called on a journey with existing eori" in {
      val journey = BusinessEntityJourney(eori = AddEoriFormPage(eori1.some))
      val updated = journey.setEori(eori2)
      updated.eori.value shouldBe eori2.some
      updated.oldEORI shouldBe eori1.some
    }
    "return a uri for AddBusinessFormPage" in {
      AddBusinessFormPage().uri should not be empty
    }

    "return a uri for AddEoriFormPage" in {
      AddEoriFormPage().uri should not be empty
    }
  }
}
