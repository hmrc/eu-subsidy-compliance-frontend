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

import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BecomeLeadController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override protected val store: Store,
  escService: EscService,
  override protected val emailService: EmailService,
  override protected val emailVerificationService: EmailVerificationService,
  auditService: AuditService,
  becomeAdminPage: BecomeAdminPage,
  becomeAdminAcceptResponsibilitiesPage: BecomeAdminAcceptResponsibilitiesPage,
  becomeAdminConfirmationPage: BecomeAdminConfirmationPage,
  override protected val confirmEmailPage: ConfirmEmailPage,
  override protected val inputEmailPage: InputEmailPage,

)(implicit
  val appConfig: AppConfig,
  override protected val executionContext: ExecutionContext
) extends BaseController(mcc) with EmailVerificationSupport {

  import actionBuilders._

  private val becomeAdminForm = formWithSingleMandatoryField("becomeAdmin")

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

  def getConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    handleConfirmEmailGet[BecomeLeadJourney](
      previous = routes.BecomeLeadController.getAcceptResponsibilities(),
      formAction = routes.BecomeLeadController.postConfirmEmail()
    )
  }

  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    handleConfirmEmailPost[BecomeLeadJourney](
      previous = routes.BecomeLeadController.getConfirmEmail(),
      next = routes.BecomeLeadController.getBecomeLeadEori(),
      formAction = routes.BecomeLeadController.postConfirmEmail(),
      generateVerifyEmailUrl = (id: String) => routes.BecomeLeadController.getVerifyEmail(id).url
    )
  }

  def getVerifyEmail(verificationId: String): Action[AnyContent] = enrolled.async { implicit request =>
    handleVerifyEmailGet[BecomeLeadJourney](
      verificationId = verificationId,
      previous = routes.BecomeLeadController.getConfirmEmail(),
      next = routes.BecomeLeadController.getBecomeLeadEori()
    )
  }

  def getBecomeLeadEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val result = for {
      journey <- store.getOrCreate[BecomeLeadJourney](BecomeLeadJourney()).toContext
      _ <- escService.retrieveUndertaking(eori).toContext
      form = journey.becomeLeadEori.value.fold(becomeAdminForm)(e => becomeAdminForm.fill(FormValues(e.toString)))
    } yield Ok(becomeAdminPage(form))

    result.getOrElse(handleMissingSessionData("No undertaking Found"))
  }


  def postBecomeLeadEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmission(form: FormValues) = {
      if (form.value.isTrue) promoteBusinessEntity()
      else Redirect(routes.AccountController.getAccountPage()).toFuture
    }

    def promoteBusinessEntity() = withJourneyOrRedirect[BecomeLeadJourney](routes.AccountController.getAccountPage()) { _ =>
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

  override protected def addVerifiedEmailToJourney(email: String)(implicit eori: EORI): Future[Unit] =
    ().toFuture

}
