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
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{Format, Reads}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.{EmailFormProvider, OptionalEmailFormProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, FormValues, OptionalEmailFormInput}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EmailVerificationService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{ConfirmEmailPage, InputEmailPage}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait EmailVerificationSupport extends ControllerFormHelpers { this: FrontendController =>

  protected val emailService: EmailService
  protected val emailVerificationService: EmailVerificationService
  protected val confirmEmailPage: ConfirmEmailPage
  protected val inputEmailPage: InputEmailPage

  protected implicit val executionContext: ExecutionContext

  protected val optionalEmailForm: Form[OptionalEmailFormInput] = OptionalEmailFormProvider().form

  protected val emailForm: Form[FormValues] = EmailFormProvider().form

  protected def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] =
    emailVerificationService
      .getEmailVerification(eori)
      .toContext
      .foldF(retrieveCdsEmail)(_.email.some.toFuture)

  private def retrieveCdsEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] =
    emailService
      .retrieveEmailByEORI(eori)
      .map {
        case RetrieveEmailResponse(EmailType.VerifiedEmail, email) => email.map(_.value)
        case _ => Option.empty
      }

  // Runs the supplied function if a journey is found in the store. Otherwise redirects back to noJourneyFound.
  protected def withJourneyOrRedirect[A : ClassTag](
    noJourneyFound: Call
  )(f: A => Future[Result])(implicit eori: EORI, reads: Reads[A]): Future[Result] =
    store
      .get[A]
      .toContext
      .foldF(Redirect(noJourneyFound).toFuture)(f)

  protected def handleConfirmEmailGet[A : ClassTag](
    previous: Call,
    formAction: Call
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    messages: Messages,
    appConfig: AppConfig,
    format: Format[A]
  ): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    withJourneyOrRedirect[A](previous) { _ =>
      findVerifiedEmail.toContext
        .fold(Ok(inputEmailPage(emailForm, previous.url))) { e =>
          Ok(confirmEmailPage(optionalEmailForm, formAction, EmailAddress(e), previous.url))
        }
    }
  }

  protected def addVerifiedEmailToJourney(email: String)(implicit eori: EORI): Future[Unit]

  protected def handleConfirmEmailPost[A : ClassTag](
    previous: Call,
    next: Call,
    formAction: Call,
    generateVerifyEmailUrl: String => String
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    messages: Messages,
    appConfig: AppConfig
  ): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    def verifyEmailUrl(id: String) = generateVerifyEmailUrl(id)

    def handleConfirmEmailPageSubmission(email: String) =
      optionalEmailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(confirmEmailPage(errors, formAction, EmailAddress(email), previous.url)).toFuture,
          form =>
            if (form.usingStoredEmail.isTrue)
              for {
                _ <- emailVerificationService.addVerifiedEmail(eori, email)
                _ <- addVerifiedEmailToJourney(email)
              } yield Redirect(next)
            else {
              // Redirect back to the previous page if no email address appears to be entered. This should never happen
              // with a legitimate form submission.
              form.value.fold(Redirect(previous).toFuture) { email =>
                emailVerificationService.makeVerificationRequestAndRedirect(email, previous, verifyEmailUrl)
              }
            }
        )

    def handleInputEmailPageSubmission() =
      emailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(inputEmailPage(errors, previous.url)).toFuture,
          form => emailVerificationService.makeVerificationRequestAndRedirect(form.value, previous, verifyEmailUrl)
        )

    findVerifiedEmail.toContext
      .foldF(handleInputEmailPageSubmission())(email => handleConfirmEmailPageSubmission(email))
  }

  protected def handleVerifyEmailGet[A : ClassTag](
    verificationId: String,
    previous: Call,
    next: Call
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], format: Format[A]): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    withJourneyOrRedirect[A](previous) { _ =>
      emailVerificationService.approveVerificationRequest(eori, verificationId).flatMap { approveSuccessful =>
        if (approveSuccessful) {
          val result = for {
            stored <- emailVerificationService.getEmailVerification(eori).toContext
            _ <- addVerifiedEmailToJourney(stored.email).toContext
          } yield Redirect(next.url)

          result.getOrElse(Redirect(previous))
        } else Redirect(previous.url).toFuture
      }
    }

  }
}
