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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import cats.implicits.catsSyntaxOptionId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, OptionalEORI, OptionalTraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.claimAmount

class SubsidyJourneySpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "SubsidyJourney" should {

    "return an updated instance with the specified value when setReportPayment is called" in {
      val value = true
      SubsidyJourney().setReportPayment(value) shouldBe SubsidyJourney(reportPayment =
        ReportPaymentFormPage(value.some)
      )
    }

    "return an updated instance with the specified value when setClaimAmount is called" in {
      val value = claimAmount
      SubsidyJourney().setClaimAmount(value) shouldBe SubsidyJourney(claimAmount = ClaimAmountFormPage(value.some))
    }

    "return an updated instance with the specified value when setClaimDate is called" in {
      val value = DateFormValues("1", "2", "3")
      SubsidyJourney().setClaimDate(value) shouldBe SubsidyJourney(claimDate = ClaimDateFormPage(value.some))
    }

    "return an updated instance with the specified value when setClaimEori is called" in {
      val value = OptionalEORI("true", "121212121212".some)
      SubsidyJourney().setClaimEori(value) shouldBe SubsidyJourney(addClaimEori = AddClaimEoriFormPage(value.some))
    }

    "return an updated instance with the specified value when setPublicAuthority is called" in {
      val value = "Some Public Authority"
      SubsidyJourney().setPublicAuthority(value) shouldBe SubsidyJourney(publicAuthority =
        PublicAuthorityFormPage(value.some)
      )
    }

    "return an updated instance with the specified value when setTraderRef is called" in {
      val value = OptionalTraderRef("true", "Some Trader Reference".some)
      SubsidyJourney().setTraderRef(value) shouldBe SubsidyJourney(traderRef = TraderRefFormPage(value.some))
    }

    "return an updated instance with the specified value when setCya is called" in {
      val value = true
      SubsidyJourney().setCya(value) shouldBe SubsidyJourney(cya = CyaFormPage(value.some))
    }

    "when next is called" should {

      "return a redirect to the next page in the journey if not on amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/")
        val result = SubsidyJourney().next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getReportPayment().url)
      }

      "return a redirect to the check your answers page if on amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/")
        val result = SubsidyJourney(existingTransactionId = SubsidyRef("SomeRef").some).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers().url)
      }

      "skip the confirm converted amount page if an amount in Euros is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/")
        val result = SubsidyJourney().next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori().url)
      }

      "return the confirm converted amount page if an amount in Pounds sterling is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/")
        val result = SubsidyJourney().next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount().url)
      }

    }

    // TODO - are tests for previous needed too?

  }

}
