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

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.{EscCDSActionBuilders, EscInitialActionBuilder}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BecomeLeadController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  escInitialActionBuilders: EscInitialActionBuilder,
  store: Store,
  escService: EscService,
  emailService: EmailService,
  auditService: AuditService,
  becomeAdminPage: BecomeAdminPage,
  becomeAdminTermsAndConditionsPage: BecomeAdminTermsAndConditionsPage,
  becomeAdminConfirmationPage: BecomeAdminConfirmationPage
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escCDSActionBuilder._
  import escInitialActionBuilders._

  def getBecomeLeadEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
      journey <- store.get[BecomeLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      result <- becomeLeadResult(journey, undertakingOpt)
    } yield result
  }

  private def becomeLeadResult(
    becomeLeadJourneyOpt: Option[BecomeLeadJourney],
    undertakingOpt: Option[Undertaking]
  )(implicit request: AuthenticatedEscRequest[_], eori: EORI): Future[Result] =
    (becomeLeadJourneyOpt, undertakingOpt) match {
      case (Some(journey), Some(undertaking)) =>
        val form = journey.becomeLeadEori.value.fold(becomeAdminForm)(e => becomeAdminForm.fill(FormValues(e.toString)))
        Ok(becomeAdminPage(form, undertaking.name, eori)).toFuture
      case (None, Some(undertaking)) => // initialise the empty Journey model
        store.put(BecomeLeadJourney()).map { _ =>
          Ok(becomeAdminPage(becomeAdminForm, undertaking.name, eori))
        }
      case _ =>
        throw new IllegalStateException("missing undertaking name")
    }

  def postBecomeLeadEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    becomeAdminForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            undertakingOpt <- escService.retrieveUndertaking(eori)
          } yield undertakingOpt match {
            case Some(undertaking) =>
              BadRequest(becomeAdminPage(formWithErrors, undertaking.name, eori))
            case _ =>
              throw new IllegalStateException("missing undertaking name")
          },
        form =>
          store
            .update[BecomeLeadJourney](j =>
              j.copy(becomeLeadEori = j.becomeLeadEori.copy(value = Some(form.value == "true")))
            )
            .flatMap { _ =>
              if (form.value == "true") {
                Redirect(routes.BecomeLeadController.getAcceptPromotionTerms()).toFuture
              } else Future(Redirect(routes.AccountController.getAccountPage()))
            }
      )
  }

  /**
    * This route should check for the CDS enrolment to allow email if BE request to be a Lead.
    * @return
    */

  def getAcceptPromotionTerms: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    emailService.retrieveEmailByEORI(eori) flatMap { response =>
      response.emailType match {
        case EmailType.VerifiedEmail =>
          store.get[BecomeLeadJourney].flatMap {
            case Some(journey) =>
              if (journey.becomeLeadEori.value.getOrElse(false)) {
                Future(Ok(becomeAdminTermsAndConditionsPage(eori)))
              } else {
                Future(Redirect(routes.BecomeLeadController.getBecomeLeadEori()))
              }
            case None => Future(Redirect(routes.BecomeLeadController.getBecomeLeadEori()))
          }
        case EmailType.UnVerifiedEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUnverifiedEmailAddress()).toFuture
        case EmailType.UnDeliverableEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUndeliveredEmailAddress()).toFuture
      }
    }

  }

  def postAcceptPromotionTerms: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](j => j.copy(acceptTerms = j.acceptTerms.copy(value = Some(true))))
      .flatMap(_ => Future(Redirect(routes.BecomeLeadController.getPromotionConfirmation())))
  }

  def getPromotionConfirmation: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[BecomeLeadJourney].flatMap {
      case Some(journey) =>
        for {
          retrievedUndertaking <- escService
            .retrieveUndertaking(eori)
            .map(_.getOrElse(handleMissingSessionData("Undertaking")))
          undertakingRef = retrievedUndertaking.reference
          newLead = retrievedUndertaking.undertakingBusinessEntity
            .find(_.businessEntityIdentifier == eori)
            .fold(handleMissingSessionData("lead Business Entity"))(_.copy(leadEORI = true))
          oldLead = retrievedUndertaking.undertakingBusinessEntity
            .find(_.leadEORI)
            .fold(handleMissingSessionData("lead Business Entity"))(_.copy(leadEORI = false))
          _ <- escService.addMember(undertakingRef, newLead)
          _ <- escService.addMember(undertakingRef, oldLead)
          _ <- emailService.sendEmail(eori, PromotedSelfToNewLead, retrievedUndertaking)
          _ <- emailService.sendEmail(oldLead.businessEntityIdentifier, RemovedAsLeadToFormerLead, retrievedUndertaking)
          // Flush any stale undertaking journey data
           _ <- store.delete[UndertakingJourney]
          _ = auditService.sendEvent[BusinessEntityPromotedSelf](
            AuditEvent.BusinessEntityPromotedSelf(
              undertakingRef,
              request.authorityId,
              oldLead.businessEntityIdentifier,
              newLead.businessEntityIdentifier
            )
          )
        } yield
          if (journey.acceptTerms.value.getOrElse(false)) {
            Ok(becomeAdminConfirmationPage(oldLead.businessEntityIdentifier))
          } else {
            Redirect(routes.BecomeLeadController.getBecomeLeadEori())
          }
      case None => Future(Redirect(routes.BecomeLeadController.getBecomeLeadEori()))
    }
  }

  def getPromotionCleanup: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](_ => BecomeLeadJourney())
      .flatMap(_ => Future(Redirect(routes.AccountController.getAccountPage())))
  }

  private val becomeAdminForm: Form[FormValues] = Form(
    mapping("becomeAdmin" -> mandatory("becomeAdmin"))(FormValues.apply)(FormValues.unapply)
  )
}
