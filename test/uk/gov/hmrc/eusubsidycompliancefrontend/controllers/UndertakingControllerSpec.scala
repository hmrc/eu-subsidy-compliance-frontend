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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import cats.implicits.catsSyntaxOptionId
import org.jsoup.Jsoup
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.UndertakingControllerSpec.{ModifyUndertakingRow, SectorRadioOption}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailSendResult.EmailSent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{CreateUndertaking, DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.{Amend, EmailStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EmailStatus, IndustrySectorLimit, Sector, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, VerifiedEmail}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

import java.time.LocalDate
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class UndertakingControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with EmailSupport
    with AuditServiceSupport
    with EscServiceSupport
    with EmailVerificationServiceSupport
    with TimeProviderSupport {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore),
    bind[EscService].toInstance(mockEscService),
    bind[EmailService].toInstance(mockEmailService),
    bind[AuditService].toInstance(mockAuditService),
    bind[TimeProvider].toInstance(mockTimeProvider),
    bind[EmailVerificationService].toInstance(mockEmailVerificationService)
  )

  override def additionalConfig: Configuration = super.additionalConfig.withFallback(
    Configuration.from(
      Map(
        "play.i18n.langs" -> Seq("en", "cy", "fr"),
        "email-send.create-undertaking-template-en" -> "template_EN",
        "email-send.create-undertaking-template-cy" -> "template_CY"
      )
    )
  )

  private val controller = instanceOf[UndertakingController]
  val exception = new Exception("oh no")

  "UndertakingController" when {

    "handling request to first empty page" must {

      def performAction() =
        controller.firstEmptyPage(FakeRequest())

      "throw technical error" when {

        "call to Get Or Create undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "redirects to next page" when {

        "undertaking journey is present and  is not None and is complete" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))
          }
          checkIsRedirect(performAction(), routes.BusinessEntityController.getAddBusinessEntity().url)
        }

        "undertaking journey is present and  is not None and is not complete" when {

          def testRedirect(undertakingJourney: UndertakingJourney, redirectTo: String): Unit = {
            inSequence {
              mockAuthWithEnrolmentAndNoEmailVerification()
              mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourney))
            }
            checkIsRedirect(performAction(), redirectTo)
          }

          "undertaking journey only contains undertaking name" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some)
              ),
              routes.UndertakingController.getSector.url
            )
          }

          "undertaking journey contains undertaking name and sector" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some)
              ),
              routes.UndertakingController.getConfirmEmail.url
            )
          }

          "undertaking journey contains undertaking name, sector and verified email" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some),
                verifiedEmail = UndertakingConfirmEmailFormPage("joe.bloggs@something.com".some)
              ),
              routes.UndertakingController.getAddBusiness.url
            )
          }

          "undertaking journey contains undertaking name, sector, verified email and add business" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some),
                verifiedEmail = UndertakingConfirmEmailFormPage("joe.bloggs@something.com".some),
                addBusiness = UndertakingAddBusinessFormPage(false.some)
              ),
              routes.UndertakingController.getCheckAnswers.url
            )
          }

          "undertaking journey contains cya" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some),
                verifiedEmail = UndertakingConfirmEmailFormPage("joe.bloggs@something.com".some),
                addBusiness = UndertakingAddBusinessFormPage(false.some),
                cya = UndertakingCyaFormPage(true.some)
              ),
              routes.UndertakingController.postConfirmation.url
            )
          }

          "undertaking journey contains confirmation" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some),
                cya = UndertakingCyaFormPage(true.some),
                verifiedEmail = UndertakingConfirmEmailFormPage("joe.bloggs@something.com".some),
                addBusiness = UndertakingAddBusinessFormPage(false.some),
                confirmation = UndertakingConfirmationFormPage(true.some)
              ),
              routes.BusinessEntityController.getAddBusinessEntity().url
            )
          }

        }

      }

    }

    "handling request to get About Undertaking" must {

      def performAction() =
        controller.getAboutUndertaking(FakeRequest(GET, routes.UndertakingController.getAboutUndertaking.url))

      "display the page" when {

        def testDisplay(undertakingJourney: UndertakingJourney, backUrl: String): Unit = {

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingName.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe backUrl
              doc.select("form").attr("action") shouldBe routes.UndertakingController.postAboutUndertaking.url
            }
          )

        }

        "no undertaking journey is there in store" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingName.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.EligibilityController.getEoriCheck.url
              val input = doc.select(".govuk-input").attr("value")
              input shouldBe ""

              val button = doc.select("form")
              button.attr("action") shouldBe routes.UndertakingController.postAboutUndertaking.url
            }
          )
        }

        "undertaking journey is there in store and user has already answered the questions and all answers are complete" in {
          testDisplay(undertakingJourneyComplete, routes.EligibilityController.getEoriCheck.url)
        }

        "undertaking journey is there in store and user hasn't  answered any questions" in {
          testDisplay(UndertakingJourney(), routes.EligibilityController.getEoriCheck.url)
        }

        "undertaking journey is there in store and user has answered the question but journey is not complete" in {
          testDisplay(
            UndertakingJourney(
              about = AboutUndertakingFormPage("TestUndertaking".some)
            ),
            routes.EligibilityController.getEoriCheck.url
          )
        }

        "page appeared via amend undertaking journey" in {
          testDisplay(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails.url)
        }
      }

    }

    "handling request to post About Undertaking" must {

      def performAction(data: (String, String)*) = controller
        .postAboutUndertaking(
          FakeRequest(POST, routes.UndertakingController.getAboutUndertaking.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to  get undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingName" -> "TestUndertaking123")))
        }

        "call to  get undertaking journey passes but com back with empty response" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction("undertakingName" -> "TestUndertaking123")))
        }

        "call to update undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("continue" -> "true")))
        }

        "submitted form does not contain expected data" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          assertThrows[IllegalStateException](await(performAction("this is not" -> "valid")))
        }
      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String): Unit = {
          val updatedUndertaking = undertakingJourney.copy(about = AboutUndertakingFormPage("TestUndertaking123".some))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockUpdate[UndertakingJourney](eori1)(
              Right(updatedUndertaking)
            )
          }
          checkIsRedirect(performAction("continue" -> "true"), nextCall)
        }

        "page is reached via amend details page " in {
          test(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails.url)

        }

        "page is reached via normal undertaking creation process" in {
          test(UndertakingJourney(), routes.UndertakingController.getSector.url)
        }

        "page is reached via normal undertaking creation process when all answers have been provided" in {
          test(undertakingJourneyComplete, routes.UndertakingController.getCheckAnswers.url)
        }

      }

    }

    "handling request to get sector" must {

      def performAction() = controller.getSector(FakeRequest(GET, routes.UndertakingController.getSector.url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        val allRadioTexts: List[SectorRadioOption] = List(
          SectorRadioOption(
            "agriculture",
            "Primary production of agricultural products",
            "An ‘agricultural product’ can be products such as: live animals, meat and edible meat offal, dairy produce, birds’ eggs, natural honey. The cap for this sector is €20,000."
          ),
          SectorRadioOption(
            "aquaculture",
            "Fishery and aquaculture products",
            "If any part of your business is involved in the production, processing or marketing of fishery and aquaculture products. A ‘fishery product’ means aquatic organisms resulting from any fishing activity or products derived from them. The cap for this sector is €30,000."
          ),
          SectorRadioOption(
            "transport",
            "Road freight transport for hire or reward",
            "This sector includes couriers and hauliers who get paid by someone to transport their goods by road. The cap for this sector is €100,000."
          ),
          SectorRadioOption("other", "Other", "The cap for all other sectors is €200,000.")
        )

        def test(undertakingJourney: UndertakingJourney, previousCall: String, inputValue: Option[String]): Unit = {

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingSector.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputValue match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }

              testRadioButtonOptions(doc, allRadioTexts)

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postSector.url
            }
          )

        }

        "user has not already answered the question (normal add undertaking journey)" in {
          test(
            undertakingJourney = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking1".some)),
            previousCall = routes.UndertakingController.getAboutUndertaking.url,
            inputValue = None
          )
        }

        "user has already answered the question (normal add undertaking journey)" in {
          test(
            undertakingJourney = UndertakingJourney(
              about = AboutUndertakingFormPage("TestUndertaking1".some),
              sector = UndertakingSectorFormPage(Sector(2).some)
            ),
            previousCall = routes.UndertakingController.getAboutUndertaking.url,
            inputValue = "2".some
          )
        }

        "user has already answered the question and is on Amend journey" in {
          test(
            undertakingJourney = undertakingJourneyComplete1,
            previousCall = routes.UndertakingController.getAmendUndertakingDetails.url,
            inputValue = "2".some
          )
        }

        "legend is displayed in fieldset component" in {
          val undertakingJourney = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking1".some))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }

          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val legendText: String =
            document
              .getElementsByClass("govuk-fieldset__legend--xl")
              .text()
          legendText shouldBe "What is the industry sector of your undertaking?"

          val hintText: String =
            document
              .getElementById("undertakingSector-hint")
              .text()
          hintText shouldBe "Your undertaking may have businesses working in different sectors. For your whole undertaking the lowest subsidy allowance will apply."
        }

      }

      "redirect to journey start page" when {

        "call to fetch undertaking journey passes  but return no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to previous question" when {

        "about has not been answered" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

    }

    "handling request to post sector" must {
      def performAction(data: (String, String)*) = controller
        .postSector(
          FakeRequest(POST, routes.UndertakingController.getSector.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get previous url fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        }

        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to fetch undertaking journey passes  buy fetches nothing" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update undertaking journey fails" in {
          val currentUndertaking = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking".some))
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(currentUndertaking.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        }

      }

      "display form error" when {

        "nothing is submitted" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage()).some)
            )
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingSector.title"),
            messageFromMessageKey("undertakingSector.error.required")
          )

        }

      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String): Unit = {

          val newSector = UndertakingSectorFormPage(Sector(3).some)

          val updatedUndertaking = undertakingJourney.copy(sector = newSector)
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertaking))
          }
          checkIsRedirect(performAction("undertakingSector" -> "3"), nextCall)
        }

        "page is reached via amend details page " in {
          test(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails.url)

        }

        "page is reached via normal undertaking creation process" in {
          test(UndertakingJourney(), routes.UndertakingController.getConfirmEmail.url)
        }

        "page is reached via normal undertaking creation process when all answers have been provided" in {
          test(undertakingJourneyComplete, routes.UndertakingController.getCheckAnswers.url)
        }

      }

    }

    "handling request to get verify email" must {

      def performAction(pendingVerificationId: String) = controller.getVerifyEmail(pendingVerificationId)(
        FakeRequest(GET, routes.UndertakingController.getVerifyEmail(pendingVerificationId).url)
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("abcdefh")))
        }

      }

      "display the page" when {

        "User has verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            verifiedEmail = UndertakingConfirmEmailFormPage("joe.bloggs@something.com".some)
          )

          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockApproveVerification(eori1, "id")(Right(true))
            mockGetEmailVerification(eori1)(Right(VerifiedEmail("", "", verified = true).some))
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourney))
          }

          redirectLocation(performAction("id")) shouldBe Some(routes.UndertakingController.getAddBusiness.url)
        }
      }

    }

    "handling request to get confirm email" must {

      def performAction() =
        controller.getConfirmEmail(FakeRequest(GET, routes.UndertakingController.getConfirmEmail.url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "User has verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some)
          )
          val previousCall = routes.UndertakingController.getSector.url

          val email = "email@test.com"
          val pageTitle = s"Is $email the right email address to receive notifications?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockGetEmailVerification(eori1)(Right(VerifiedEmail(email, "id123", verified = true).some))
          }
          checkPageIsDisplayed(
            performAction(),
            pageTitle,
            { doc =>
              val heading = doc.getElementsByClass("govuk-fieldset__heading")
              heading.size shouldBe 1
              heading.text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              doc
                .getElementsByTag("legend")
                .hasClass(
                  "govuk-fieldset__legend govuk-fieldset__legend--xl govuk-!-display-block break-word"
                ) shouldBe true

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postConfirmEmail.url
            }
          )

        }

        "User does not have verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some)
          )
          val previousCall = routes.UndertakingController.getSector.url

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockGetEmailVerification(eori1)(Right(VerifiedEmail("", "", verified = true).some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("confirmEmail.title", ""),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postConfirmEmail.url
            }
          )

        }
      }

      "redirect to previous page" when {

        "call to fetch undertaking journey returns no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getSector.url)
        }
      }

    }

    "handling request to post confirm email call" must {

      def performAction(data: (String, String)*) =
        controller.postConfirmEmail(
          FakeRequest(POST, routes.UndertakingController.postConfirmEmail.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        "email submitted is empty" in {

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          assertThrows[Exception](
            await(
              performAction(
                "using-stored-email" -> "false"
              )
            )
          )
        }

        "email submitted is invalid" in {

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          assertThrows[Exception](
            await(
              performAction(
                "using-stored-email" -> "false",
                "email" -> "joe bloggs"
              )
            )
          )
        }

      }

      "return bad request and show error" when {

        "invalid email format - no domain" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
          }
          checkFormErrorIsDisplayed(
            result = performAction("using-stored-email" -> "false", "email" -> "some@thing"),
            expectedTitle = messageFromMessageKey("confirmEmail.title", email),
            formError = messageFromMessageKey("email.error.email")
          )
        }

        "invalid email format - has spaces" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
          }
          checkFormErrorIsDisplayed(
            result = performAction("using-stored-email" -> "false", "email" -> "some @ thing.com"),
            expectedTitle = messageFromMessageKey("confirmEmail.title", email),
            formError = messageFromMessageKey("email.error.email")
          )
        }

        "invalid email format - no @" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
          }
          checkFormErrorIsDisplayed(
            result = performAction("using-stored-email" -> "false", "email" -> "something.com"),
            expectedTitle = messageFromMessageKey("confirmEmail.title", email),
            formError = messageFromMessageKey("email.error.email")
          )
        }

      }

      "redirect to add business page" when {

        "all api calls are successful" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
            mockAddVerifiedEmail(eori1, "foo@example.com")()
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))

          }
          checkIsRedirect(
            performAction("using-stored-email" -> "true"),
            routes.UndertakingController.getAddBusiness.url
          )
        }

        "No verification found or cds with valid form should redirect" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
            mockMakeVerificationRequestAndRedirect(Redirect("email-verification-redirect").toFuture)
          }
          redirectLocation(
            performAction("using-stored-email" -> "false", "email" -> "something@aol.com")
          ) shouldBe "email-verification-redirect".some
        }

        "No verification found or cds with invalid form should be bad request" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerification()
          }
          status(performAction("using-stored-email" -> "false", "email" -> "somethingl.com")) shouldBe BAD_REQUEST
        }

      }

    }

    "handling request to get intention to add business" must {

      def performAction() = controller.getAddBusiness(
        FakeRequest(GET, routes.UndertakingController.getAddBusiness.url)
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to fetch undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        "When Change of undertaking business is available in" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            verifiedEmail = UndertakingConfirmEmailFormPage("some@email.com".some)
          )

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }

          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))

          val findTitle = document.getElementById("addBusinessTitleId").text()
          findTitle shouldBe "Businesses in your undertaking"

          val legendText: String =
            document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m").text()
          legendText shouldBe "Are there other businesses in your undertaking?"

        }

        "addBusiness page has an intent" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            verifiedEmail = UndertakingConfirmEmailFormPage("some@email.com".some)
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          val document = Jsoup.parse(contentAsString(performAction()))
          document.select(".govuk-back-link").attr("href") shouldBe previousCall
          val paraElement = document.getElementById("intentId").text()
          paraElement shouldBe messageFromMessageKey("addBusinessIntent.p1")
        }

        "when question has not been answered" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            verifiedEmail = UndertakingConfirmEmailFormPage("some@email.com".some)
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("addBusinessIntent.title", undertakingJourney.about.value.getOrElse("")),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postAddBusiness.url
            }
          )

        }

        "when question has been answered" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            verifiedEmail = UndertakingConfirmEmailFormPage("some@email.com".some),
            addBusiness = UndertakingAddBusinessFormPage(true.some)
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("addBusinessIntent.title", undertakingJourney.about.value.getOrElse("")),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe previousCall

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postAddBusiness.url
            }
          )

        }
      }

      "redirect to journey start page" when {

        "call to fetch undertaking journey passes  but return no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to previous step" when {

        "email question has not been answered" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGet[UndertakingJourney](eori1)(
              Right(
                UndertakingJourney(
                  about = AboutUndertakingFormPage("TestUndertaking".some),
                  sector = UndertakingSectorFormPage(Sector(1).some)
                ).some
              )
            )
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getConfirmEmail.url)
        }
      }

    }

    "handling request to post intention to add business" must {

      def performAction(data: (String, String)*) = controller.postAddBusiness(
        FakeRequest(POST, routes.UndertakingController.postAddBusiness.url).withFormUrlEncodedBody(data: _*)
      )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("addBusinessIntent" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String): Unit = {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          checkFormErrorIsDisplayed(
            performAction(data: _*),
            messageFromMessageKey("addBusinessIntent.title"),
            messageFromMessageKey(errorMessage)
          )
        }

        "nothing has been submitted" in {
          displayErrorTest()("addBusinessIntent.error.required")
        }

      }

      "redirect to the next page" when {

        "user selected No" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(
              Right(UndertakingJourney(addBusiness = UndertakingAddBusinessFormPage(false.some)))
            )
          }
          checkIsRedirect(
            performAction("addBusinessIntent" -> "false"),
            routes.UndertakingController.getCheckAnswers.url
          )
        }

        "user selected Yes" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(
              Right(UndertakingJourney(addBusiness = UndertakingAddBusinessFormPage(true.some)))
            )
          }
          checkIsRedirect(
            performAction("addBusinessIntent" -> "true"),
            routes.UndertakingController.getCheckAnswers.url
          )
        }

      }

    }

    "handling request to get check your answers page" must {

      def performAction() = controller.getCheckAnswers(
        FakeRequest(GET, routes.UndertakingController.getCheckAnswers.url)
      )

      "display the page" in {

        val expectedRows = List(
          ModifyUndertakingRow(
            messageFromMessageKey("undertaking.cya.summary-list.eori.key"),
            eori1,
            "" // User cannot change the EORI on the undertaking
          ),
          ModifyUndertakingRow(
            messageFromMessageKey("undertaking.cya.summary-list.sector.key"),
            messageFromMessageKey(s"sector.label.${undertaking.industrySector.id.toString}"),
            routes.UndertakingController.getSector.url
          ),
          ModifyUndertakingRow(
            messageFromMessageKey("undertaking.cya.summary-list.verified-email"),
            "joebloggs@something.com",
            routes.UndertakingController.getConfirmEmail.url
          ),
          ModifyUndertakingRow(
            messageFromMessageKey("undertaking.cya.summary-list.other-business"),
            "No",
            routes.UndertakingController.getAddBusiness.url
          )
        )
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.cya.title"),
          { doc =>
            doc.select(".govuk-back-link").attr("href") shouldBe routes.UndertakingController.getAddBusiness.url
            val rows =
              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
                val question = element.select(".govuk-summary-list__key").text()
                val answer = element.select(".govuk-summary-list__value").text()
                val changeUrl = element.select(".govuk-link").attr("href")
                ModifyUndertakingRow(question, answer, changeUrl)
              }

            rows shouldBe expectedRows
          }
        )

      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect" when {
        "call to get undertaking journey fetches the journey without verified email" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(verifiedEmail = UndertakingConfirmEmailFormPage()).some)
            )
          }
          redirectLocation(performAction()) shouldBe Some(routes.UndertakingController.getAddBusiness.url)
        }

        "to journey start when call to get undertaking journey fetches nothing" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

    }

    "handling request to Post Check your Answers call" must {

      def performAction(data: (String, String)*) =
        controller.postCheckAnswers(
          FakeRequest(POST, routes.UndertakingController.getCheckAnswers.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {

        val exception = new Exception("oh no !")

        "cya form is empty, nothing is submitted" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update undertaking journey fails" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "updated undertaking journey don't have undertaking name" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "updated undertaking journey don't have undertaking sector" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to create undertaking fails" in {

          val updatedUndertakingJourney = undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage(false.some))

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertakingJourney))
            mockCreateUndertaking(undertakingCreated)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))

        }

        "call to send email fails" in {

          val updatedUndertakingJourney =
            undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage(false.some))

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertakingJourney))
            mockCreateUndertaking(undertakingCreated)(Right(undertakingRef))
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockSendEmail(eori1, CreateUndertaking, undertaking)(
              Left(ConnectorError(exception))
            )
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))

        }
      }

      "redirect to confirmation page" when {

        def testRedirection(): Unit = {

          val updatedUndertakingJourney = undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage(false.some))

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertakingJourney))
            mockCreateUndertaking(undertakingCreated)(Right(undertakingRef))
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockSendEmail(eori1, CreateUndertaking, undertaking)(
              Right(EmailSent)
            )
            mockTimeProviderNow(timeNow)
            mockSendAuditEvent(
              createUndertakingAuditEvent(undertaking1.getIndustrySectorLimit)
            )
          }
          checkIsRedirect(
            result = performAction("cya" -> "true"),
            expectedRedirectLocation = routes.UndertakingController.getConfirmation(undertakingRef).url
          )
        }

        "all api calls are successful" in {
          testRedirection()
        }

      }

    }

    "handling request to get confirmation" must {

      def performAction() = controller.getConfirmation(undertakingRef)(
        FakeRequest(GET, routes.UndertakingController.getConfirmation(undertakingRef).url)
      )

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.confirmation.title"),
          { doc =>
            val heading2 = doc.select(".govuk-body").text()
            heading2 should include regex messageFromMessageKey("undertaking.confirmation.p1")
            doc.text() should not include messageFromMessageKey(
              "undertaking.confirmation.p2",
              routes.BusinessEntityController.getAddBusinessEntity()
            )
          }
        )

      }

      "display the page with intent to add business" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockGet[UndertakingJourney](eori1)(
            Right(undertakingJourneyComplete.copy(addBusiness = UndertakingAddBusinessFormPage(true.some)).some)
          )
        }

        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.confirmation.title"),
          { doc =>
            val heading2 = doc.select(".govuk-body").text()
            heading2 should include regex messageFromMessageKey("undertaking.confirmation.p1")

            val confirmationP2 = doc.getElementById("confirmation-p2")
            confirmationP2.text should startWith("You can now add businesses to your undertaking using the ")
            confirmationP2.text should endWith(" link in the ‘Undertaking administration’ section of the homepage.")
            val link = doc.getElementById("add-remove-business-link")
            link.text shouldBe "Add and remove businesses"
            link.attr("href") shouldBe routes.BusinessEntityController.startJourney().url

          }
        )

      }

    }

    "handling request to Post Confirmation page" must {

      def performAction(data: (String, String)*) =
        controller.postConfirmation(FakeRequest(POST, "/").withFormUrlEncodedBody(data: _*))

      "throw technical error" when {

        "confirmation form is empty" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update undertaking confirmation fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("confirm" -> "true")))
        }
      }

      "redirect to next page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))
        }

        checkIsRedirect(performAction("confirm" -> "true"), routes.AccountController.getAccountPage.url)
      }

    }

    "handling request to get Amend Undertaking Details" must {

      def performAction() = controller.getAmendUndertakingDetails(FakeRequest())

      val expectedRows = List(
        ModifyUndertakingRow(
          messageFromMessageKey("undertaking.amendUndertaking.summary-list.sector.key"),
          messageFromMessageKey(s"sector.label.${undertaking.industrySector.id.toString}"),
          routes.UndertakingController.getSector.url
        ),
        ModifyUndertakingRow(
          messageFromMessageKey("undertaking.amendUndertaking.summary-list.undertaking-admin-email.key"),
          "foo@example.com",
          routes.UndertakingController.getAddEmailForVerification(Amend).url
        )
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        "call to get undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

        "call to update the undertaking journey fails" in {

          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "display the page" when {

        "is Amend is true" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.copy(isAmend = true).some))
            mockGetEmailVerification()
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertaking.amendUndertaking.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url

              val rows =
                doc.select(".govuk-summary-list__row").asScala.toList.map { element =>
                  val question = element.select(".govuk-summary-list__key").text()
                  val answer = element.select(".govuk-summary-list__value").text()
                  val changeUrl = element.select(".govuk-link").attr("href")
                  ModifyUndertakingRow(question, answer, changeUrl)
                }
              rows shouldBe expectedRows
            }
          )
        }

        "is Amend flag is false" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.copy(isAmend = true)))
            mockGetEmailVerification()
          }

          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertaking.amendUndertaking.title"),
            { doc =>
              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url

              val rows =
                doc.select(".govuk-summary-list__row").asScala.toList.map { element =>
                  val question = element.select(".govuk-summary-list__key").text()
                  val answer = element.select(".govuk-summary-list__value").text()
                  val changeUrl = element.select(".govuk-link").attr("href")
                  ModifyUndertakingRow(question, answer, changeUrl)
                }
              rows shouldBe expectedRows
            }
          )
        }

      }

      "redirect to journey start page" when {

        "call to get undertaking journey fetches nothing" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }

      }

    }

    "handling request to post Amend undertaking" must {

      def performAction(data: (String, String)*) = controller
        .postAmendUndertaking(
          FakeRequest(POST, routes.UndertakingController.getAmendUndertakingDetails.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to update undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to update undertaking journey passes but return undertaking with no name" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage()))
            )
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to update undertaking journey passes but return undertaking with no secctor" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage()))
            )
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))

        }

        "call to retrieve undertaking fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage("true".some)))
            )
            mockRetrieveUndertaking(eori1)(Future.failed(exception))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to retrieve undertaking passes but no undertaking was fetched" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage("true".some)))
            )
            mockRetrieveUndertaking(eori1)(None.toFuture)
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

        "call to update undertaking fails" in {
          val updatedUndertaking =
            undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockUpdate[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(isAmend = true))
            )
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockUpdateUndertaking(updatedUndertaking)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        }

      }

      "redirect to next page" in {
        val updatedUndertaking =
          undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockUpdate[UndertakingJourney](eori1)(
            Right(undertakingJourneyComplete.copy(isAmend = true))
          )
          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
          mockUpdateUndertaking(updatedUndertaking)(Right(undertakingRef))
          mockSendAuditEvent(
            UndertakingUpdated("1123", eori1, undertakingRef, undertaking1.name, undertaking1.industrySector)
          )
        }
        checkIsRedirect(performAction("amendUndertaking" -> "true"), routes.AccountController.getAccountPage.url)
      }

    }

    "handling request to get Disable undertaking warning" must {
      def performAction() = controller.getDisableUndertakingWarning(FakeRequest())

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("disableUndertakingWarning.title"),
          doc => doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url
        )
      }

    }

    "handling request to get Disable undertaking confirm" must {
      def performAction() = controller.getDisableUndertakingConfirm(FakeRequest())

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("disableUndertakingConfirm.title"),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.UndertakingController.getDisableUndertakingWarning.url
            val form = doc.select("form")
            form
              .attr("action") shouldBe routes.UndertakingController.postDisableUndertakingConfirm.url
            val hintText: String = doc.getElementById("disableUndertakingConfirm-hint").text()
            hintText shouldBe "This cannot be reversed"
          }
        )

      }

    }

    "handling request to post Disable undertaking confirm" must {
      def performAction(data: (String, String)*) = controller
        .postDisableUndertakingConfirm(
          FakeRequest(POST, routes.UndertakingController.getDisableUndertakingConfirm.url)
            .withFormUrlEncodedBody(data: _*)
        )

      val currentDate = LocalDate.of(2022, 10, 9)
      val formattedDate = "9 October 2022"

      "throw technical error" when {
        "call to remove disable fails" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockDisableUndertaking(undertaking1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("disableUndertakingConfirm" -> "true")))
        }
      }

      "display the error" when {

        "Nothing is submitted" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkFormErrorIsDisplayed(
            performAction(),
            messageFromMessageKey("disableUndertakingConfirm.title"),
            messageFromMessageKey("disableUndertakingConfirm.error.required")
          )

        }
      }

      "redirect to next page" when {

        "user selected Yes" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
            mockDisableUndertaking(undertaking1)(Right(undertakingRef))
            mockDeleteAll(eori1)(Right(()))
            mockDeleteAll(eori4)(Right(()))
            mockTimeProviderToday(currentDate)
            mockSendAuditEvent[UndertakingDisabled](
              UndertakingDisabled("1123", undertakingRef, currentDate, undertaking1)
            )
            mockTimeProviderToday(currentDate)
            mockSendEmail(eori1, DisableUndertakingToLead, undertaking1, formattedDate)(Right(EmailSent))
            mockSendEmail(eori1, DisableUndertakingToBusinessEntity, undertaking1, formattedDate)(Right(EmailSent))
          }
          checkIsRedirect(
            performAction("disableUndertakingConfirm" -> "true"),
            routes.UndertakingController.getUndertakingDisabled.url
          )
        }

        "user selected No" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail()
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          }
          checkIsRedirect(
            performAction("disableUndertakingConfirm" -> "false"),
            routes.AccountController.getAccountPage.url
          )
        }
      }

    }

    "handling request to get Undertaking Disabled" must {
      def performAction() = controller.getUndertakingDisabled(FakeRequest())

      "display the page" in {
        inSequence {
          mockAuthWithEnrolmentAndValidEmail()
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertakingDisabled.title")
        )
      }
    }

    "handling request to get add email for verification" must {

      def performAction(status: EmailStatus = EmailStatus.New) =
        controller.getAddEmailForVerification(status)(FakeRequest(GET, "/some-url"))

      "display the page" when {

        "status is 'new' (user is in registration journey)" in {

          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe routes.UndertakingController.getSector.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postAddEmailForVerification(EmailStatus.New).url
            }
          )

        }

        "status is 'unverified' (user has no verified email address)" in {
          val status = EmailStatus.Unverified
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc.select(".govuk-back-link").attr("href") shouldBe routes.UnverifiedEmailController.unverifiedEmail.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postAddEmailForVerification(status).url
            }
          )

        }
      }

    }

    "handling request to post add email for verification" must {

      def performAction(status: EmailStatus, data: (String, String)*) =
        controller.postAddEmailForVerification(status = status)(
          FakeRequest(POST, "/some-url").withFormUrlEncodedBody(data: _*)
        )

      "redirect to home page when status is EmailStatus.Unverified" in {
        val redirectUrl = routes.AccountController.getAccountPage.url
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockMakeVerificationRequestAndRedirect(Redirect(redirectUrl).toFuture)
        }
        checkIsRedirect(
          performAction(status = EmailStatus.Unverified, data = ("email" -> "foo@example.com")),
          redirectUrl
        )
      }

      "redirect to add business page when status is EmailStatus.New" in {
        val redirectUrl = routes.UndertakingController.getAddBusiness.url
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockMakeVerificationRequestAndRedirect(Redirect(redirectUrl).toFuture)
        }
        checkIsRedirect(performAction(status = EmailStatus.New, data = ("email" -> "foo@example.com")), redirectUrl)
      }

      "return bad request and show error" when {
        val pageTitle = "What is your email address?"
        val errorMessage = "Enter a valid email address"

        "invalid email format - no domain" in {
          inSequence {
            mockAuthWithEnrolment(eori1)
          }
          checkFormErrorIsDisplayed(
            result = performAction(status = EmailStatus.New, data = "email" -> "some@thing"),
            expectedTitle = pageTitle,
            formError = errorMessage
          )
        }

        "invalid email format - has spaces" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolment(eori1)
          }
          checkFormErrorIsDisplayed(
            result = performAction(status = EmailStatus.Unverified, data = "email" -> "some @ thing.com"),
            expectedTitle = pageTitle,
            formError = errorMessage
          )
        }

        "invalid email format - no @" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolment(eori1)
          }
          checkFormErrorIsDisplayed(
            result = performAction(status = EmailStatus.New, data = "email" -> "something.com"),
            expectedTitle = pageTitle,
            formError = errorMessage
          )
        }

      }

    }

  }
}

object UndertakingControllerSpec {
  final case class ModifyUndertakingRow(question: String, answer: String, changeUrl: String)

  final case class SectorRadioOption(sector: String, label: String, hint: String)
}
