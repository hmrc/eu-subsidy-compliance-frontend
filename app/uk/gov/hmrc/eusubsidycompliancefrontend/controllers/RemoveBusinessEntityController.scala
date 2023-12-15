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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveBusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  timeProvider: TimeProvider,
  emailService: EmailService,
  auditService: AuditService,
  removeBusinessPage: RemoveBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val removeBusinessForm = formWithSingleMandatoryField("removeBusiness")

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    logger.info("RemoveBusinessEntityController.getRemoveBusinessEntity")
    withLeadUndertaking { _ =>
      escService.retrieveUndertaking(EORI(eoriEntered)).map {
        case Some(undertaking) =>
          logger.info(s"Found undertaking for $eoriEntered")
          val removeBE = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))
          Ok(removeBusinessPage(removeBusinessForm, removeBE, routes.AddBusinessEntityController.startJourney().url))
        case _ =>
          logger.info(
            s"Did not find undertaking for $eoriEntered, redirecting to AddBusinessEntityController.getAddBusinessEntity"
          )
          Redirect(routes.AddBusinessEntityController.getAddBusinessEntity())
      }
    }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = verifiedEori.async {
    implicit request: AuthenticatedEnrolledRequest[AnyContent] =>
      logger.info("RemoveBusinessEntityController.postRemoveBusinessEntity")

      withLeadUndertaking { _ =>
        escService
          .retrieveUndertaking(EORI(eoriEntered))
          .toContext
          .foldF(Redirect(routes.AddBusinessEntityController.getAddBusinessEntity()).toFuture) { undertaking =>
            val undertakingRef = undertaking.reference
            val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))

            removeBusinessForm
              .bindFromRequest()
              .fold(
                errors =>
                  BadRequest(
                    removeBusinessPage(errors, removeBE, routes.AddBusinessEntityController.startJourney().url)
                  ).toFuture,
                success = form => handleValidBE(eoriEntered, form, undertakingRef, removeBE, undertaking)
              )
          }
      }
  }

  private def handleValidBE(
    eoriEntered: String,
    form: FormValues,
    undertakingRef: UndertakingRef,
    businessEntityToRemove: BusinessEntity,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    implicit val eori: EORI = request.eoriNumber
    logger.info(s"handleValidBE for eoriEntered:$eoriEntered")

    if (form.value.isTrue) {
      logger.info("processing form as true is selected")
      val effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
      for {
        _ <- escService.removeMember(undertakingRef, businessEntityToRemove)
        _ <- emailService.sendEmail(EORI(eoriEntered), RemoveMemberToBusinessEntity, undertaking, effectiveDate)
        _ <- emailService.sendEmail(eori, EORI(eoriEntered), RemoveMemberToLead, undertaking, effectiveDate)
        _ = auditService
          .sendEvent(
            AuditEvent.BusinessEntityRemoved(undertakingRef, request.authorityId, eori, EORI(eoriEntered))
          )
      } yield {
        logger.info(
          s"Removed undertakingRef:$undertakingRef, Redirecting to AddBusinessEntityController.getAddBusinessEntity"
        )
        Redirect(
          routes.AddBusinessEntityController
            .startJourney(businessRemoved = Some(true), removedAddedEoriOpt = Some(eoriEntered))
        )
      }
    } else {
      //fixme why do we need this?
      logger.info(
        s"Did not remove undertakingRef:$undertakingRef as form was not true, redirecting to AddBusinessEntityController.getAddBusinessEntity"
      )
      Redirect(routes.AddBusinessEntityController.startJourney()).toFuture
    }
  }
}
