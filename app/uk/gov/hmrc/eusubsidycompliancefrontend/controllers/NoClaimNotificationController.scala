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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.NilReturnJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyNilReturn
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, NilSubmissionDate, SubsidyUpdate, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NoClaimNotificationController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  auditService: AuditService,
  timeProvider: TimeProvider,
  noClaimNotificationPage: NoClaimNotificationPage,
  noClaimConfirmationPage: NoClaimConfirmationPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with ControllerFormHelpers
    with LeadOnlyUndertakingSupport {
  import actionBuilders._

  private val noClaimForm: Form[FormValues] = formWithSingleMandatoryField("noClaimNotification")

  def getNoClaimNotification: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    logger.info("NoBusinessPresentController.getNoClaimNotification")

    withLeadUndertaking { undertaking =>
      escService
        .retrieveSubsidiesForDateRange(undertaking.reference, timeProvider.today.toSearchRange)
        .toContext
        .foldF(handleMissingSessionData("No claim notification - subsidies -")) { undertakingSubsidies =>
          val previous =
            if (undertakingSubsidies.hasNeverSubmitted) routes.SubsidyController.getReportPaymentFirstTimeUser.url
            else routes.SubsidyController.getReportedPaymentReturningUserPage.url
          val today = timeProvider.today
          val startDate = today.toEarliestTaxYearStart

          val lastSubmitted = undertakingSubsidies.lastSubmitted.orElse(undertaking.lastSubsidyUsageUpdt)

          logger.info("NoBusinessPresentController.getNoClaimNotification showing noClaimNotificationPage")

          Ok(
            noClaimNotificationPage(
              noClaimForm,
              previous,
              undertakingSubsidies.hasNeverSubmitted,
              startDate.toDisplayFormat,
              lastSubmitted
                .map(_.toDisplayFormat)
                .getOrElse("")
            )
          ).toFuture
        }
    }
  }

  def postNoClaimNotification: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    logger.info("NoBusinessPresentController.postNoClaimNotification")

    withLeadUndertaking { undertaking: Undertaking =>
      escService
        .retrieveSubsidiesForDateRange(undertaking.reference, timeProvider.today.toSearchRange)
        .toContext
        .foldF(handleMissingSessionData("No claim notification - subsidies")) { undertakingSubsidies =>
          val previous =
            if (undertakingSubsidies.hasNeverSubmitted) routes.SubsidyController.getReportPaymentFirstTimeUser.url
            else routes.SubsidyController.getReportedPaymentReturningUserPage.url
          val today = timeProvider.today
          val startDate = today.toEarliestTaxYearStart

          val lastSubmitted = undertakingSubsidies.lastSubmitted.orElse(undertaking.lastSubsidyUsageUpdt)

          noClaimForm
            .bindFromRequest()
            .fold(
              errors => {
                logger.info(s"NoBusinessPresentController.postNoClaimNotification showing errors")

                BadRequest(
                  noClaimNotificationPage(
                    errors,
                    previous,
                    undertakingSubsidies.hasNeverSubmitted,
                    startDate.toDisplayFormat,
                    lastSubmitted
                      .map(_.toDisplayFormat)
                      .getOrElse("")
                  )
                ).toFuture
              },
              _ => {
                handleValidNoClaim(undertaking)
              }
            )
        }
    }
  }

  private def handleValidNoClaim(
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    implicit val eori: EORI = request.eoriNumber

    val nilSubmissionDate = timeProvider.today

    val result = for {
      reference <- undertaking.reference.toContext
      _ <- store.update[NilReturnJourney](e => e.copy(displayNotification = true)).toContext
      _ <- escService.createSubsidy(SubsidyUpdate(reference, NilSubmissionDate(nilSubmissionDate))).toContext
      _ = auditService
        .sendEvent(
          NonCustomsSubsidyNilReturn(request.authorityId, eori, reference, nilSubmissionDate)
        )
      _ <- escService.clearUndertakingCache(reference).toContext
      isSuspended = undertaking.isAutoSuspended
    } yield Redirect(routes.NoClaimNotificationController.getNotificationConfirmation(isSuspended))

    result.getOrElse(handleMissingSessionData("Undertaking ref"))
  }

  // We need to show a confirmation along with the next submission date
  def getNotificationConfirmation(isSuspended: Boolean = false): Action[AnyContent] = verifiedEori.async {
    implicit request =>
      logger.info("NoBusinessPresentController.getNotificationConfirmation")

      withLeadUndertaking { _ =>
        val nextClaimDueDate = ReportReminderHelpers.dueDateToReport(timeProvider.today)
        Ok(noClaimConfirmationPage(nextClaimDueDate, isSuspended)).toFuture
      }
  }

}
