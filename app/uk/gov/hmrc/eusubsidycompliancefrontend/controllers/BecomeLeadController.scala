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
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
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

  // TODO - refactor the code here and consider using getUndertaking to remove some of the boilerplate
  def getAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

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

  // TODO - determine the best point to check for a verified email address
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


  def postBecomeLeadEori: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmission(form: FormValues) =
      if (form.value.isTrue) promoteBusinessEntity()
      else Redirect(routes.AccountController.getAccountPage()).toFuture

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
            // Flush any state state
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
    implicit val eori: EORI = request.eoriNumber
    Ok(becomeAdminConfirmationPage()).toFuture
  }

}
