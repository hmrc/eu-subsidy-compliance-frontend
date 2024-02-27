/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField, mandatory}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.{CreateUndertaking, UndertakingDisabled, UndertakingUpdated}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{DisableUndertakingToBusinessEntity, DisableUndertakingToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, EmailStatus, UndertakingName, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.{Store, UndertakingCache}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.{HeaderCarrier, HttpVerbs, NotFoundException}
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney.Forms.UndertakingCyaFormPage

@Singleton
class UndertakingEmailController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  override val emailService: EmailService,
  override val emailVerificationService: EmailVerificationService,
  override val confirmEmailPage: ConfirmEmailPage,
  override val inputEmailPage: InputEmailPage
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with EmailVerificationSupport
    with ControllerFormHelpers {

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private def generateBackLink(emailStatus: EmailStatus): Call = {
    emailStatus match {
      case EmailStatus.Unverified => routes.UnverifiedEmailController.unverifiedEmail
      case EmailStatus.Amend => routes.UndertakingAmendDetailsController.getAmendUndertakingDetails
      case EmailStatus.BecomeLead => routes.BecomeLeadController.getConfirmEmail
      case EmailStatus.CYA => routes.UndertakingCheckYourAnswersController.getCheckAnswers
      case _ => routes.UndertakingSectorController.getSector
    }
  }

  def getConfirmEmail: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.AboutUndertakingController.getAboutUndertaking).toFuture) { journey =>
        handleConfirmEmailGet[UndertakingJourney](
          previous = Call(HttpVerbs.GET, journey.previous),
          formAction = routes.UndertakingEmailController.postConfirmEmail
        )
      }
  }

  def getAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = generateBackLink(status).url
      Future.successful(Ok(inputEmailPage(emailForm, backLink, Some(status))))
  }

  def postAddEmailForVerification(status: EmailStatus = EmailStatus.New): Action[AnyContent] = enrolled.async {
    implicit request =>
      val backLink = generateBackLink(status)

      val nextUrl = (id: String) => routes.UndertakingEmailController.getVerifyEmail(id, Some(status)).url
      val reEnterEmailUrl = routes.UndertakingEmailController.getAddEmailForVerification(status).url

      emailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(inputEmailPage(errors, backLink.url, Some(status))).toFuture,
          form =>
            emailVerificationService.makeVerificationRequestAndRedirect(
              email = form.value,
              previousPage = backLink.url,
              nextPageUrl = nextUrl,
              reEnterEmailUrl = reEnterEmailUrl
            )
        )
  }

  override def addVerifiedEmailToJourney(implicit eori: EORI): Future[Unit] =
    store
      .update[UndertakingJourney](_.setHasVerifiedEmail(true))
      .void

  def postConfirmEmail: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.AboutUndertakingController.getAboutUndertaking) { journey =>
      handleConfirmEmailPost[UndertakingJourney](
        previous = journey.previous,
        inputEmailRoute = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.New).url,
        next = {
          val call =
            if (journey.isAmend) routes.UndertakingAmendDetailsController.getAmendUndertakingDetails
            else routes.UndertakingAddBusinessController.getAddBusiness

          if (journey.cya.value.getOrElse(false)) routes.UndertakingCheckYourAnswersController.getCheckAnswers.url
          else call.url
        },
        formAction = routes.UndertakingEmailController.postConfirmEmail
      )
    }
  }

  def getVerifyEmail(
    verificationId: String,
    status: Option[EmailStatus] = Some(EmailStatus.New)
  ): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withJourneyOrRedirect[UndertakingJourney](routes.AboutUndertakingController.getAboutUndertaking) { journey =>
      val previousAndNext = status match {
        case Some(emailStatus @ EmailStatus.Unverified) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.AccountController.getAccountPage
          )
        case Some(emailStatus @ EmailStatus.Amend) =>
          (
            routes.UndertakingAmendDetailsController.getAmendUndertakingDetails,
            routes.UndertakingAmendDetailsController.getAmendUndertakingDetails
          )
        case Some(emailStatus @ EmailStatus.BecomeLead) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.BecomeLeadController.getBecomeLeadEori()
          )
        case Some(emailStatus @ EmailStatus.CYA) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.UndertakingCheckYourAnswersController.getCheckAnswers
          )
        case _ =>
          (routes.UndertakingEmailController.getConfirmEmail, routes.UndertakingAddBusinessController.getAddBusiness)
      }

      handleVerifyEmailGet[UndertakingJourney](
        verificationId = verificationId,
        previous = previousAndNext._1,
        next = previousAndNext._2
      )
    }
  }
}
