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
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

class BusinessEntityJourneySpec extends BaseSpec with Matchers {

  "BusinessEntityJourney" should {

    "return an updated instance with the specified eori when setEori is called" in {
      BusinessEntityJourney().setEori(eori1) shouldBe BusinessEntityJourney(eori = AddEoriFormPage(eori1.some))
    }

    "return an updated instance with the specified boolean value when setAddBusiness is called" in {
      BusinessEntityJourney().setAddBusiness(true) shouldBe
        BusinessEntityJourney(addBusiness = AddBusinessFormPage(true.some))
    }
  }
}
