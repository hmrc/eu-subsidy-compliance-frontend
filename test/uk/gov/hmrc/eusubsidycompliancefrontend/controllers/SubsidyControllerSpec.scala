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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.implicits.catsSyntaxOptionId
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.SubsidyControllerSpec.RemoveSubsidyRow
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{NonHmrcSubsidy, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyRemoved
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, SubsidyRef, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.SubsidyJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, AuditServiceSupport, EscService, JourneyTraverseService, Store, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData.{undertakingRef, _}

import java.time.LocalDate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax._

import scala.collection.JavaConverters.asScalaIteratorConverter

class SubsidyControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with JourneySupport
    with AuditServiceSupport {

  private val mockEscService = mock[EscService]
  private val mockTimeProvider = mock[TimeProvider]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider)
  )

  private def mockRetrieveSubsidy(subsidyRetrieve: SubsidyRetrieve)(result: Future[UndertakingSubsidies]) =
    (mockEscService
      .retrieveSubsidy(_: SubsidyRetrieve)(_: HeaderCarrier))
      .expects(subsidyRetrieve, *)
      .returning(result)

  private def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  private def mockRemoveSubsidy(reference: UndertakingRef, nonHmrcSubsidy: NonHmrcSubsidy)(
    result: Either[Error, UndertakingRef]
  ) =
    (mockEscService
      .removeSubsidy(_: UndertakingRef, _: NonHmrcSubsidy)(_: HeaderCarrier))
      .expects(reference, nonHmrcSubsidy, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)), Future.successful(_)))

  private def mockCreateSubsidy(reference: UndertakingRef, subsidyUpdate: SubsidyUpdate)(
    result: Either[Error, UndertakingRef]
  ) =
    (mockEscService
      .createSubsidy(_: UndertakingRef, _: SubsidyUpdate)(_: HeaderCarrier))
      .expects(reference, subsidyUpdate, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)), Future.successful(_)))

  private def mockTimeProviderToday(today: LocalDate) =
    (mockTimeProvider.today _).expects().returning(today)

  private val controller = instanceOf[SubsidyController]
  private val exception = new Exception("oh no!")
  private val currentDate = LocalDate.of(2022, 10, 9)

  "SubsidyControllerSpec" when {

    "handling request to get report payment page" must {

      def performAction() = controller.getReportPayment(FakeRequest())

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockGet[Undertaking](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(nonHMRCSubsidyUsage: List[NonHmrcSubsidy]) =
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
            mockGet[Undertaking](eori1)(Right(undertaking.some))
            mockTimeProviderToday(currentDate)
            mockRetrieveSubsidy(subsidyRetrieve)(
              Future(undertakingSubsidies.copy(nonHMRCSubsidyUsage = nonHMRCSubsidyUsage))
            )

          }

        "user hasn't already answered the question" in {
          test(List.empty)
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("report-payment.title"),
            { doc =>
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postReportPayment().url
              doc.select("#subsidy-list").size() shouldBe 0
            }
          )
        }

        "user has already answered the question" in {
          test(nonHmrcSubsidyList.map(_.copy(subsidyUsageTransactionId = SubsidyRef("Z12345").some)))
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("report-payment.title"),
            { doc =>
              val subsidyList = doc.select("#subsidy-list")

              subsidyList.select("thead > tr > th:nth-child(1)").text() shouldBe "Date"
              subsidyList.select("thead > tr > th:nth-child(2)").text() shouldBe "Amount"
              subsidyList.select("thead > tr > th:nth-child(3)").text() shouldBe "EORI number"
              subsidyList.select("thead > tr > th:nth-child(4)").text() shouldBe "Public authority"
              subsidyList.select("thead > tr > th:nth-child(5)").text() shouldBe "Your reference"

              subsidyList.select("tbody > tr > td:nth-child(1)").text() shouldBe "1 Jan 2022"
              subsidyList.select("tbody > tr > td:nth-child(2)").text() shouldBe "€1,234.56"
              subsidyList.select("tbody > tr > td:nth-child(3)").text() shouldBe "GB123456789012"
              subsidyList.select("tbody > tr > td:nth-child(4)").text() shouldBe "Local Authority"
              subsidyList.select("tbody > tr > td:nth-child(5)").text() shouldBe "ABC123"
              subsidyList.select("tbody > tr > td:nth-child(6)").text() shouldBe "Change"
              subsidyList.select("tbody > tr > td:nth-child(7)").text() shouldBe "Remove"

              subsidyList.select("tbody > tr > td:nth-child(6) > a").attr("href") shouldBe routes.SubsidyController
                .getChangeSubsidyClaim("Z12345")
                .url
              subsidyList.select("tbody > tr > td:nth-child(7) > a").attr("href") shouldBe routes.SubsidyController
                .getRemoveSubsidyClaim("Z12345")
                .url

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postReportPayment().url
            }
          )
        }
      }

    }

    "handling request to post report payment" must {

      def performAction(data: (String, String)*) = controller.postReportPayment(
        FakeRequest("POST", routes.SubsidyController.postReportPayment().url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        "user selected Yes" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](identity, eori1)(Right(subsidyJourney))
          }
          checkIsRedirect(performAction(("reportPayment", "true")), routes.SubsidyController.getClaimDate().url)
        }
      }
    }

    "handling request to get claim date page" must {

      def performAction() = controller.getClaimDate(FakeRequest())

      "throw technical error" when {

        val exception = new Exception("oh no")

        "call to get session fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))

        }

        "call to get sessions returns none" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))

        }

      }

      "display the page" when {

        "happy path" in {
          inAnyOrder {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
            mockGetPrevious[SubsidyJourney](eori1)(Right("previous"))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-date.title"),
            { doc =>
              doc.select("#claim-date > div:nth-child(1) > div > label").text() shouldBe "Day"
              doc.select("#claim-date > div:nth-child(2) > div > label").text() shouldBe "Month"
              doc.select("#claim-date > div:nth-child(3) > div > label").text() shouldBe "Year"
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postClaimDate().url
            }
          )
        }

      }
    }

    "handling request to post claim date" must {

      def performAction(data: (String, String)*) = controller.postClaimDate(
        FakeRequest("POST", routes.SubsidyController.postClaimDate().url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        "valid input" in {
          val updatedDate = DateFormValues("1", "2", LocalDate.now().getYear.toString)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
            mockUpdate[SubsidyJourney](j => j.map(_.copy(claimDate = ClaimDateFormPage(updatedDate.some))), eori1)(
              Right(subsidyJourney.copy(claimDate = ClaimDateFormPage(updatedDate.some)))
            )
          }
          checkIsRedirect(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year),
            routes.SubsidyController.getClaimAmount().url
          )
        }
      }

      "invalid input" should {
        "invalid date" in {
          val updatedDate = DateFormValues("20", "20", LocalDate.now().getYear.toString)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
          }
          status(
            performAction("day" -> updatedDate.day, "month" -> updatedDate.month, "year" -> updatedDate.year)
          ) shouldBe BAD_REQUEST
        }
      }
    }

    "handling request to get claim amount" must {

      def performAction() = controller
        .getClaimAmount(FakeRequest("GET", routes.SubsidyController.getClaimAmount().url))

      "throw technical error" when {

        val exception = new Exception("oh no !")
        "call to get subsidy journey fails " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but return None " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          mockAuthWithNecessaryEnrolment()
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-amount.title"),
            { doc =>
              val text = doc.select(".govuk-list").text
              text should include regex subsidyJourney.claimDate.value.map(_.year).getOrElse("")
              text should include regex subsidyJourney.claimDate.value.map(_.month).getOrElse("")

              val input = doc.select(".govuk-input").attr("value")
              input shouldBe subsidyJourney.claimAmount.value.map(_.toString()).getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimAmount().url

            }
          )
        }

        "user hasn't already answered the question" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
            )
          )
        }

        "user has already answered the question" in {
          test(
            SubsidyJourney(
              reportPayment = ReportPaymentFormPage(true.some),
              claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
              claimAmount = ClaimAmountFormPage(BigDecimal(123.45).some)
            )
          )
        }

      }

    }

    "handling request to Post claim amount" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimAmount(
          FakeRequest("POST", routes.SubsidyController.getClaimAmount().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no !")
        "call to get subsidy journey fails " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back empty " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey passes but come back with No claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get subsidy journey come back with no claim date " in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(SubsidyJourney().some))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the subsidy journey fails" in {

          val subsidyJourneyOpt = SubsidyJourney(
            reportPayment = ReportPaymentFormPage(true.some),
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          ).some

          def update(subsidyJourneyOpt: Option[SubsidyJourney]) =
            subsidyJourneyOpt.map(_.copy(claimAmount = ClaimAmountFormPage(BigDecimal(123.45).some)))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourneyOpt), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("claim-amount" -> "123.45")))
        }
      }

      "display form error" when {

        def displayError(data: (String, String)*)(errorMessageKey: String): Unit = {

          val subsidyJourneyOpt = SubsidyJourney(
            reportPayment = ReportPaymentFormPage(true.some),
            claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
          ).some
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("add-claim-amount.title"),
            messageFromMessageKey(errorMessageKey)
          )
        }

        "nothing is entered" in {
          displayError("claim-amount" -> "")("add-claim-amount.error.real")

        }
        "claim amount entered in wrong format" in {
          displayError("claim-amount" -> "123.4")("add-claim-amount.error.amount.incorrectFormat")
        }

        "claim amount entered is more than 17 chars" in {
          displayError("claim-amount" -> "1234567890.12345678")("add-claim-amount.error.amount.tooBig")
        }

        "claim amount entered is too small < 0.01" in {
          displayError("claim-amount" -> "00.01")("add-claim-amount.error.amount.tooSmall")
        }

      }

      "redirect to next page" in {

        val subsidyJourney = SubsidyJourney(
          reportPayment = ReportPaymentFormPage(true.some),
          claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some),
          claimAmount = ClaimAmountFormPage(BigDecimal(123.45).some)
        )

        val subsidyJourneyOpt = SubsidyJourney(
          reportPayment = ReportPaymentFormPage(true.some),
          claimDate = ClaimDateFormPage(DateFormValues("9", "10", "2022").some)
        ).some

        def update(subsidyJourneyOpt: Option[SubsidyJourney]) =
          subsidyJourneyOpt.map(_.copy(claimAmount = ClaimAmountFormPage(BigDecimal(123.45).some)))
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[SubsidyJourney](eori1)(Right(subsidyJourneyOpt))
          mockUpdate[SubsidyJourney](_ => update(subsidyJourneyOpt), eori1)(Right(subsidyJourney))
        }

        checkIsRedirect(
          performAction("claim-amount" -> "123.45"),
          routes.SubsidyController.getAddClaimEori().url
        )

      }

    }

    "handling request to get Add Claim Eori " must {

      def performAction() = controller
        .getAddClaimEori(FakeRequest("GET", routes.SubsidyController.getAddClaimEori().url))

      "throw technical error" when {
        val exception = new Exception("oh no")
        " the call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        " the call to get subsidy journey comes back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
            mockGetPrevious[SubsidyJourney](eori1)(Right("next"))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-eori.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.addClaimEori.value match {
                case Some(OptionalEORI(input, eori)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe eori.map(_.drop(2)).getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimEori().url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(None)))
        }

        "the user has already answered the question" in {
          List(
            subsidyJourney,
            subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(OptionalEORI("false", None).some))
          )
            .foreach { subsidyJourney =>
              withClue(s" for each subsidy journey $subsidyJourney") {
                test(subsidyJourney)
              }
            }
        }

      }
    }

    "handling the request to post add claim eori" must {

      def performAction(data: (String, String)*) = controller
        .postAddClaimEori(
          FakeRequest("POST", routes.SubsidyController.getAddClaimEori().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update subsidy journey fails" in {

          def update(subsidyJourneyOpt: Option[SubsidyJourney]) =
            subsidyJourneyOpt.map(_.copy(addClaimEori = AddClaimEoriFormPage(OptionalEORI("false", None).some)))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-amount"))
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourney.copy(addClaimEori = AddClaimEoriFormPage(None)).some),
              eori1
            )(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("should-claim-eori" -> "false")))
        }
      }

      "show form error" when {

        def testFormError(
          inputAnswer: Option[List[(String, String)]],
          errorMessageKey: String
        ): Unit = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-amount"))
          }
          checkFormErrorIsDisplayed(
            performAction(answers: _*),
            messageFromMessageKey("add-claim-eori.title"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "add-claim-eori.error.required")
        }

        "yes is selected but no eori is entered" in {
          testFormError(Some(List("should-claim-eori" -> "true")), "add-claim-eori.error.format")

        }

      }

      "redirect to next page" when {

        def update(subsidyJourneyOpt: Option[SubsidyJourney], formValues: Option[OptionalEORI]) =
          subsidyJourneyOpt.map(_.copy(addClaimEori = AddClaimEoriFormPage(formValues)))

        def testRedirect(optionalEORI: OptionalEORI, inputAnswer: List[(String, String)]): Unit = {
          val updatedSubsidyJourney =
            update(subsidyJourney.some, optionalEORI.some).getOrElse(sys.error("no subsidy journey"))

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right(routes.SubsidyController.getClaimAmount().url))
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some, optionalEORI.some), eori1)(
              Right(updatedSubsidyJourney)
            )
          }
          checkIsRedirect(performAction(inputAnswer: _*), routes.SubsidyController.getAddClaimPublicAuthority().url)
        }

        "user selected yes and enter eori number" in {
          testRedirect(
            OptionalEORI("true", "123456789013".some),
            List("should-claim-eori" -> "true", "claim-eori" -> "123456789013")
          )
        }

        "user selected No " in {
          testRedirect(OptionalEORI("false", None), List("should-claim-eori" -> "false"))
        }

      }

    }

    "handling request to get add claim public authority" must {

      def performAction() = controller.getAddClaimPublicAuthority(FakeRequest())

      "display the page" when {
        "happy path" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
            mockGetPrevious[SubsidyJourney](eori1)(Right("previous"))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-public-authority.title"),
            { doc =>
              doc.select("#claim-public-authority-hint").text() shouldBe "For example, Invest NI, NI Direct."
              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimPublicAuthority().url
            }
          )
        }
      }
    }

    "handling request to post add claim public authority" must {

      def performAction(data: (String, String)*) = controller.postAddClaimPublicAuthority(
        FakeRequest("POST", routes.SubsidyController.postAddClaimPublicAuthority().url).withFormUrlEncodedBody(data: _*)
      )

      "redirect to the next page" when {

        "valid input" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(Some(subsidyJourney)))
            mockUpdate[SubsidyJourney](
              j => j.map(_.copy(publicAuthority = PublicAuthorityFormPage(Some("My Authority")))),
              eori1
            )(
              Right(subsidyJourney.copy(publicAuthority = PublicAuthorityFormPage(Some("My Authority"))))
            )
          }
          checkIsRedirect(
            performAction("claim-public-authority" -> "My Authority"),
            routes.SubsidyController.getAddClaimReference().url
          )
        }
      }
    }

    "handling request to get Add Claim Reference" must {
      def performAction() = controller
        .getAddClaimReference(FakeRequest("GET", routes.SubsidyController.getAddClaimReference().url))

      "throw technical error" when {
        val exception = new Exception("oh no")
        " the call to get subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        " the call to get subsidy journey comes back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        def test(subsidyJourney: SubsidyJourney): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[SubsidyJourney](eori1)(Right(subsidyJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("add-claim-trader-reference.title"),
            { doc =>
              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              val inputText = doc.select(".govuk-input").attr("value")

              subsidyJourney.traderRef.value match {
                case Some(OptionalTraderRef(input, traderRef)) =>
                  selectedOptions.attr("value") shouldBe input
                  inputText shouldBe traderRef.getOrElse("")
                case _ => selectedOptions.isEmpty shouldBe true
              }

              val button = doc.select("form")
              button.attr("action") shouldBe routes.SubsidyController.postAddClaimReference().url
            }
          )
        }

        "the user hasn't already answered the question" in {
          test(
            subsidyJourney.copy(
              traderRef = TraderRefFormPage(),
              cya = CyaFormPage()
            )
          )
        }

        "the user has already answered the question" in {
          List(
            subsidyJourney,
            subsidyJourney.copy(traderRef = TraderRefFormPage(OptionalTraderRef("false", None).some))
          )
            .foreach { subsidyJourney =>
              withClue(s" for each subsidy journey $subsidyJourney") {
                test(subsidyJourney)
              }

            }

        }

      }

    }

    "handling request to post Add Claim Reference" must {
      def performAction(data: (String, String)*) = controller
        .postAddClaimReference(
          FakeRequest("POST", routes.SubsidyController.getAddClaimReference().url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get previous fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "show form error" when {

        def testFormError(inputAnswer: Option[List[(String, String)]], errorMessageKey: String): Unit = {
          val answers = inputAnswer.getOrElse(Nil)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[SubsidyJourney](eori1)(Right("add-claim-public-authority"))
          }
          checkFormErrorIsDisplayed(
            performAction(answers: _*),
            messageFromMessageKey("add-claim-trader-reference.title"),
            messageFromMessageKey(errorMessageKey)
          )

        }

        "nothing is selected" in {
          testFormError(None, "add-claim-trader-reference.error.required")
        }

        "yes is selected but no trader reference is added" in {
          testFormError(Some(List("should-store-trader-ref" -> "true")), "add-claim-trader-reference.error.isempty")
        }

      }

    }

    "handling request to get remove subsidy claim" must {

      val transactionId = "TID1234"

      def performAction(transactionId: String) = controller
        .getRemoveSubsidyClaim(transactionId)(
          FakeRequest("GET", routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
        )

      "throw technical error" when {

        "call to fetch undertaking fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to fetch undertaking passes but come back empty" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to fetch undertaking passes but come back with empty reference" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy failed" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }

        "call to retrieve subsidy comes back with nonHMRC subsidy usage list with missing transactionId" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.successful(undertakingSubsidies))
          }
          assertThrows[Exception](await(performAction(transactionId)))
        }
      }

      "display the page" in {

        def expectedRows(nonHmrcSubsidy: NonHmrcSubsidy) = List(
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.claimDate.key"),
            nonHmrcSubsidy.allocationDate.toDisplayFormat
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.amount.key"),
            nonHmrcSubsidy.nonHMRCSubsidyAmtEUR.toEuros
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.claim.key"),
            nonHmrcSubsidy.businessEntityIdentifier.getOrElse("")
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.authority.key"),
            nonHmrcSubsidy.publicAuthority.getOrElse("")
          ),
          RemoveSubsidyRow(
            messageFromMessageKey("subsidy.cya.summary-list.traderRef.key"),
            nonHmrcSubsidy.traderReference.getOrElse("")
          )
        )
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[Undertaking](eori1)(Right(undertaking1.some))
          mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.successful(undertakingSubsidies1))
        }
        checkPageIsDisplayed(
          performAction(transactionId),
          List(messageFromMessageKey("subsidy.remove.title"), messageFromMessageKey("subsidy.remove.yesno.legend"))
            .mkString(" "),
          { doc =>
            val rows =
              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
                val question = element.select(".govuk-summary-list__key").text()
                val answer = element.select(".govuk-summary-list__value").text()
                RemoveSubsidyRow(question, answer)
              }
            rows shouldBe expectedRows(nonHmrcSubsidyList1.head)
            val button = doc.select("form")
            button.attr("action") shouldBe routes.SubsidyController.postRemoveSubsidyClaim(transactionId).url

          }
        )
      }

    }

    "handling post remove subsidy claim" must {

      def performAction(data: (String, String)*)(transactionId: String) = controller
        .postRemoveSubsidyClaim(transactionId)(
          FakeRequest("POST", routes.SubsidyController.getRemoveSubsidyClaim(transactionId).url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        "call to get undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to get undertaking passes but comes back empty" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to get undertaking passes but comes back with No reference" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to retrieve subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

        "call to remove subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.successful(undertakingSubsidies1))
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("removeSubsidyClaim" -> "true")("TID1234")))
        }

      }

      "display page error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.successful(undertakingSubsidies1))
          }
          checkFormErrorIsDisplayed(
            performAction()("TID1234"),
            List(messageFromMessageKey("subsidy.remove.title"), messageFromMessageKey("subsidy.remove.yesno.legend"))
              .mkString(" "),
            messageFromMessageKey("subsidy.remove.error.required")
          )
        }
      }

      "redirect to next page" when {

        "If user select yes" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[Undertaking](eori1)(Right(undertaking1.some))
            mockRetrieveSubsidy(SubsidyRetrieve(undertakingRef, None))(Future.successful(undertakingSubsidies1))
            mockRemoveSubsidy(undertakingRef, nonHmrcSubsidyList1.head)(Right(undertakingRef))
            mockSendAuditEvent[NonCustomsSubsidyRemoved](AuditEvent.NonCustomsSubsidyRemoved("1123", undertakingRef))
          }
          checkIsRedirect(
            performAction("removeSubsidyClaim" -> "true")("TID1234"),
            routes.SubsidyController.getReportPayment().url
          )

        }

        "if user selects no" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }
          checkIsRedirect(
            performAction("removeSubsidyClaim" -> "false")("TID1234"),
            routes.SubsidyController.getReportPayment().url
          )

        }

      }
    }

    "handling post to check your answers" must {

      def performAction(data: (String, String)*) = controller
        .postCheckAnswers(
          FakeRequest("POST", routes.SubsidyController.getCheckAnswers().url)
            .withFormUrlEncodedBody(data: _*)
        )

      def update(subsidyJourneyOpt: Option[SubsidyJourney]) = subsidyJourneyOpt
        .map(_.copy(cya = CyaFormPage(value = true.some)))

      val updatedJourney = subsidyJourney.copy(cya = CyaFormPage(value = true.some))

      "throw technical error" when {

        "call to update subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to fetch undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some), eori1)(Right(updatedJourney))
            mockRetreiveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to fetch undertaking come back with No reference" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some), eori1)(Right(updatedJourney))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to create subsidy fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some), eori1)(Right(updatedJourney))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              undertakingRef,
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to reset subsidy journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](_ => update(subsidyJourney.some), eori1)(Right(updatedJourney))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              undertakingRef,
              SubsidyController.toSubsidyUpdate(subsidyJourney, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }
      }

      "redirect to next page" when {

        "cya page is reached via update journey" in {

          val subsidyJourneyExisting = subsidyJourney.copy(existingTransactionId = SubsidyRef("TD123").some)
          val updatedSJ = subsidyJourneyExisting.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourneyExisting.some),
              eori1
            )(Right(updatedSJ))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              undertakingRef,
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
            mockSendAuditEvent[AuditEvent.NonCustomsSubsidyUpdated](
              AuditEvent.NonCustomsSubsidyUpdated(
                ggDetails = "1123",
                undertakingRef = undertakingRef,
                subsidyJourney = updatedSJ,
                currentDate
              )
            )
          }

          checkIsRedirect(
            performAction("cya" -> "true"),
            routes.SubsidyController.getReportPayment().url
          )
        }

        "cya page is reached via add journey" in {

          val updatedSJ = subsidyJourney.copy(cya = CyaFormPage(value = true.some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[SubsidyJourney](
              _ => update(subsidyJourney.some),
              eori1
            )(Right(updatedSJ))
            mockRetreiveUndertaking(eori1)(Future.successful(undertaking1.some))
            mockTimeProviderToday(currentDate)
            mockCreateSubsidy(
              undertakingRef,
              SubsidyController.toSubsidyUpdate(updatedSJ, undertakingRef, currentDate)
            )(Right(undertakingRef))
            mockPut[SubsidyJourney](SubsidyJourney(), eori1)(Right(SubsidyJourney()))
            mockSendAuditEvent[AuditEvent.NonCustomsSubsidyAdded](
              AuditEvent.NonCustomsSubsidyAdded(
                ggDetails = "1123",
                leadEori = eori1,
                undertakingRef = undertakingRef,
                subsidyJourney = updatedSJ,
                currentDate
              )
            )
          }

          checkIsRedirect(
            performAction("cya" -> "true"),
            routes.SubsidyController.getReportPayment().url
          )
        }
      }

    }

  }

}

object SubsidyControllerSpec {
  case class RemoveSubsidyRow(key: String, value: String)
}
