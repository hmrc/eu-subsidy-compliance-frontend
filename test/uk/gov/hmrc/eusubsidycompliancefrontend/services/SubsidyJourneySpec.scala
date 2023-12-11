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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import play.api.http.HeaderNames.REFERER
import play.api.http.Status.SEE_OTHER
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.SubsidyRef
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{DateFormValues, OptionalClaimEori, OptionalTraderRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._

class SubsidyJourneySpec extends BaseSpec with Matchers with ScalaFutures {

  private val amendJourney = SubsidyJourney(cya = CyaFormPage(Some(true))).setTraderRef(optionalTraderRef)

  "SubsidyJourney" should {

    "return an updated instance with the specified value" when {

      "setClaimAmount is called" in {
        val value = claimAmountPounds
        SubsidyJourney().setClaimAmount(value) shouldBe SubsidyJourney(claimAmount = ClaimAmountFormPage(value.some))
      }

      "setConvertedClaimAmount is called" in {
        val value = claimAmountPounds
        SubsidyJourney().setConvertedClaimAmount(value) shouldBe
          SubsidyJourney(convertedClaimAmountConfirmation = ConvertedClaimAmountConfirmationPage(value.some))
      }

      "setClaimDate is called" in {
        val value = DateFormValues("1", "2", "3")
        SubsidyJourney().setClaimDate(value) shouldBe SubsidyJourney(claimDate = ClaimDateFormPage(value.some))
      }

      "setClaimEori is called" in {
        val value = OptionalClaimEori("true", "121212121212".some)
        SubsidyJourney().setClaimEori(value) shouldBe SubsidyJourney(addClaimEori = AddClaimEoriFormPage(value.some))
      }

      "setPublicAuthority is called" in {
        val value = "Some Public Authority"
        SubsidyJourney().setPublicAuthority(value) shouldBe SubsidyJourney(publicAuthority =
          PublicAuthorityFormPage(value.some)
        )
      }

    }

    "return an updated instance with the specified value when setTraderRef is called" in {
      val value = OptionalTraderRef("true", "Some Trader Reference".some)
      SubsidyJourney().setTraderRef(value) shouldBe SubsidyJourney(traderRef = TraderRefFormPage(value.some))
    }

    "return an updated instance with the specified value when setCya is called" in {
      val value = true
      SubsidyJourney().setCya(value) shouldBe SubsidyJourney(cya = CyaFormPage(value.some))
    }

    "return an updated instance with the specified value when setAddBusiness is called" in {
      val value = true
      SubsidyJourney().setAddBusiness(value) shouldBe
        SubsidyJourney(addClaimBusiness = AddClaimBusinessFormPage(value.some))
    }

    "when next is called" should {

      "skip the confirm converted amount page if an amount in Euros is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountEuros.some)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)
      }

      "return the confirm converted amount page if an amount in Pounds sterling is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount.url)
      }

      "return a redirect to the add claim eori page if a GBP to EUR conversion has been confirmed" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount.url)
        val result = SubsidyJourney().setTraderRef(OptionalTraderRef("true", None)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimEori.url)
      }

      "return a redirect to the add claim business page if an eori has been entered that is not part of any undertaking" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url)
        val result = SubsidyJourney(
          existingTransactionId = SubsidyRef("SomeRef").some,
          claimAmount = ClaimAmountFormPage(claimAmountEuros.some),
          addClaimEori = AddClaimEoriFormPage(OptionalClaimEori("true", eori1.some, addToUndertaking = true).some)
        ).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimBusiness.url)
      }

      "return a redirect to the add public authority page if an eori has been entered that is part of the current undertaking" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url)
        val result = SubsidyJourney(
          existingTransactionId = SubsidyRef("SomeRef").some,
          claimAmount = ClaimAmountFormPage(claimAmountEuros.some),
          addClaimEori = AddClaimEoriFormPage(OptionalClaimEori("false", Option.empty).some)
        ).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimPublicAuthority.url)
      }

    }

    "when next is called on the amend journey" should {

      "return a redirect to the confirm claim amount page if a GBP amount is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)
        val result = amendJourney.next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount.url)
      }

      "return a redirect to the check your answers page if an EUR amount is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)
        val result = amendJourney.setClaimAmount(claimAmountEuros).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers.url)
      }

      "return a redirect to the confirm converted amount page if a GBP amount is entered" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)
        val result = amendJourney.setClaimAmount(claimAmountPounds).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getConfirmClaimAmount.url)
      }

      "return a redirect to the check your answers page if a GBP to EUR conversion has been confirmed" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getConfirmClaimAmount.url)
        val result = amendJourney.setTraderRef(OptionalTraderRef("true", None)).next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getCheckAnswers.url)
      }

      "return a redirect to the add claim business page if an eori has been entered that is not part of any undertaking" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url)
        val result = amendJourney
          .setClaimEori(optionalEORI.copy(addToUndertaking = true))
          .next
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.SubsidyController.getAddClaimBusiness.url)
      }

    }

    "when previous is called" should {

      "return URI to the reported non custom subsidy page from the claim date page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimDate.url)
        SubsidyJourney().previous shouldBe routes.SubsidyController.getReportedNoCustomSubsidyPage.url
      }

      "return URI to confirm converted amount page if current page is add claim EORI and the amount entered is in GBP" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountPounds.some)).previous
        result shouldBe routes.SubsidyController.getConfirmClaimAmount.url
      }

      "return URI to claimAmount page if current page is add claim EORI and the amount entered was in EUR" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimEori.url)
        val result = SubsidyJourney(claimAmount = ClaimAmountFormPage(claimAmountEuros.some)).previous
        result shouldBe routes.SubsidyController.getClaimAmount.url
      }

      "return URI to check your answers page if request for any page that is not the CYA page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getClaimAmount.url)

        val result = amendJourney
          .setTraderRef(OptionalTraderRef("true", None))
          .previous

        result shouldBe routes.SubsidyController.getCheckAnswers.url
      }

      "return to the previous page otherwise" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getAddClaimReference.url)
        SubsidyJourney().previous shouldBe routes.SubsidyController.getAddClaimPublicAuthority.url
      }

      "return URI to account home page if on first time user page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getReportPaymentFirstTimeUser.url)

        SubsidyJourney().previous shouldBe routes.AccountController.getAccountPage.url
      }
      "return URI to account home page if on returning user page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getReportedPaymentReturningUserPage.url)

        SubsidyJourney().previous shouldBe routes.AccountController.getAccountPage.url
      }

    }

    "when previous is called on the amend journey" should {

      "return URI to the claim reference page from the check your answers page" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, routes.SubsidyController.getCheckAnswers.url)
        amendJourney.previous shouldBe routes.SubsidyController.backFromCheckYourAnswers.url
      }

      List(
        routes.SubsidyController.getAddClaimReference.url,
        routes.SubsidyController.getAddClaimPublicAuthority.url,
        routes.SubsidyController.getAddClaimEori.url,
        routes.SubsidyController.getClaimAmount.url,
        routes.SubsidyController.getConfirmClaimAmount.url,
        routes.SubsidyController.getClaimDate.url
      ).foreach { page =>
        testPreviousWhenCyaAlreadyVisited(page)
      }
    }

  }

  private def testPreviousWhenCyaAlreadyVisited(uri: String): Unit = {
    s"return URI to check your answers for $uri" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, uri)
      amendJourney.previous shouldBe routes.SubsidyController.getCheckAnswers.url
    }
  }

}
