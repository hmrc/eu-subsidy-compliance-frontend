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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BecomeLeadController @Inject() (
                                       mcc: MessagesControllerComponents,
                                       actionBuilders: ActionBuilders,
                                       store: Store,
                                       escService: EscService,
                                       emailService: EmailService,
                                       auditService: AuditService,
                                       becomeAdminPage: BecomeAdminPage,
                                       becomeAdminAcceptResponsibilitiesPage: BecomeAdminAcceptResponsibilitiesPage,
                                       becomeAdminConfirmationPage: BecomeAdminConfirmationPage
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val becomeAdminForm = formWithSingleMandatoryField("becomeAdmin")

  def getAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    // TODO - simplify
    def handleRequest(
      becomeLeadJourneyOpt: Option[BecomeLeadJourney],
      undertakingOpt: Option[Undertaking]
    )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] = {
      (becomeLeadJourneyOpt, undertakingOpt) match {
        case (Some(journey), Some(_)) =>
          Ok(becomeAdminAcceptResponsibilitiesPage(eori)).toFuture
        case (None, Some(_)) => // initialise the empty Journey model
          store.put(BecomeLeadJourney()).map { _ =>
            Ok(becomeAdminAcceptResponsibilitiesPage(eori))
          }
        case _ =>
          // TODO - we should never be able to get there since there should always be an undertaking. :/
          throw new IllegalStateException("missing undertaking")
      }
    }

    // TODO - review the logic here
    for {
      journey <- store.get[BecomeLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      result <- handleRequest(journey, undertakingOpt)
    } yield result

  }

  def postAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](j => j.copy(acceptResponsibilities = j.acceptResponsibilities.copy(value = Some(true))))
      .flatMap(_ => Future(Redirect(routes.BecomeLeadController.getBecomeLeadEori())))
  }

  def getBecomeLeadEori: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleRequest(
      becomeLeadJourneyOpt: Option[BecomeLeadJourney],
      undertakingOpt: Option[Undertaking]
    )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] = {
      (becomeLeadJourneyOpt, undertakingOpt) match {
        case (Some(journey), Some(undertaking)) =>

          val form = journey.becomeLeadEori.value.fold(becomeAdminForm)(e => becomeAdminForm.fill(FormValues(e.toString)))
          Ok(becomeAdminPage(form, eori)).toFuture

        case (None, Some(undertaking)) => // initialise the empty Journey model
          store.put(BecomeLeadJourney()).map { _ =>
            Ok(becomeAdminPage(becomeAdminForm, eori))

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


  // TODO - review this code
  def postBecomeLeadEori: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    becomeAdminForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          for {
            undertakingOpt <- escService.retrieveUndertaking(eori)
          } yield undertakingOpt match {
            case Some(undertaking) =>
              BadRequest(becomeAdminPage(formWithErrors, eori))
            case _ =>
              throw new IllegalStateException("missing undertaking")
          },
        form =>
          store
            .update[BecomeLeadJourney](j =>
              j.copy(becomeLeadEori = j.becomeLeadEori.copy(value = Some(form.value.isTrue)))
            )
            .flatMap { _ =>
              if (form.value.isTrue) {
                Redirect(routes.BecomeLeadController.getAcceptResponsibilities()).toFuture
              } else Future(Redirect(routes.AccountController.getAccountPage()))
            }
      )
  }


  // TODO - review access on this route - needs a verified email
  def getPromotionConfirmation: Action[AnyContent] = enrolled.async { implicit request =>
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
          if (journey.acceptResponsibilities.value.getOrElse(false)) {
            Ok(becomeAdminConfirmationPage(oldLead.businessEntityIdentifier))
          } else {
            Redirect(routes.BecomeLeadController.getBecomeLeadEori())
          }
      case None => Future(Redirect(routes.BecomeLeadController.getBecomeLeadEori()))
    }
  }

  // TODO - review access on this route - needs a verified email
  def getPromotionCleanup: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](_ => BecomeLeadJourney())
      .flatMap(_ => Future(Redirect(routes.AccountController.getAccountPage())))
  }

}
