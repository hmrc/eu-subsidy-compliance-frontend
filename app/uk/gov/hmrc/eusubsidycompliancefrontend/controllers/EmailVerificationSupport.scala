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
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContent, Call, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{EmailAddress, FormValues, OptionalEmailFormInput}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BecomeLeadJourney, EmailService, EmailVerificationService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{ConfirmEmailPage, InputEmailPage}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait EmailVerificationSupport extends FormHelpers { this: FrontendController =>

  protected val emailService: EmailService
  protected val emailVerificationService: EmailVerificationService
  protected val confirmEmailPage: ConfirmEmailPage
  protected val inputEmailPage: InputEmailPage

  protected implicit val executionContext: ExecutionContext

  protected val optionalEmailForm: Form[OptionalEmailFormInput] = Form(
    mapping(
      "using-stored-email" -> mandatory("using-stored-email"),
      "email" -> mandatoryIfEqual("using-stored-email", "false", email)
    )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)
  )

  protected val emailForm: Form[FormValues] = Form(
    mapping(
      "email" -> email
    )(FormValues.apply)(FormValues.unapply)
  )

  protected def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] = {
    emailVerificationService
      .getEmailVerification(eori)
      .toContext
      .foldF(retrieveCdsEmail)(_.email.some.toFuture)
  }

  private def retrieveCdsEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] =
    emailService
      .retrieveEmailByEORI(eori)
      .map {
        case RetrieveEmailResponse(EmailType.VerifiedEmail, email) => email.map(_.value)
        case _ => Option.empty
      }

  protected def withJourney[A: ClassTag](f: A => Future[Result])(implicit eori: EORI, reads: Reads[A]) =
    store
      .get[A]
      .toContext
      .foldF(Redirect(routes.AccountController.getAccountPage()).toFuture)(f)

  def handleConfirmEmailGet[A: ClassTag](
    previousPage: Call,
    formAction: Call
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], messages: Messages, appConfig: AppConfig, reads: Reads[A]) = {

    implicit val eori: EORI = request.eoriNumber

    withJourney[A] { _ =>

      findVerifiedEmail
        .toContext
        .fold(Ok(inputEmailPage(emailForm, previousPage.url))) { e =>
          Ok(confirmEmailPage(optionalEmailForm, formAction, EmailAddress(e), previousPage.url))
        }
    }
  }
}
