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

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.BusinessEntity
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{MemberRemoveSelfToBusinessEntity, MemberRemoveSelfToLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EmailService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.ExecutionContext

@Singleton
class SignOutController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  escService: EscService,
  emailService: EmailService,
  auditService: AuditService,
  timedOutPage: TimedOut,
  signOutPage: SignOutPage,
  cdsEnrolmentMissingPage: CdsEnrolmentMissingPage,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, executionContext: ExecutionContext)
    extends BaseController(mcc)
    with I18nSupport
    with Logging {

  import escCDSActionBuilder._

  val signOutFromTimeout: Action[AnyContent] = Action { implicit request =>
    Ok(timedOutPage()).withNewSession
  }

  val signOut: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    emailService.retrieveEmailByEORI(eori) flatMap { response =>
      response.emailType match {
        case EmailType.VerifiedEmail =>
          escService.retrieveUndertaking(eori).flatMap {
            case Some(undertaking) =>
              val removalEffectiveDateString = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
              val result = for {
                undertakingRef <- undertaking.reference.toContext
                removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(eori)
                leadEORI = undertaking.getLeadEORI
                _ <- escService.removeMember(undertakingRef, removeBE).toContext
                _ <- emailService.sendEmail(eori, MemberRemoveSelfToBusinessEntity, undertaking, removalEffectiveDateString).toContext
                _ <- emailService.sendEmail(leadEORI, eori, MemberRemoveSelfToLead, undertaking, removalEffectiveDateString).toContext
                _ = auditService
                  .sendEvent(
                    AuditEvent
                      .BusinessEntityRemovedSelf(undertakingRef, request.authorityId, leadEORI, eori)
                  )
              } yield ()
              result.fold(handleMissingSessionData("Undertaking Ref"))(_ => Ok(signOutPage()).withNewSession)

            case None => handleMissingSessionData("Undertaking journey")
          }

        case EmailType.UnVerifiedEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUnverifiedEmailAddress()).toFuture
        case EmailType.UnDeliverableEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUndeliveredEmailAddress()).toFuture
      }
    }

  }

  val noCdsEnrolment: Action[AnyContent] = Action { implicit request =>
    Ok(cdsEnrolmentMissingPage()).withNewSession

  }

}
