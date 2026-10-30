/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.implicits._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EmailStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EmailVerificationService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{ConfirmEmailPage, InputEmailPage}
import uk.gov.hmrc.http.HttpVerbs

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingEmailController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  override val emailService: EmailService,
  override val emailVerificationService: EmailVerificationService,
  val errorHandler: ErrorHandler,
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
      case EmailStatus.Amend => routes.UndertakingController.getAmendUndertakingDetails
      case EmailStatus.BecomeLead => routes.BecomeLeadController.getConfirmEmail
      case EmailStatus.CYA => routes.UndertakingController.getCheckAnswers
      case EmailStatus.New => routes.UndertakingEmailController.getConfirmEmail
      case _ => routes.UndertakingController.getSector
    }
  }

  def getConfirmEmail: Action[AnyContent] = enrolledUndertakingJourney.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .get[UndertakingJourney]
      .toContext
      .foldF(Redirect(routes.UndertakingController.getAboutUndertaking).toFuture) { journey =>
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
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      handleConfirmEmailPost[UndertakingJourney](
        previous = journey.previous,
        inputEmailRoute = routes.UndertakingEmailController.getAddEmailForVerification(EmailStatus.New).url,
        next = {
          val call =
            if (journey.isAmend) routes.UndertakingController.getAmendUndertakingDetails
            else routes.UndertakingAddBusinessController.getAddBusiness

          if (journey.cya.value.getOrElse(false)) routes.UndertakingController.getCheckAnswers.url
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
    withJourneyOrRedirect[UndertakingJourney](routes.UndertakingController.getAboutUndertaking) { journey =>
      val previousAndNext = status match {
        case Some(emailStatus @ EmailStatus.Unverified) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.AccountController.getAccountPage
          )
        case Some(emailStatus @ EmailStatus.Amend) =>
          (
            routes.UndertakingController.getAmendUndertakingDetails,
            routes.UndertakingController.getAmendUndertakingDetails
          )
        case Some(emailStatus @ EmailStatus.BecomeLead) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.BecomeLeadController.getBecomeLeadEori()
          )
        case Some(emailStatus @ EmailStatus.CYA) =>
          (
            routes.UndertakingEmailController.getAddEmailForVerification(emailStatus),
            routes.UndertakingController.getCheckAnswers
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
