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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse, VerificationStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.{Amend, EmailStatus}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EmailStatus, Sector, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress}
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
  val verificationUrlNew = routes.UndertakingController.getAddEmailForVerification(EmailStatus.New).url
  val verificationUrlAmend = routes.UndertakingController.getAddEmailForVerification(EmailStatus.Amend).url
  val exception = new Exception("oh no")

  "UndertakingController" when {

    "handling request to first empty page" must {

      def performAction() =
        controller.firstEmptyPage(FakeRequest())

      "throw technical error" when {

        "call to Get Or Create undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "redirects to next page" when {

        "undertaking journey is present and  is not None and is complete" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))
          }
          checkIsRedirect(performAction(), routes.AddBusinessEntityController.getAddBusinessEntity().url)
        }

        "undertaking journey is present and  is not None and is not complete" when {

          def testRedirect(undertakingJourney: UndertakingJourney, redirectTo: String): Unit = {
            inSequence {
              mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
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
                hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
              ),
              routes.UndertakingController.getAddBusiness.url
            )
          }

          "undertaking journey contains undertaking name, sector, verified email and add business" in {
            testRedirect(
              UndertakingJourney(
                about = AboutUndertakingFormPage("TestUndertaking".some),
                sector = UndertakingSectorFormPage(Sector(1).some),
                hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
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
                hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
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
                hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
                addBusiness = UndertakingAddBusinessFormPage(false.some),
                confirmation = UndertakingConfirmationFormPage(true.some)
              ),
              routes.AddBusinessEntityController.getAddBusinessEntity().url
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourney))
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourney))
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            val emptyJourney = UndertakingJourney()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(emptyJourney))
            mockUpdate[UndertakingJourney](eori1)(Right(emptyJourney))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingName.title"),
            { doc =>
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.EligibilityEoriCheckController.getEoriCheck.url
              val input = doc.select(".govuk-input").attr("value")
              input shouldBe ""

              val button = doc.select("form")
              button.attr("action") shouldBe routes.UndertakingController.postAboutUndertaking.url
            }
          )
        }

        "undertaking journey is there in store and user has already answered the questions and all answers are complete" in {
          testDisplay(undertakingJourneyComplete, routes.UndertakingController.getCheckAnswers.url)
        }

        "undertaking journey is there in store and user hasn't  answered any questions" in {
          testDisplay(UndertakingJourney(), routes.EligibilityEoriCheckController.getEoriCheck.url)
        }

        "undertaking journey is there in store and user has answered the question but journey is not complete" in {
          testDisplay(
            UndertakingJourney(
              about = AboutUndertakingFormPage("TestUndertaking".some)
            ),
            routes.EligibilityEoriCheckController.getEoriCheck.url
          )
        }

        "page appeared via amend undertaking journey" in {
          testDisplay(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("undertakingName" -> "TestUndertaking123")))
        }

        "call to  get undertaking journey passes but com back with empty response" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          assertThrows[Exception](await(performAction("undertakingName" -> "TestUndertaking123")))
        }

        "call to update undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("continue" -> "true")))
        }

        "submitted form does not contain expected data" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          assertThrows[IllegalStateException](await(performAction("this is not" -> "valid")))
        }
      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String): Unit = {
          val updatedUndertaking = undertakingJourney.copy(about = AboutUndertakingFormPage("TestUndertaking123".some))
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.copy(cya = UndertakingCyaFormPage()).some))
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

        //        "page is reached via normal undertaking creation process when all answers have been provided" in {
        //          test(undertakingJourneyComplete, routes.UndertakingController.getCheckAnswers.url)
        //        }

      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }

    "handling request to get sector" must {

      def performAction() = controller.getSector(FakeRequest(GET, routes.UndertakingController.getSector.url))

      "throw technical error" when {

        val exception = new Exception("oh no")
        //        "call to fetch undertaking journey fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndNoEmailVerification()
        //            mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction()))
        //        }

      }

      "display the page" when {

        val allRadioTexts: List[SectorRadioOption] = List(
          SectorRadioOption(
            "agriculture",
            "Primary production of agricultural products",
            "An 'agricultural product' can be products such as: live animals, meat and edible meat offal, dairy produce, birds' eggs, natural honey. The cap for this sector is €50,000."
          ),
          SectorRadioOption(
            "aquaculture",
            "Fishery and aquaculture products",
            "If any part of your business is involved in the production, processing or marketing of fishery and aquaculture products. A 'fishery product' means aquatic organisms resulting from any fishing activity or products derived from them. The cap for this sector is €30,000."
          ),
          SectorRadioOption(
            "transport",
            "Road freight transport for hire or reward",
            "This sector includes couriers and hauliers who get paid by someone to transport their goods by road. The cap for this sector is €300,000."
          ),
          SectorRadioOption("other", "Other", "The cap for all other sectors is €300,000.")
        )

        def test(undertakingJourney: UndertakingJourney, previousCall: String, inputValue: Option[String]): Unit = {

          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(undertakingJourney))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          checkPageIsDisplayed(
            performAction(),
            messageFromMessageKey("undertakingSector.title"),
            { doc =>
              val back = doc.select(".govuk-back-link")
              if (!back.isEmpty) {
                back.attr("href") shouldBe previousCall
              }

              val selectedOptions = doc.select(".govuk-radios__input[checked]")
              inputValue match {
                case Some(value) => selectedOptions.attr("value") shouldBe value
                case None => selectedOptions.isEmpty shouldBe true
              }

              testRadioButtonOptions(doc, allRadioTexts)
              doc.select("form").attr("action") shouldBe routes.UndertakingController.postSector.url
            }
          )

        }

        //        "user has not already answered the question (normal add undertaking journey)" in {
        //          test(
        //            undertakingJourney = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking1".some)),
        //            previousCall = routes.UndertakingController.getAboutUndertaking.url,
        //            inputValue = None
        //          )
        //        }

        //        "user has already answered the question (normal add undertaking journey)" in {
        //          test(
        //            undertakingJourney = UndertakingJourney(
        //              about = AboutUndertakingFormPage("TestUndertaking1".some),
        //              sector = UndertakingSectorFormPage(Sector(2).some)
        //            ),
        //            previousCall = routes.UndertakingController.getAboutUndertaking.url,
        //            inputValue = "2".some
        //          )
        //        }

        //        "user has already answered the question and is on Amend journey" in {
        //          test(
        //            undertakingJourney = undertakingJourneyComplete1,
        //            previousCall = routes.UndertakingController.getAmendUndertakingDetails.url,
        //            inputValue = "2".some
        //          )
        //        }

        //        "legend is displayed in fieldset component" in {
        //          val undertakingJourney = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking1".some))
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
        //          }
        //
        //          val result = performAction()
        //          status(result) shouldBe OK
        //          val document = Jsoup.parse(contentAsString(result))
        //
        //          val legendText: String =
        //            document
        //              .getElementsByClass("govuk-fieldset__legend--xl")
        //              .text()
        //          legendText shouldBe "What is the industry sector of your undertaking?"
        //
        //          val hintText: String =
        //            document
        //              .getElementById("undertakingSector-hint")
        //              .text()
        //          hintText shouldBe "Your undertaking may have businesses working in different sectors. For your whole undertaking the lowest subsidy allowance will apply."
        //        }

      }

      "redirect to previous question" when {

        "about has not been answered" in {
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
            mockGetOrCreate[UndertakingJourney](eori1)(Right(UndertakingJourney()))
            mockGet[UndertakingJourney](eori1)(Right(UndertakingJourney().some))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        //        "cya is true" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
        //          }
        //          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        //        }
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

        //        "call to get previous url fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        //        }

        //        "call to fetch undertaking journey fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction()))
        //        }

        //        "call to fetch undertaking journey passes  buy fetches nothing" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Right(None))
        //          }
        //          assertThrows[Exception](await(performAction()))
        //        }

        //        "call to update undertaking journey fails" in {
        //          val currentUndertaking = UndertakingJourney(about = AboutUndertakingFormPage("TestUndertaking".some))
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Right(currentUndertaking.some))
        //            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("undertakingSector" -> "2")))
        //        }

      }

      "display form error" when {

        //        "nothing is submitted" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(
        //              Right(undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage()).some)
        //            )
        //          }
        //          checkFormErrorIsDisplayed(
        //            performAction(),
        //            messageFromMessageKey("undertakingSector.title"),
        //            messageFromMessageKey("undertakingSector.error.required")
        //          )
        //
        //        }

      }

      "redirect to next page" when {

        def test(undertakingJourney: UndertakingJourney, nextCall: String): Unit = {

          val newSector = UndertakingSectorFormPage(Sector(3).some)

          val updatedUndertaking = undertakingJourney.copy(sector = newSector)
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertaking))
          }
          checkIsRedirect(performAction("undertakingSector" -> "3"), nextCall)
        }

        //        "page is reached via amend details page " in {
        //          test(undertakingJourneyComplete1, routes.UndertakingController.getAmendUndertakingDetails.url)
        //
        //        }

        //        "page is reached via normal undertaking creation process" in {
        //          test(UndertakingJourney(), routes.UndertakingController.getConfirmEmail.url)
        //        }

        //        "page is reached via normal undertaking creation process when all answers have been provided" in {
        //          test(undertakingJourneyComplete, routes.UndertakingController.getCheckAnswers.url)
        //        }

      }

      "redirect to undertaking already submitted page" when {
        //        "cya is true" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
        //          }
        //          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        //        }
      }

    }

    "handling request to get verify email" must {

      def performAction(pendingVerificationId: String) = controller.getVerifyEmail(pendingVerificationId)(
        FakeRequest(GET, routes.UndertakingController.getVerifyEmail(pendingVerificationId).url)
      )

      "throw technical error" when {

        val exception = new Exception("oh no")
        //        "call to fetch undertaking journey fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndNoEmailVerification()
        //            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("abcdefh")))
        //        }

      }

      "display the page" when {

        "User has verified email in CDS" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val email = "joebloggs@something.com"

          inSequence {
            mockAuthWithEnrolment(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockGetEmailVerificationStatus(
              Future.successful(Some(VerificationStatus(emailAddress = email, verified = true, locked = false)))
            )
            mockUpdateEmailForEori(eori1, email)(Future.successful(()))
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "display the page" when {

        //        "User has verified email in CDS" in {
        //          val undertakingJourney = UndertakingJourney(
        //            about = AboutUndertakingFormPage("TestUndertaking1".some),
        //            sector = UndertakingSectorFormPage(Sector(2).some)
        //          )
        //          val previousCall = routes.UndertakingController.getSector.url
        //
        //          val email = "email@test.com"
        //          val pageTitle = s"Is $email the right email address to receive notifications?"
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
        //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
        //            mockRetrieveEmail(eori1)(
        //              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
        //            )
        //          }
        //          checkPageIsDisplayed(
        //            performAction(),
        //            pageTitle,
        //            { doc =>
        //              val heading = doc.getElementsByClass("govuk-fieldset__heading")
        //              heading.size shouldBe 1
        //              heading.text shouldBe pageTitle
        //              doc.select(".govuk-back-link").attr("href") shouldBe previousCall
        //
        //              doc
        //                .getElementsByTag("legend")
        //                .hasClass(
        //                  "govuk-fieldset__legend govuk-fieldset__legend--xl govuk-!-display-block break-word"
        //                ) shouldBe true
        //
        //              val form = doc.select("form")
        //              form
        //                .attr("action") shouldBe routes.UndertakingController.postConfirmEmail.url
        //            }
        //          )
        //
        //        }

        //        "User does not have verified email in CDS" in {
        //          val undertakingJourney = UndertakingJourney(
        //            about = AboutUndertakingFormPage("TestUndertaking1".some),
        //            sector = UndertakingSectorFormPage(Sector(2).some)
        //          )
        //          val previousCall = routes.UndertakingController.getSector.url
        //          val email = "email@t.com"
        //
        //          inSequence {
        //            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
        //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
        //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
        //            mockRetrieveEmail(eori1)(
        //              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
        //            )
        //          }
        //          checkPageIsDisplayed(
        //            performAction(),
        //            "Is email@t.com the right email address to receive notifications?",
        //            { doc =>
        //              doc.select(".govuk-back-link").attr("href") shouldBe previousCall
        //
        //              val form = doc.select("form")
        //              form
        //                .attr("action") shouldBe routes.UndertakingController.postConfirmEmail.url
        //            }
        //          )
        //
        //        }
      }

      "redirect to about undertaking page" when {

        "call to fetch undertaking journey returns no undertaking journey" in {
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        //        "cya is true" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
        //          }
        //          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        //        }
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
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
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney()
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

      "redirect to add business page" when {

        "all api calls are successful" in {
          val email = "foo@example.com"
          inSequence {
            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyWithCyaNotVisited.some))
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress(email).some))
            )
            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))

          }
          checkIsRedirect(
            performAction("using-stored-email" -> "true"),
            routes.UndertakingController.getAddBusiness.url
          )
        }

        "No verification found or cds with valid form should redirect" in {
          inSequence {

            mockAuthWithEnrolmentAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(
              Right(undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage()).some)
            )
            mockRetrieveEmail(eori1)(
              Right(RetrieveEmailResponse(EmailType.VerifiedEmail, EmailAddress("email").some))
            )
          }
          val result = performAction(
            "using-stored-email" -> "false"
          )

          status(result) shouldBe SEE_OTHER
          redirectLocation(result) should contain(verificationUrlNew)
        }

      }

      "redirect to undertaking already submitted page" when {
        //        "cya is true" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndSubmittedUndertakingJourney(eori1)
        //          }
        //          checkIsRedirect(
        //            performAction("using-stored-email" -> "false", "email" -> "somethingl.com"),
        //            routes.RegistrationSubmittedController.registrationAlreadySubmitted.url
        //          )
        //        }
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourney.some))
          }
          val document = Jsoup.parse(contentAsString(performAction()))
          document.select(".govuk-back-link").attr("href") shouldBe previousCall
          document.getElementById("intentId").text() shouldBe messageFromMessageKey("addBusinessIntent.p1")
        }

        "when question has not been answered" in {
          val undertakingJourney = UndertakingJourney(
            about = AboutUndertakingFormPage("TestUndertaking1".some),
            sector = UndertakingSectorFormPage(Sector(2).some),
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some))
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            hasVerifiedEmail = Some(UndertakingConfirmEmailFormPage(true.some)),
            addBusiness = UndertakingAddBusinessFormPage(true.some)
          )
          val previousCall = routes.UndertakingController.getConfirmEmail.url

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney(eori1)
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to previous step" when {
        "email question has not been answered" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney(eori1)
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

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }

          assertThrows[Exception](await(performAction("addBusinessIntent" -> "true")))
        }

      }

      "show a form error" when {

        def displayErrorTest(data: (String, String)*)(errorMessage: String): Unit = {

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

    }

    "handling request to get check your answers page" must {

      def performAction() = controller.getCheckAnswers(
        FakeRequest(GET, routes.UndertakingController.getCheckAnswers.url)
      )

      //      "display the page" in {
      //
      //        val expectedRows = List(
      //          ModifyUndertakingRow(
      //            messageFromMessageKey("undertaking.cya.summary-list.eori.key"),
      //            eori1,
      //            "" // User cannot change the EORI on the undertaking
      //          ),
      //          ModifyUndertakingRow(
      //            messageFromMessageKey("undertaking.cya.summary-list.sector.key"),
      //            messageFromMessageKey(s"sector.label.${undertaking.industrySector.id.toString}"),
      //            routes.UndertakingController.getSector.url
      //          ),
      //          ModifyUndertakingRow(
      //            messageFromMessageKey("undertaking.cya.summary-list.verified-email"),
      //            "joebloggs@something.com",
      //            routes.UndertakingController.getAddEmailForVerification(EmailStatus.CYA).url
      //          ),
      //          ModifyUndertakingRow(
      //            messageFromMessageKey("undertaking.cya.summary-list.other-business"),
      //            "No",
      //            routes.UndertakingController.getAddBusiness.url
      //          )
      //        )
      //        inSequence {
      //          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
      //          mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
      //          mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("joebloggs@something.com"))
      //          mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete))
      //        }
      //
      //        checkPageIsDisplayed(
      //          performAction(),
      //          messageFromMessageKey("undertaking.cya.title"),
      //          { doc =>
      //            doc
      //              .select(".govuk-back-link")
      //              .attr("href") shouldBe routes.UndertakingController.backFromCheckYourAnswers.url
      //            val rows =
      //              doc.select(".govuk-summary-list__row").iterator().asScala.toList.map { element =>
      //                val question = element.select(".govuk-summary-list__key").text()
      //                val answer = element.select(".govuk-summary-list__value").text()
      //                val changeUrl = element.select(".govuk-link").attr("href")
      //                ModifyUndertakingRow(question, answer, changeUrl)
      //              }
      //
      //            rows shouldBe expectedRows
      //          }
      //        )
      //
      //      }

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }

      }

      "redirect" when {
        "to journey start when call to get undertaking journey fetches nothing" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
          }
          assertThrows[Exception](await(performAction()))
        }

        //        "call to update undertaking journey fails" in {
        //
        //          inSequence {
        //            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
        //            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("cya" -> "true")))
        //        }

        "updated undertaking journey don't have undertaking name" in {

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "updated undertaking journey don't have undertaking sector" in {

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))
        }

        "call to create undertaking fails" in {

          val updatedUndertakingJourney = undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage(false.some))

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertakingJourney))
            mockCreateUndertaking(undertakingCreated)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction("cya" -> "true")))

        }

        "call to send email fails" in {

          val updatedUndertakingJourney =
            undertakingJourneyComplete.copy(cya = UndertakingCyaFormPage(false.some))

          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
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
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockUpdate[UndertakingJourney](eori1)(Right(updatedUndertakingJourney))
            mockCreateUndertaking(undertakingCreated)(Right(undertakingRef))
            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
            mockSendEmail(eori1, CreateUndertaking, undertaking)(
              Right(EmailSent)
            )
            mockTimeProviderNow(timeNow)
            mockSendAuditEvent(
              createUndertakingAuditEvent(undertaking1.industrySectorLimit)
            )
            mockUpdate[UndertakingJourney](eori1)(Right(submittedUndertakingJourney))
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

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
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
          messageFromMessageKey("undertaking.confirmation.title")
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
          messageFromMessageKey("undertaking.confirmation.title")
        )

      }

      "show confirmation page even if undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentAndValidEmail(eori1)
            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
          }

          checkPageIsDisplayed(
            performAction(),
            "Undertaking registered"
          )
        }
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

    //    "handling request to get Amend Undertaking Details" must {
    //
    //      def performAction() = controller.getAmendUndertakingDetails(FakeRequest())
    //
    //      val expectedRows = List(
    //        ModifyUndertakingRow(
    //          messageFromMessageKey("undertaking.amendUndertaking.summary-list.sector.key"),
    //          messageFromMessageKey(s"sector.label.${undertaking.industrySector.id.toString}"),
    //          routes.UndertakingController.getSectorForUpdate.url
    //        ),
    //        ModifyUndertakingRow(
    //          messageFromMessageKey("undertaking.amendUndertaking.summary-list.undertaking-admin-email.key"),
    //          "foo@example.com",
    //          routes.UndertakingController.getAddEmailForVerification(Amend).url
    //        )
    //      )
    //
    //      "throw technical error" when {
    //
    //        val exception = new Exception("oh no")
    //        "call to get undertaking journey fails" in {
    //          inSequence {
    //            mockAuthWithEnrolmentAndValidEmail()
    //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
    //            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
    //          }
    //          assertThrows[Exception](await(performAction()))
    //        }
    //
    //        "call to update the undertaking journey fails" in {
    //
    //          inSequence {
    //            mockAuthWithEnrolmentAndValidEmail()
    //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
    //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
    //            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
    //          }
    //          assertThrows[Exception](await(performAction()))
    //        }
    //      }
    //
    //      "display the page" when {
    //
    //        "is Amend is true" in {
    //          inSequence {
    //            mockAuthWithEnrolmentAndValidEmail()
    //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
    //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.copy(isAmend = true).some))
    //            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("foo@example.com"))
    //          }
    //
    //          checkPageIsDisplayed(
    //            performAction(),
    //            messageFromMessageKey("undertaking.amendUndertaking.title"),
    //            { doc =>
    //              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url
    //              val rows =
    //                doc.select(".govuk-summary-list__row").asScala.toList.map { element =>
    //                  val question = element.select(".govuk-summary-list__key").text()
    //                  val answer = element.select(".govuk-summary-list__value").text()
    //                  val changeUrl = element.select(".govuk-link").attr("href")
    //                  ModifyUndertakingRow(question, answer, changeUrl)
    //                }
    //              rows shouldBe expectedRows
    //            }
    //          )
    //        }
    //
    //        "is Amend flag is false" in {
    //          inSequence {
    //            mockAuthWithEnrolmentAndValidEmail()
    //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
    //            mockGet[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.some))
    //            mockUpdate[UndertakingJourney](eori1)(Right(undertakingJourneyComplete.copy(isAmend = true)))
    //            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("foo@example.com"))
    //          }
    //
    //          checkPageIsDisplayed(
    //            performAction(),
    //            messageFromMessageKey("undertaking.amendUndertaking.title"),
    //            { doc =>
    //              doc.select(".govuk-back-link").attr("href") shouldBe routes.AccountController.getAccountPage.url
    //
    //              val rows =
    //                doc.select(".govuk-summary-list__row").asScala.toList.map { element =>
    //                  val question = element.select(".govuk-summary-list__key").text()
    //                  val answer = element.select(".govuk-summary-list__value").text()
    //                  val changeUrl = element.select(".govuk-link").attr("href")
    //                  ModifyUndertakingRow(question, answer, changeUrl)
    //                }
    //              rows shouldBe expectedRows
    //            }
    //          )
    //        }
    //
    //      }
    //
    //      "redirect to journey start page" when {
    //
    //        "call to get undertaking journey fetches nothing" in {
    //          inSequence {
    //            mockAuthWithEnrolmentAndValidEmail()
    //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
    //            mockGet[UndertakingJourney](eori1)(Right(None))
    //          }
    //          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
    //        }
    //
    //      }
    //
    //    }

    "handling request to post Amend undertaking" must {

      def performAction(data: (String, String)*) = controller
        .postAmendUndertaking(
          FakeRequest(POST, routes.UndertakingController.getAmendUndertakingDetails.url)
            .withFormUrlEncodedBody(data: _*)
        )

      "throw technical error" when {
        val exception = new Exception("oh no")

        //        "call to update undertaking journey fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndValidEmail()
        //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        //            mockUpdate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        //
        //        }

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

        //        "call to retrieve undertaking fails" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndValidEmail()
        //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        //            mockUpdate[UndertakingJourney](eori1)(
        //              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage("true".some)))
        //            )
        //            mockRetrieveUndertaking(eori1)(Future.failed(exception))
        //          }
        //          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        //        }

        //        "call to retrieve undertaking passes but no undertaking was fetched" in {
        //          inSequence {
        //            mockAuthWithEnrolmentAndValidEmail()
        //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        //            mockUpdate[UndertakingJourney](eori1)(
        //              Right(undertakingJourneyComplete.copy(about = AboutUndertakingFormPage("true".some)))
        //            )
        //            mockRetrieveUndertaking(eori1)(None.toFuture)
        //          }
        //          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        //        }

        //        "call to update undertaking fails" in {
        //          val updatedUndertaking =
        //            undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
        //          inSequence {
        //            mockAuthWithEnrolmentAndValidEmail()
        //            mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        //            mockUpdate[UndertakingJourney](eori1)(
        //              Right(undertakingJourneyComplete.copy(isAmend = true))
        //            )
        //            mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
        //            mockUpdateUndertaking(updatedUndertaking)(Left(ConnectorError(exception)))
        //          }
        //          assertThrows[Exception](await(performAction("amendUndertaking" -> "true")))
        //        }

      }

      //      "redirect to next page" in {
      //        val updatedUndertaking =
      //          undertaking1.copy(name = UndertakingName("TestUndertaking"), industrySector = Sector(1))
      //        inSequence {
      //          mockAuthWithEnrolmentAndValidEmail()
      //          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
      //          mockUpdate[UndertakingJourney](eori1)(
      //            Right(undertakingJourneyComplete.copy(isAmend = true))
      //          )
      //          mockRetrieveUndertaking(eori1)(undertaking1.some.toFuture)
      //          mockUpdateUndertaking(updatedUndertaking)(Right(undertakingRef))
      //          mockSendAuditEvent(
      //            UndertakingUpdated("1123", eori1, undertakingRef, undertaking1.name, undertaking1.industrySector)
      //          )
      //        }
      //        checkIsRedirect(performAction("amendUndertaking" -> "true"), routes.AccountController.getAccountPage.url)
      //      }

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
            hintText shouldBe "This cannot be reversed. If you deregister an undertaking by mistake, you can register it again as a new undertaking."
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
          messageFromMessageKey("undertakingDisabled.title"),
          { doc =>
            doc.getElementById("undertakingDisabledParaOne").text shouldBe "We have sent you a confirmation email."
            doc.getElementById("undertakingDisabledParaTwo").text shouldBe "You have been signed out."
            doc.getElementById("undertakingDisabledFeedbackHeader").text shouldBe "Before you go"
            doc
              .getElementById("undertakingDisabledParaThree")
              .text shouldBe "Your feedback helps us make our service better."
            doc
              .getElementById("undertakingDisabledParaFour")
              .text shouldBe "Take our survey to share your feedback on this service. It takes about 1 minute to complete."
          }
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

        "status is 'amend' (user clicks 'amend email')" in {
          val status = EmailStatus.Amend
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.UndertakingController.getAmendUndertakingDetails.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController.postAddEmailForVerification(EmailStatus.Amend).url
            }
          )

        }

        "status is 'BecomeLead' (user has come from 'BecomeLead' controller)" in {
          val status = EmailStatus.BecomeLead
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.BecomeLeadController.getConfirmEmail.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController
                .postAddEmailForVerification(EmailStatus.BecomeLead)
                .url
            }
          )

        }
        "status is 'CYA' (user has come from 'change' link in CYA page)" in {
          val status = EmailStatus.CYA
          val pageTitle = "What is your email address?"
          inSequence {
            mockAuthWithEnrolmentAndNoEmailVerification()
          }
          checkPageIsDisplayed(
            performAction(status),
            pageTitle,
            { doc =>
              doc.getElementById("inputEmail-h1").text shouldBe pageTitle
              doc
                .select(".govuk-back-link")
                .attr("href") shouldBe routes.UndertakingController.getCheckAnswers.url

              val form = doc.select("form")
              form
                .attr("action") shouldBe routes.UndertakingController
                .postAddEmailForVerification(EmailStatus.CYA)
                .url
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
    }

    "deriving level 1 code from level 2 code" must {

      "return A for agriculture codes" in {
        controller.deriveLevel1Code("01") shouldBe "A"
        controller.deriveLevel1Code("02") shouldBe "A"
        controller.deriveLevel1Code("03") shouldBe "A"
      }

      "return B for mining codes" in {
        controller.deriveLevel1Code("05") shouldBe "B"
        controller.deriveLevel1Code("06") shouldBe "B"
        controller.deriveLevel1Code("07") shouldBe "B"
        controller.deriveLevel1Code("08") shouldBe "B"
        controller.deriveLevel1Code("09") shouldBe "B"
      }

      "return C for manufacturing codes" in {
        controller.deriveLevel1Code("10") shouldBe "C"
        controller.deriveLevel1Code("11") shouldBe "C"
        controller.deriveLevel1Code("12") shouldBe "C"
        controller.deriveLevel1Code("13") shouldBe "C"
        controller.deriveLevel1Code("14") shouldBe "C"
        controller.deriveLevel1Code("15") shouldBe "C"
        controller.deriveLevel1Code("16") shouldBe "C"
        controller.deriveLevel1Code("17") shouldBe "C"
        controller.deriveLevel1Code("18") shouldBe "C"
        controller.deriveLevel1Code("19") shouldBe "C"
        controller.deriveLevel1Code("20") shouldBe "C"
        controller.deriveLevel1Code("21") shouldBe "C"
        controller.deriveLevel1Code("22") shouldBe "C"
        controller.deriveLevel1Code("23") shouldBe "C"
        controller.deriveLevel1Code("24") shouldBe "C"
        controller.deriveLevel1Code("25") shouldBe "C"
        controller.deriveLevel1Code("26") shouldBe "C"
        controller.deriveLevel1Code("27") shouldBe "C"
        controller.deriveLevel1Code("28") shouldBe "C"
        controller.deriveLevel1Code("29") shouldBe "C"
        controller.deriveLevel1Code("30") shouldBe "C"
        controller.deriveLevel1Code("31") shouldBe "C"
        controller.deriveLevel1Code("32") shouldBe "C"
        controller.deriveLevel1Code("33") shouldBe "C"
      }

      "return D for electricity codes" in {
        controller.deriveLevel1Code("35") shouldBe "D"
      }

      "return E for water supply codes" in {
        controller.deriveLevel1Code("36") shouldBe "E"
        controller.deriveLevel1Code("37") shouldBe "E"
        controller.deriveLevel1Code("38") shouldBe "E"
        controller.deriveLevel1Code("39") shouldBe "E"
      }

      "return F for construction codes" in {
        controller.deriveLevel1Code("41") shouldBe "F"
        controller.deriveLevel1Code("42") shouldBe "F"
        controller.deriveLevel1Code("43") shouldBe "F"
      }

      "return G for wholesale and retail codes" in {
        controller.deriveLevel1Code("46") shouldBe "G"
        controller.deriveLevel1Code("47") shouldBe "G"
      }

      "return H for transport codes" in {
        controller.deriveLevel1Code("49") shouldBe "H"
        controller.deriveLevel1Code("50") shouldBe "H"
        controller.deriveLevel1Code("51") shouldBe "H"
        controller.deriveLevel1Code("52") shouldBe "H"
        controller.deriveLevel1Code("53") shouldBe "H"
      }

      "return I for accommodation codes" in {
        controller.deriveLevel1Code("55") shouldBe "I"
        controller.deriveLevel1Code("56") shouldBe "I"
      }

      "return J for publishing codes" in {
        controller.deriveLevel1Code("58") shouldBe "J"
        controller.deriveLevel1Code("59") shouldBe "J"
        controller.deriveLevel1Code("60") shouldBe "J"
      }

      "return K for telecommunications codes" in {
        controller.deriveLevel1Code("61") shouldBe "K"
        controller.deriveLevel1Code("62") shouldBe "K"
        controller.deriveLevel1Code("63") shouldBe "K"
      }

      "return L for financial codes" in {
        controller.deriveLevel1Code("64") shouldBe "L"
        controller.deriveLevel1Code("65") shouldBe "L"
        controller.deriveLevel1Code("66") shouldBe "L"
      }

      "return M for real estate codes" in {
        controller.deriveLevel1Code("68") shouldBe "M"
      }

      "return N for professional codes" in {
        controller.deriveLevel1Code("69") shouldBe "N"
        controller.deriveLevel1Code("70") shouldBe "N"
        controller.deriveLevel1Code("71") shouldBe "N"
        controller.deriveLevel1Code("72") shouldBe "N"
        controller.deriveLevel1Code("73") shouldBe "N"
        controller.deriveLevel1Code("74") shouldBe "N"
        controller.deriveLevel1Code("75") shouldBe "N"
      }

      "return O for administrative codes" in {
        controller.deriveLevel1Code("77") shouldBe "O"
        controller.deriveLevel1Code("78") shouldBe "O"
        controller.deriveLevel1Code("79") shouldBe "O"
        controller.deriveLevel1Code("80") shouldBe "O"
        controller.deriveLevel1Code("81") shouldBe "O"
        controller.deriveLevel1Code("82") shouldBe "O"
      }

      "return P for public administration codes" in {
        controller.deriveLevel1Code("84") shouldBe "P"
      }

      "return Q for education codes" in {
        controller.deriveLevel1Code("85") shouldBe "Q"
      }

      "return R for health codes" in {
        controller.deriveLevel1Code("86") shouldBe "R"
        controller.deriveLevel1Code("87") shouldBe "R"
        controller.deriveLevel1Code("88") shouldBe "R"
      }

      "return S for arts codes" in {
        controller.deriveLevel1Code("90") shouldBe "S"
        controller.deriveLevel1Code("91") shouldBe "S"
        controller.deriveLevel1Code("92") shouldBe "S"
        controller.deriveLevel1Code("93") shouldBe "S"
      }

      "return T for other service codes" in {
        controller.deriveLevel1Code("94") shouldBe "T"
        controller.deriveLevel1Code("95") shouldBe "T"
        controller.deriveLevel1Code("96") shouldBe "T"
      }

      "return U for households codes" in {
        controller.deriveLevel1Code("97") shouldBe "U"
        controller.deriveLevel1Code("98") shouldBe "U"
      }

      "return V for extraterritorial codes" in {
        controller.deriveLevel1Code("99") shouldBe "V"
      }
    }
  }
  "building NACE view model" must {

    "correctly derive all NACE levels for a retail code (47.11)" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.naceLevel4Code shouldBe "47.11"
      viewModel.naceLevel3Code shouldBe "47.1"
      viewModel.naceLevel2Code shouldBe "47"
      viewModel.naceLevel1Code shouldBe "G"
      viewModel.sector shouldBe Sector.other
    }

    "set showLevel1 to true for non-agriculture sectors" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.showLevel1 shouldBe true
    }

    "set showLevel1_1 correctly for manufacturing sector (C)" in {
      val naceCode = "10.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.naceLevel1Code shouldBe "C"
      viewModel.showLevel1_1 shouldBe true
    }

    "set showLevel1_1 to false for manufacturing code 32" in {
      val naceCode = "32.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.naceLevel1Code shouldBe "C"
      viewModel.showLevel1_1 shouldBe false
    }

    "set showLevel2 to true for other codes" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.showLevel2 shouldBe true
    }

    "set showLevel3 to true when level3 code does not end with .0" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.naceLevel3Code shouldBe "47.1"
      viewModel.showLevel3 shouldBe true
    }

    "set showLevel4 to true when last digits don't match" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.showLevel4 shouldBe true
    }

    "generate correct change URLs" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))

      viewModel.changeSectorUrl shouldBe routes.UndertakingController.getSector.url
      viewModel.changeLevel1Url should not be empty
      viewModel.changeLevel2Url should not be empty
      viewModel.changeLevel3Url should not be empty
      viewModel.changeLevel4Url should not be empty
    }
  }

  "handling request to get Amend Undertaking Details" must {

    def performAction() = controller.getAmendUndertakingDetails(FakeRequest())

    "throw technical error" when {

      "call to getOrCreate undertaking journey fails" in {
        inSequence {
          mockAuthWithEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
        }
        assertThrows[Exception](await(performAction()))
      }
    }

    "redirect to sector page" when {

      "NACE code is empty" in {
        val journeyWithEmptySector = UndertakingJourney(
          sector = UndertakingSectorFormPage(None)
        )
        inSequence {
          mockAuthWithEnrolment()
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
          mockGetOrCreate[UndertakingJourney](eori1)(Right(journeyWithEmptySector))
          mockUpdate[UndertakingJourney](eori1)(Right(journeyWithEmptySector))
          mockUpdate[UndertakingJourney](eori1)(Right(journeyWithEmptySector))
          mockUpdate[UndertakingJourney](eori1)(Right(journeyWithEmptySector))
          mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
        }
        checkIsRedirect(performAction(), routes.UndertakingController.getSector.url)
      }
    }
  }

  "handling request to post Sector" must {

    def performAction(data: (String, String)*) = controller
      .postSector(
        FakeRequest(POST, routes.UndertakingController.postSector.url)
          .withFormUrlEncodedBody(data: _*)
      )

    "display form error" when {
      "nothing is submitted" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(None)
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
        }
        val result = performAction()
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to next page in NewRegChangeMode" when {
      "previous answer equals form value and isNaceCYA is true" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.generalTrade.some),
          isNaceCYA = true,
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.NACECheckDetailsController.getCheckDetails().url
        )
      }
    }

    "redirect to amend undertaking details" when {
      "previous answer equals form value and mode is AmendNaceMode" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.generalTrade.some),
          mode = appConfig.AmendNaceMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.UndertakingController.getAmendUndertakingDetails.url
        )
      }
    }

    "redirect to next page for same answer" when {
      "previous answer equals form value in normal mode" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.generalTrade.some),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
        )
      }
    }

    "update sector and redirect" when {
      "answer has changed from agriculture code 01" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.cropAnimalProduction.some),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
        )
      }

      "answer has changed from aquaculture code 03" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.fishingAndAquaculture.some),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
        )
      }

      "answer has changed from general trade to agriculture" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.generalTrade.some),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "01"),
          routes.AgricultureController.loadAgricultureLvl3Page().url
        )
      }

      "answer has changed from other to general trade" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(Sector.other.some),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
        )
      }

      "sector has no previous value" in {
        val journey = UndertakingJourney(
          about = AboutUndertakingFormPage("TestUndertaking".some),
          sector = UndertakingSectorFormPage(None),
          mode = appConfig.NewRegMode
        )
        inSequence {
          mockAuthWithEnrolment()
          mockGet[UndertakingJourney](eori1)(Right(journey.some))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
          mockUpdate[UndertakingJourney](eori1)(Right(journey))
        }
        checkIsRedirect(
          performAction("undertakingSector" -> "00"),
          routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
        )
      }
    }

    "handling request to get check your answers page" must {

      def performAction() = controller.getCheckAnswers(
        FakeRequest(GET, routes.UndertakingController.getCheckAnswers.url)
      )

      "throw technical error" when {
        val exception = new Exception("oh no")

        "call to get undertaking journey fails" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Left(ConnectorError(exception)))
          }
          assertThrows[Exception](await(performAction()))
        }
      }

      "redirect" when {
        "to journey start when call to get undertaking journey fetches nothing" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(None))
          }
          checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
        }
      }

      "redirect to undertaking already submitted page" when {
        "cya is true" in {
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndSubmittedUndertakingJourney(eori1)
          }
          checkIsRedirect(performAction(), routes.RegistrationSubmittedController.registrationAlreadySubmitted.url)
        }
      }

      "display the page with correct NACE level 1 code derivation" when {

        "sector code is 01 (agriculture - level 1 code A)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.cerealsLeguminousCrops.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
          val document = Jsoup.parse(contentAsString(result))
          document.select(".govuk-summary-list__key").text() should include("Industry sector")
        }

        "sector code is 47 (retail - level 1 code G)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
        }

        "sector code is 10 (manufacturing - level 1 code C)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.meatProcessing.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
        }
      }

      "display page with correct industry sector key" when {

        "sector code is 01 (agriculture)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.cerealsLeguminousCrops.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
        }

        "sector code is 03 (fishery and aquaculture)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.marineAquaculture.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
        }

        "sector code is other (general trade)" in {
          val journey = undertakingJourneyComplete.copy(
            sector = UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
          )
          inSequence {
            mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
            mockGet[UndertakingJourney](eori1)(Right(journey.some))
            mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
            mockUpdate[UndertakingJourney](eori1)(Right(journey))
          }
          val result = performAction()
          status(result) shouldBe OK
        }
      }
      "update CYA flag and display page" in {
        val journeyWithValidSector = undertakingJourneyComplete.copy(
          sector = UndertakingSectorFormPage(Sector.NonSpecialisedFoodRetail.some)
        )
        inSequence {
          mockAuthWithEnrolmentWithValidEmailAndUnsubmittedUndertakingJourney()
          mockGet[UndertakingJourney](eori1)(Right(journeyWithValidSector.some))
          mockRetrieveVerifiedEmailAddressByEORI(eori1)(Future.successful("test@example.com"))
          mockUpdate[UndertakingJourney](eori1)(Right(journeyWithValidSector))
        }
        checkPageIsDisplayed(
          performAction(),
          messageFromMessageKey("undertaking.cya.title"),
          { doc =>
            doc
              .select(".govuk-back-link")
              .attr("href") shouldBe routes.UndertakingController.backFromCheckYourAnswers.url
          }
        )
      }
    }
  }
  "generating level 2 change URLs" must {

    "return correct URL for manufacturing (C)" in {
      val naceCode = "10.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for construction (F)" in {
      val naceCode = "41"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for retail (G)" in {
      val naceCode = "47.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for accommodation (I)" in {
      val naceCode = "55.1"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for publishing (J)" in {
      val naceCode = "58.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for administrative (O)" in {
      val naceCode = "77.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for health (R)" in {
      val naceCode = "86.1"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for households (U)" in {
      val naceCode = "97"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for transport (H)" in {
      val naceCode = "49.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for mining (B)" in {
      val naceCode = "05.10"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for professional (N)" in {
      val naceCode = "69.1"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for water supply (E)" in {
      val naceCode = "36"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for telecommunications (K)" in {
      val naceCode = "61.1"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }

    "return correct URL for arts (S)" in {
      val naceCode = "90.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url should not be empty
    }
  }
  "testing getLevel1_1ChangeUrl for manufacturing subcategories" must {

    "return correct URL for clothes/textiles/homeware - textiles (13)" in {
      val naceCode = "13.10"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    }

    "return correct URL for clothes/textiles/homeware - leather (15)" in {
      val naceCode = "15.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    }

    "return correct URL for clothes/textiles/homeware - wood (16)" in {
      val naceCode = "16.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    }

    "return correct URL for clothes/textiles/homeware - rubber/plastic (22)" in {
      val naceCode = "22.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    }

    "return correct URL for clothes/textiles/homeware - furniture (31)" in {
      val naceCode = "31"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    }

    "return correct URL for computers/electronics/machinery - computers (26)" in {
      val naceCode = "26.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage().url
    }

    "return correct URL for computers/electronics/machinery - electrical (27)" in {
      val naceCode = "27.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage().url
    }

    "return correct URL for computers/electronics/machinery - other machinery (28)" in {
      val naceCode = "28.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage().url
    }

    "return correct URL for food/beverages/tobacco - food (10)" in {
      val naceCode = "10.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url
    }

    "return correct URL for food/beverages/tobacco - tobacco (12)" in {
      val naceCode = "12"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url
    }

    "return correct URL for metals/chemicals/materials - chemicals (20)" in {
      val naceCode = "20.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
    }

    "return correct URL for metals/chemicals/materials - non-metallic (23)" in {
      val naceCode = "23.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
    }

    "return correct URL for metals/chemicals/materials - fabricated metal (25)" in {
      val naceCode = "25.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
    }

    "return correct URL for paper/printed - paper (17)" in {
      val naceCode = "17.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url
    }

    "return correct URL for paper/printed - printing (18)" in {
      val naceCode = "18.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url
    }

    "return correct URL for vehicles/transport - other transport (30)" in {
      val naceCode = "30.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadVehiclesTransportPage().url
    }

    "return default URL for other manufacturing (32)" in {
      val naceCode = "32.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
    }

    "return default URL for repair/maintenance (33)" in {
      val naceCode = "33.11"
      val viewModel = controller.buildViewModel(naceCode)(messagesApi.preferred(Seq.empty))
      viewModel.changeLevel2Url shouldBe routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
    }
  }

  "handling request to get sector in UpdateNaceMode" must {

    def performAction() = controller.getSector(FakeRequest(GET, routes.UndertakingController.getSector.url))

    "transform sector code '01' to display value '01' in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.cropAnimalProduction.some),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][value=01]").hasAttr("checked") shouldBe true
    }

    "transform sector code '01.11' to display value '01' in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.cerealsLeguminousCrops.some),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][value=01]").hasAttr("checked") shouldBe true
    }

    "transform sector code '03' to display value '03' in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.fishingAndAquaculture.some),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][value=03]").hasAttr("checked") shouldBe true
    }

    "transform sector code '03.21' to display value '03' in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.marineAquaculture.some),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][value=03]").hasAttr("checked") shouldBe true
    }

    "display empty form when sector value is None in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(None),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][checked]").size() shouldBe 0
    }

    "render sector page correctly with complete journey in UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.cropAnimalProduction.some),
        about = AboutUndertakingFormPage("My Test Undertaking Ltd".some),
        mode = appConfig.UpdateNaceMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
      }

      val result = performAction()
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=undertakingSector][value=01]").hasAttr("checked") shouldBe true
      document.select("h1").text() should include("industry sector")
    }
  }

  "handling request to get sector when NOT in UpdateNaceMode" must {

    def performAction() = controller.getSector(FakeRequest(GET, routes.UndertakingController.getSector.url))

    "execute getSectorPage logic when mode is not UpdateNaceMode" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.cropAnimalProduction.some),
        about = AboutUndertakingFormPage("Test Undertaking".some),
        mode = appConfig.NewRegMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
        mockGet[UndertakingJourney](eori1)(Right(journey.some))
      }

      val result = performAction()
      status(result) shouldBe OK
    }

    "redirect to about undertaking page when about is not answered" in {
      val journey = UndertakingJourney(
        sector = UndertakingSectorFormPage(Sector.cropAnimalProduction.some),
        about = AboutUndertakingFormPage(None),
        mode = appConfig.NewRegMode
      )
      inSequence {
        mockAuthWithEnrolment()
        mockGetOrCreate[UndertakingJourney](eori1)(Right(journey))
        mockGet[UndertakingJourney](eori1)(Right(journey.some))
      }

      checkIsRedirect(performAction(), routes.UndertakingController.getAboutUndertaking.url)
    }
  }

}

object UndertakingControllerSpec {
  final case class ModifyUndertakingRow(question: String, answer: String, changeUrl: String)
  final case class SectorRadioOption(sector: String, label: String, hint: String)
}
