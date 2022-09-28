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
import play.api.data.{Form, Forms}
import play.api.data.Forms.{email, mapping}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BecomeLeadController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  escService: EscService,
  emailService: EmailService,
  emailVerificationService: EmailVerificationService,
  auditService: AuditService,
  becomeAdminPage: BecomeAdminPage,
  becomeAdminAcceptResponsibilitiesPage: BecomeAdminAcceptResponsibilitiesPage,
  becomeAdminConfirmationPage: BecomeAdminConfirmationPage,
  confirmEmailPage: ConfirmEmailPage,
  inputEmailPage: InputEmailPage,

)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val becomeAdminForm = formWithSingleMandatoryField("becomeAdmin")

  private val optionalEmailForm = Form(
    mapping(
      "using-stored-email" -> mandatory("using-stored-email"),
      "email" -> mandatoryIfEqual("using-stored-email", "false", email)
    )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)
  )

  private val emailForm = Form(
    mapping(
      "email" -> email
    )(FormValues.apply)(FormValues.unapply)
  )

  def getAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val result = for {
      _ <- escService.getUndertaking(eori).toContext
      _ <- store.getOrCreate[BecomeLeadJourney](BecomeLeadJourney()).toContext
    } yield Ok(becomeAdminAcceptResponsibilitiesPage(eori))

    result.getOrElse(Redirect(routes.AccountController.getAccountPage()))
  }

  def postAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](_.setAcceptResponsibilities(true))
      .flatMap(_ => Future(Redirect(routes.BecomeLeadController.getConfirmEmail())))
  }

  private def withJourney(f: BecomeLeadJourney => Future[Result])(implicit eori: EORI) =
    store
      .get[BecomeLeadJourney]
      .toContext
      .foldF(Redirect(routes.AccountController.getAccountPage()).toFuture)(f)

  def getConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    withJourney { _ =>
      val previous = routes.BecomeLeadController.getAcceptResponsibilities().url
      val formAction = routes.BecomeLeadController.postConfirmEmail()

      def renderConfirmEmailPage(email: EmailAddress) =
        Ok(confirmEmailPage(optionalEmailForm, formAction, email, previous))

      findVerifiedEmail
        .toContext
        .fold(Ok(inputEmailPage(emailForm, previous)))(e => renderConfirmEmailPage(EmailAddress(e)))
    }
  }

  private def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier) = {

    def findVerifiedEmailOnCds: Future[Option[String]] =
      emailService
        .retrieveEmailByEORI(eori)
        .map {
          case RetrieveEmailResponse(EmailType.VerifiedEmail, email) => email.map(_.value)
          case _ => Option.empty
        }

      emailVerificationService
        .getEmailVerification(eori)
        .toContext
        .foldF(findVerifiedEmailOnCds)(_.email.some.toFuture)
  }

  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val previous = routes.BecomeLeadController.getConfirmEmail()
    val next = routes.BecomeLeadController.getBecomeLeadEori().url
    val formAction = routes.BecomeLeadController.postConfirmEmail()

    def verifyEmailUrl(id: String) = routes.BecomeLeadController.getVerifyEmail(id).url

    val verifiedEmail = findVerifiedEmail

    def handleConfirmEmailPageSubmission(email: String) =
      optionalEmailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(confirmEmailPage(errors, formAction, EmailAddress(email), previous.url)).toFuture,
          form =>
            if (form.usingStoredEmail.isTrue)
              for {
                _ <- emailVerificationService.addVerifiedEmail(eori, email)
                _ <- store.update[UndertakingJourney](_.setVerifiedEmail(email))
              } yield Redirect(next)
            else emailVerificationService.makeVerificationRequestAndRedirect(email, previous, verifyEmailUrl)
        )

    def handleInputEmailPageSubmission() =
      emailForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(inputEmailPage(errors, previous.url)).toFuture,
          form => emailVerificationService.makeVerificationRequestAndRedirect(form.value, previous, verifyEmailUrl)
        )

    verifiedEmail
      .toContext
      .foldF(handleInputEmailPageSubmission())(email => handleConfirmEmailPageSubmission(email))
  }

  def getVerifyEmail(verificationId: String): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val redirectToAccountHome = Redirect(routes.AccountController.getAccountPage())

    store
      .get[BecomeLeadJourney]
      .toContext
      .foldF(redirectToAccountHome.toFuture) { _ =>
        emailVerificationService.approveVerificationRequest(eori, verificationId).flatMap { result =>
          val approveSuccessful = result.getMatchedCount > 0
          if (approveSuccessful) {
            val result = for {
              stored <- emailVerificationService.getEmailVerification(eori).toContext
              _ <- store.update[UndertakingJourney](_.setVerifiedEmail(stored.email)).toContext
            } yield Redirect(routes.BecomeLeadController.getBecomeLeadEori().url)

            result.getOrElse(redirectToAccountHome)
          }
          else Redirect(routes.BecomeLeadController.getConfirmEmail().url).toFuture
        }
      }
  }

  // TODO - review this
  def getBecomeLeadEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleRequest(
      becomeLeadJourneyOpt: Option[BecomeLeadJourney],
      undertakingOpt: Option[Undertaking]
    )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] = {
      (becomeLeadJourneyOpt, undertakingOpt) match {
        case (Some(journey), Some(_)) =>

          val form = journey.becomeLeadEori.value.fold(becomeAdminForm)(e => becomeAdminForm.fill(FormValues(e.toString)))
          Ok(becomeAdminPage(form)).toFuture

        case (None, Some(_)) => // initialise the empty Journey model
          store.put(BecomeLeadJourney()).map { _ =>
            Ok(becomeAdminPage(becomeAdminForm))

          }

        case _ => throw new IllegalStateException("missing undertaking name")
      }
    }

    for {
      journey <- store.get[BecomeLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      result <- handleRequest(journey, undertakingOpt)
    } yield result
  }


  def postBecomeLeadEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmission(form: FormValues) = {
      println(s"handling form: $form")
      if (form.value.isTrue) promoteBusinessEntity()
      else Redirect(routes.AccountController.getAccountPage()).toFuture
    }

    def promoteBusinessEntity() =
      store.get[BecomeLeadJourney].toContext
        .foldF(Redirect(routes.BecomeLeadController.getBecomeLeadEori()).toFuture) { journey =>
          val result = for {
            undertaking <- escService.getUndertaking(eori).toContext
            ref = undertaking.reference
            newLead = undertaking.getBusinessEntityByEORI(eori).copy(leadEORI = true)
            formerLead = undertaking.getLeadBusinessEntity.copy(leadEORI = false)
            // Promote new lead
            _ <- escService.addMember(ref, newLead).toContext
            _ <- emailService.sendEmail(eori, PromotedSelfToNewLead, undertaking).toContext
            // Demote former lead
            _ <- escService.addMember(ref, formerLead).toContext
            _ <- emailService.sendEmail(formerLead.businessEntityIdentifier, RemovedAsLeadToFormerLead, undertaking).toContext
            // Flush any stale journey state
            _ <- store.delete[UndertakingJourney].toContext
            _ <- store.delete[BusinessEntityJourney].toContext
            // Send audit event
            _ = auditService.sendEvent[BusinessEntityPromotedSelf](
              AuditEvent.BusinessEntityPromotedSelf(
                ref,
                request.authorityId,
                formerLead.businessEntityIdentifier,
                newLead.businessEntityIdentifier
              )
            )
          } yield Redirect(routes.BecomeLeadController.getPromotionConfirmation())

          result.getOrElse(throw new IllegalStateException("Unexpected error promoting business entity to lead"))
        }

    becomeAdminForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(becomeAdminPage(formWithErrors)).toFuture,
        handleFormSubmission
      )
  }


  def getPromotionConfirmation: Action[AnyContent] = verifiedEmail.async { implicit request =>
    Ok(becomeAdminConfirmationPage()).toFuture
  }

}
