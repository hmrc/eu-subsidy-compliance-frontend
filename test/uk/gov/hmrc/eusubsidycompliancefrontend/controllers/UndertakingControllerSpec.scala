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
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.UndertakingControllerSpec.AmendUndertakingRows
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, ContactDetails, Error, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, FormPage, JourneyTraverseService, Store, UndertakingJourney}
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonTestData._

import scala.collection.JavaConverters._
import scala.concurrent.Future

class UndertakingControllerSpec extends ControllerSpec
  with AuthSupport
  with JourneyStoreSupport
  with AuthAndSessionDataBehaviour
  with JourneySupport {
  val mockEscService = mock[EscService]

  override def overrideBindings = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[JourneyTraverseService].toInstance(mockJourneyTraverseService)
  )

  val controller: UndertakingController = instanceOf[UndertakingController]

  def mockRetreiveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  def mockUpdateUndertaking(undertaking: Undertaking)(result: Either[Error, UndertakingRef]) =
    (mockEscService
      .updateUndertaking(_: Undertaking)(_: HeaderCarrier))
      .expects(undertaking, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)),Future.successful))

  def mockAddMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(result: Either[Error, UndertakingRef]) =
    (mockEscService
      .addMember(_: UndertakingRef, _:  BusinessEntity)(_: HeaderCarrier))
      .expects(undertakingRef, businessEntity, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)),Future.successful))

  "UndertakingControllerSpec" when {

    "handling request to get Undertaking Name" must {

      def performAction() = controller.getUndertakingName(FakeRequest())

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to store undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(None))
            mockPut[UndertakingJourney](UndertakingJourney(), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "no undertaking journey is not there in store" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(None))
            mockPut[UndertakingJourney](UndertakingJourney(), eori1)(Right(UndertakingJourney()))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingName.title"),
            {doc =>
              val input = doc.select(".govuk-input").attr("value")
              input shouldBe ""

              val button = doc.select("form")
              button.attr("action") shouldBe routes.UndertakingController.postUndertakingName().url
            }
          )
        }

      " undertaking journey is there in store" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertakingName.title"),
          { doc =>
            val input = doc.select(".govuk-input").attr("value")
            input shouldBe "TestUndertaking"

            val button = doc.select("form")
            button.attr("action") shouldBe routes.UndertakingController.postUndertakingName().url
          }
        )
      }
      }

    }

    "handling request to post Undertaking Name " must {

      def performAction(data: (String, String)*) = controller
        .postUndertakingName(
          FakeRequest("POST",routes.UndertakingController.getUndertakingName().url)
            .withFormUrlEncodedBody(data: _*))

      "throw technical error" when {
        val exception = new Exception("oh no")
        "call to update undertaking journey fails" in {

          def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
            undertakingJourneyOpt.map(_.copy(name = FormPage("undertaking-name", "TestUndertaking123".some)))
          }
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingName" -> "TestUndertaking123")))
        }
      }

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
          }
          checkFormErrorIsDisplayed(performAction("undertakingName" -> ""),
            messageFromMessageKey("undertakingName.title"),
            messageFromMessageKey("error.undertakingName.required")
          )
        }
      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String) = {
          def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
            undertakingJourneyOpt.map(_.copy(name = FormPage("undertaking-name", "TestUndertaking123".some)))
          }

          val updatedUndertaking = undertakingJourney.copy(name = FormPage("undertaking-name", "TestUndertaking123".some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourney.some), eori1)(Right(updatedUndertaking))
          }
          checkIsRedirect(performAction("undertakingName" -> "TestUndertaking123"), nextCall)
        }

        "page is reached via amend details page " in {
          test(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails().url)

        }

        "page is reached via normal undertaking creation process" in {
          test(undertakingJourneyComplete, "sector")
        }
      }


    }

    "handling request to get sector" must {

      def performAction() = controller.getSector(FakeRequest("GET",routes.UndertakingController.getSector().url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking journey passes  but return no undertaking journey" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        val allRadioTexts: List[String] = List(
          s"${messageFromMessageKey("sector.label.3")}" +
            s" ${messageFromMessageKey("sector.hint.3")}",
          s"${messageFromMessageKey("sector.label.2")}" +
            s" ${messageFromMessageKey("sector.hint.2")}",
          s"${messageFromMessageKey("sector.label.1")}" +
            s" ${messageFromMessageKey("sector.hint.1")}",
          messageFromMessageKey("sector.label.0")
        )

        def test(undertakingJourney: UndertakingJourney, previousCall: String, inputValue: Option[String]) = {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingSector.title", undertakingJourney.name.value.getOrElse("")),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputValue match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None        => selectedOptions.isEmpty       shouldBe true
              }

              testRadioButtonOptions(doc, allRadioTexts)

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postSector().url
            }
          )

        }

        "user has not already answered the question (normal add undertaking journey)" in {
          test(undertakingJourney = UndertakingJourney(name = FormPage("undertaking-name", "TestUndertaking1".some)),
            previousCall = "undertaking-name",
            inputValue = None)
        }

        "user has already answered the question (normal add undertaking journey)" in {
          test(undertakingJourney = UndertakingJourney(name = FormPage("undertaking-name", "TestUndertaking1".some),
            sector = FormPage("sector", Sector(2).some)),
            previousCall = "undertaking-name",
            inputValue = "2".some)
        }

        "user has already answered the question and is on Amend journey" in {
          test(undertakingJourney = undertakingJourneyComplete1,
            previousCall = routes.UndertakingController.getAmendUndertakingDetails().url,
            inputValue = "2".some)
        }

      }

    }

    "handling request to post sector" must {
      def performAction(data: (String, String)*) = controller
        .postSector(
          FakeRequest("POST",routes.UndertakingController.getSector().url)
            .withFormUrlEncodedBody(data: _*))



      def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
        undertakingJourneyOpt.map(_.copy(sector = FormPage("sector", Sector(1).some)))
      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get previous url fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        }

        "call to update undertaking journey fails" in {
          val currentUndertaking = UndertakingJourney(name = FormPage("undertaking-name", "TestUndertaking".some))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("undertaking-name"))
            mockUpdate[UndertakingJourney](_ => update(currentUndertaking.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        }

      }

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("undertaking-name"))
          }
          checkFormErrorIsDisplayed(performAction(),
            messageFromMessageKey("undertakingSector.title",""),
            messageFromMessageKey("undertakingSector.error.required")
          )

        }

      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String) = {

          val newSector = FormPage("sector", Sector(3).some)
          def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
            undertakingJourneyOpt.map(_.copy(sector = newSector))
          }

          val updatedUndertaking = undertakingJourney.copy(sector = newSector)
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("undertaking-name"))
            mockUpdate[UndertakingJourney](_ => update(undertakingJourney.some), eori1)(Right(updatedUndertaking))
          }
          checkIsRedirect(performAction("undertakingSector" -> "3"), nextCall)
        }

        "page is reached via amend details page " in {
          test(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails().url)

        }

        "page is reached via normal undertaking creation process" in {
          test(undertakingJourneyComplete, "contact")
        }

      }

    }

    "handling request to get Contact" must {

      def performAction() = controller.getContact(FakeRequest("GET",routes.UndertakingController.getContact().url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking journey passes  but return no undertaking journey" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        def test(undertakingJourney: UndertakingJourney, previousCall: String): Unit = {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingContact.title") + " " + undertakingJourney.name.value.getOrElse("") + messageFromMessageKey("undertakingContact.title.end") ,
            {doc =>

              doc.select(".govuk-back-link").attr("href") shouldBe(previousCall)

              val inputPhone = doc.select("#phone").attr("value")
              val inputMobile = doc.select("#mobile").attr("value")

              inputPhone shouldBe undertakingJourney.contact.value.flatMap(_.phone).getOrElse("")
              inputMobile shouldBe undertakingJourney.contact.value.flatMap(_.mobile).getOrElse("")

              val button = doc.select("form")
              button.attr("action") shouldBe routes.UndertakingController.postContact().url
            }
          )
        }

        "user has not already answered the question(normal add undertaking journey)" in {
          test(undertakingJourneyComplete.copy(contact = FormPage("contact"), cya = FormPage("check-your-answers")),"sector")
        }

        "user has  already answered the question(normal add undertaking journey)" in {
          test(undertakingJourneyComplete.copy(contact = FormPage("contact", contactDetails), cya = FormPage("check-your-answers")),"sector")
        }

        "user has  already answered the question but it's amend journey" in {
          test(undertakingJourneyComplete1.copy(contact = FormPage("contact", contactDetails), cya = FormPage("check-your-answers")),routes.UndertakingController.getAmendUndertakingDetails().url)
        }

      }
    }

    "handling request to post Contact" must {

      def performAction(data: (String, String)*) = controller
        .postContact(
          FakeRequest("POST",routes.UndertakingController.getContact().url)
            .withFormUrlEncodedBody(data: _*))

      def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
        undertakingJourneyOpt.map(_.copy(contact = FormPage("contact", contactDetails2)))
      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get previous url fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("phone" -> "222", "mobile" -> "333")))
        }

        "call to update undertaking journey fails" in {
          val currentUndertaking = undertakingJourneyComplete1.copy(contact = FormPage("contact"), cya = FormPage("cya"))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("undertaking-name"))
            mockUpdate[UndertakingJourney](_ => update(currentUndertaking.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("phone" -> "222", "mobile" -> "333")))
        }
      }

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("sector"))
          }
          checkFormErrorsAreDisplayed(performAction(),
            messageFromMessageKey("undertakingContact.title") + " " + "" + messageFromMessageKey("undertakingContact.title.end") ,
            List(
              messageFromMessageKey("one.or.other.mustbe.present"),
              messageFromMessageKey("one.or.other.mustbe.present")
            )
          )

        }

      }

      "redirect to next page" when {


        def test(undertakingJourney: UndertakingJourney, nextCall: String) = {

          val updatedUndertaking = undertakingJourney.copy(contact = FormPage("contact", contactDetails2))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGetPrevious[UndertakingJourney](eori1)(Right("sector"))
            mockUpdate[UndertakingJourney](_ => update(undertakingJourney.some), eori1)(Right(updatedUndertaking))
          }
          checkIsRedirect(performAction("phone" -> "222", "mobile" -> "333"), nextCall)
        }

        "page is reached via normal create undertaking" in {
          test(undertakingJourneyComplete.copy(contact = FormPage("contact"), cya = FormPage("cya")), "cya")
        }

        "page is reached via amend  undertaking journey" in {
          test(undertakingJourneyComplete1.copy(contact = FormPage("contact"), cya = FormPage("cya")), routes.UndertakingController.getAmendUndertakingDetails().url)
        }

      }

    }

    "handling request to get Amend Undertaking Details " must {

      def performAction() = controller.getAmendUndertakingDetails(FakeRequest())

      def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
        undertakingJourneyOpt.map(_.copy(isAmend = FormPage("amend-undertaking", true.some)))
      }

      val contact = contactDetails match {
        case Some(ContactDetails(Some(number1), Some(number2))) => s"$number1 $number2"
        case Some(ContactDetails(Some(number1), None)) => s"$number1"
        case Some(ContactDetails(None, Some(number2))) => s"$number2"
        case _ => ""
      }

      val expectedRows = List(
        AmendUndertakingRows(
          messageFromMessageKey("undertaking.amendUndertaking.summary-list.name.key"),
          undertaking.name,
          routes.UndertakingController.getUndertakingName().url
        ),
        AmendUndertakingRows(
          messageFromMessageKey("undertaking.amendUndertaking.summary-list.sector.key"),
          messageFromMessageKey(s"sector.label.${undertaking.industrySector.id.toString}"),
          routes.UndertakingController.getSector().url
        ),
        AmendUndertakingRows(
          messageFromMessageKey("undertaking.amendUndertaking.summary-list.telephone.key"),
          contact,
          routes.UndertakingController.getContact().url
        )
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to get undertaking journey fetches nothing" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the undertaking journey fails" in {

          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" in {
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(isAmend = FormPage("amend-undertaking", true.some))))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.amendUndertaking.title"),
          {doc =>

            doc.select(".govuk-back-link").attr("href") shouldBe(routes.AccountController.getAccountPage().url)

            val rows =
              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
                val question  = element.select(".govuk-summary-list__key").text()
                val answer    = element.select(".govuk-summary-list__value").text()
                val changeUrl = element.select(".govuk-link").attr("href")
                AmendUndertakingRows(question, answer, changeUrl)
              }
            rows shouldBe expectedRows
          }
        )

      }
      
    }

    "handling request to post Amend undertaking" must {

      def performAction(data: (String, String)*) = controller
        .postAmendUndertaking(
          FakeRequest("POST",routes.UndertakingController.getAmendUndertakingDetails().url)
            .withFormUrlEncodedBody(data: _*))

      def update(undertakingJourneyOpt: Option[UndertakingJourney]) = {
        undertakingJourneyOpt.map(_.copy(isAmend = FormPage("amend-undertaking", None)))
      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update undertaking journey fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to update undertaking journey passes but return undertaking with no name" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(name = FormPage("undertaking-name", None))))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to update undertaking journey passes but return undertaking with no secctor" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(name = FormPage("sector", None))))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to retrieve undertaking fails" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(name = FormPage("amend-undertaking", "true".some))))
            mockRetreiveUndertaking(eori)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to retrieve undertaking passes but no undertaking was fetched" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(name = FormPage("amend-undertaking", "true".some))))
            mockRetreiveUndertaking(eori)(Future.successful(None))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to retrieve undertaking passes but  undertaking was fetched with no undertaking ref" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(name = FormPage("amend-undertaking", "true".some))))
            mockRetreiveUndertaking(eori)(Future.successful(undertaking1.copy(reference = None).some))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to retrieve undertaking passes but retrieve undertaking without Lead Business Entity" in {
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(isAmend = FormPage("amend-undertaking", true.some))))
            mockRetreiveUndertaking(eori)(Future.successful(undertaking1.copy(undertakingBusinessEntity = undertaking1.undertakingBusinessEntity.filterNot(_.leadEORI)).some))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to update undertaking fails" in {
          val updatedUndertaking = undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(isAmend = FormPage("amend-undertaking", true.some))))
            mockRetreiveUndertaking(eori)(Future.successful(undertaking1.some))
            mockUpdateUndertaking(updatedUndertaking)(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to add  member fails" in {
          val updatedUndertaking = undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
          inSequence {
            mockAuthWithNecessaryEnrolment()
            mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(isAmend = FormPage("amend-undertaking", true.some))))
            mockRetreiveUndertaking(eori)(Future.successful(undertaking1.some))
            mockUpdateUndertaking(updatedUndertaking)(Right(undertakingRef))
            mockAddMember(undertakingRef, businessEntity1.copy(contacts = contactDetails))(Left(Error(exception)))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }
      }

      "redirect to next page" in {
        val updatedUndertaking = undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
        inSequence {
          mockAuthWithNecessaryEnrolment()
          mockUpdate[UndertakingJourney](_ => update(undertakingJourneyComplete.some), eori1)(Right(undertakingJourneyComplete.copy(isAmend = FormPage("amend-undertaking", true.some))))
          mockRetreiveUndertaking(eori)(Future.successful(undertaking1.some))
          mockUpdateUndertaking(updatedUndertaking)(Right(undertakingRef))
          mockAddMember(undertakingRef, businessEntity1.copy(contacts = contactDetails))(Right(undertakingRef))
        }
        checkIsRedirect(performAction("amendUndertaking" -> "true"), routes.AccountController.getAccountPage().url)
      }

    }
  }

}

object UndertakingControllerSpec {
  final case class AmendUndertakingRows(question: String, answer: String, changeUrl: String)
}
