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

import play.api.Logging
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{Format, Reads}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.EmailFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, FormValues}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EmailVerificationService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{ConfirmEmailPage, InputEmailPage}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait EmailVerificationSupport extends ControllerFormHelpers with Logging { this: FrontendController =>

  protected val emailService: EmailService
  protected val emailVerificationService: EmailVerificationService
  protected val confirmEmailPage: ConfirmEmailPage
  protected val inputEmailPage: InputEmailPage

  protected implicit val executionContext: ExecutionContext

  protected val isThisYourEmailForm: Form[FormValues] = formWithSingleMandatoryField("using-stored-email")

  protected val emailForm: Form[FormValues] = EmailFormProvider().form

  protected def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] = retrieveCdsEmail

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
          Ok(confirmEmailPage(isThisYourEmailForm, formAction, EmailAddress(e), previous.url))
        }
    }
  }

  protected def addVerifiedEmailToJourney(implicit eori: EORI): Future[Unit]

  protected def handleConfirmEmailPost[A : ClassTag](
    previous: String,
    next: String,
    inputEmailRoute: String,
    emailStatus: Option[EmailStatus] = None,
    formAction: Call
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    messages: Messages,
    appConfig: AppConfig
  ): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    def handleConfirmEmailPageSubmission(email: String) = {
      isThisYourEmailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(confirmEmailPage(errors, formAction, EmailAddress(email), previous)).toFuture,
          form =>
            if (form.value.isTrue) {
              addVerifiedEmailToJourney.map { _ =>
                Redirect(next)
              }
            } else {
              Redirect(inputEmailRoute).toFuture
            }
        )
    }

    findVerifiedEmail.toContext
      .foldF(Redirect(inputEmailRoute).toFuture)(email => handleConfirmEmailPageSubmission(email))
  }

  protected def handleVerifyEmailGet[A : ClassTag](
    verificationId: String,
    previous: Call,
    next: Call
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], format: Format[A]): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    withJourneyOrRedirect[A](previous) { _ =>
      emailVerificationService.getEmailVerificationStatus.flatMap {
        case Some(email) =>
          for {
            _ <- emailService
              .updateEmailForEori(eori, email.emailAddress)
            _ <- addVerifiedEmailToJourney
          } yield Redirect(next.url)
        case None => {
          logger.warn(s"No verified email for user with authorityId: ${request.authorityId}")
          Redirect(previous.url).toFuture
        }
      }
    }

  }
}
