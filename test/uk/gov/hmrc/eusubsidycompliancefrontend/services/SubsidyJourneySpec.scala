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
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{claimAmountEuros, claimAmountPounds}

class SubsidyJourneySpec extends AnyWordSpecLike with Matchers with ScalaFutures {

  "SubsidyJourney" should {

    "return an updated instance with the specified value when setClaimAmount is called" in {
      val value = claimAmountPounds
      SubsidyJourney().setClaimAmount(value) shouldBe SubsidyJourney(claimAmount = ClaimAmountFormPage(value.some))
    }

    "return an updated instance with the specified value when setConvertedClaimAmount is called" in {
      val value = claimAmountPounds
      SubsidyJourney().setConvertedClaimAmount(value) shouldBe
        SubsidyJourney(convertedClaimAmountConfirmation = ConvertedClaimAmountConfirmationPage(value.some))
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
        redirectLocation(result) should contain(routes.SubsidyController.getClaimDate().url)
      }

      "skip the confirm converted amount page if an amount in Euros is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount().url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountEuros.some)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori().url)
      }

      "return the confirm converted amount page if an amount in Pounds sterling is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount().url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount().url)
      }

      "return a redirect to the confirm claim amount page if a GBP amount is entered on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.SubsidyController.getClaimAmount().url)
        val result = SubsidyJourney(
          existingTransactionId = SubsidyRef("SomeRef").some,
          claimAmount = ClaimAmountFormPage(claimAmountPounds.some),
        ).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount().url)
      }

      "return a redirect to the check your answers page if a GBP to EUR conversion has been confirmed" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount().url)
        val result = SubsidyJourney(existingTransactionId = SubsidyRef("SomeRef").some).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers().url)
      }

      "return a redirect to the check your answers page if an EUR amount is entered on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.SubsidyController.getClaimAmount().url)
        val result = SubsidyJourney(
          existingTransactionId = SubsidyRef("SomeRef").some,
          claimAmount = ClaimAmountFormPage(claimAmountEuros.some),
        ).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers().url)
      }

    }

    "when previous is called" should {

      "return URI to the account home page if current page is claim date" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimDate().url)
        SubsidyJourney().previous shouldBe routes.AccountController.getAccountPage().url
      }

      "return URI to confirm converted amount page if current page is add claim EORI and the amount entered is in GBP" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori().url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).previous
        result shouldBe routes.SubsidyController.getConfirmClaimAmount().url
      }

      "return URI to claimAmount page if current page is add claim EORI and the amount entered was in EUR" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori().url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountEuros.some)).previous
        result shouldBe routes.SubsidyController.getClaimAmount().url
      }

      "return URI to claimAmount page if current page is confirm converted amount on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount().url)
        val result = SubsidyJourney(
          claimAmount = ClaimAmountFormPage(claimAmountPounds.some),
          existingTransactionId = SubsidyRef("Foo").some
        ).previous
        result shouldBe routes.SubsidyController.getClaimAmount().url
      }

      "return URI to check your answers page if request for any page that is not the CYA page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimDate().url)

        val result = SubsidyJourney(existingTransactionId = SubsidyRef("foo").some).previous
        result shouldBe routes.SubsidyController.getCheckAnswers().url
      }

      "return URI to the referring page if it is valid and matches a SubsidyJourney page on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getCheckAnswers().url)
            .withHeaders("Referer" -> routes.SubsidyController.getClaimDate().url)

        val result = SubsidyJourney(existingTransactionId = SubsidyRef("foo").some).previous
        result shouldBe routes.SubsidyController.getClaimDate().url
      }

      "return URI to claim date page if referer URL not in SubsidyJourney on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getCheckAnswers().url)
            .withHeaders("Referer" -> routes.AccountController.getAccountPage().url)

        val result = SubsidyJourney(existingTransactionId = SubsidyRef("foo").some).previous
        result shouldBe routes.SubsidyController.getClaimDate().url
      }

      "return URI to claim date page if referer URL could not be parsed on the amend journey" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getCheckAnswers().url)
            .withHeaders("Referer" -> "this is not a valid url")

        val result = SubsidyJourney(existingTransactionId = SubsidyRef("foo").some).previous
        result shouldBe routes.SubsidyController.getClaimDate().url
      }

      "return to the previous page otherwise" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimReference().url)
        SubsidyJourney().previous shouldBe routes.SubsidyController.getAddClaimPublicAuthority().url
      }

    }

  }

}
