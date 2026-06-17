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

import cats.data.OptionT
import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyRemoved
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingRef.UndertakingRef
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{ConfirmRemoveClaim, ReportedPaymentsPage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveSubsidyClaimController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  auditService: AuditService,
  confirmRemovePage: ConfirmRemoveClaim,
  reportedPaymentsPage: ReportedPaymentsPage,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val removeSubsidyClaimForm = formWithSingleMandatoryField("removeSubsidyClaim")

  def getRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    withLeadUndertaking { undertaking =>
      val result = for {
        reference <- undertaking.reference.toContext
        subsidies <- escService.retrieveSubsidiesForDateRange(reference, timeProvider.today.toSearchRange).toContext
        sub <- subsidies.nonHMRCSubsidyUsage.find(_.subsidyUsageTransactionId.contains(transactionId)).toContext
      } yield Ok(confirmRemovePage(removeSubsidyClaimForm, sub))
      result.fold(handleMissingSessionData("Subsidy Journey"))(identity)
    }
  }

  def postRemoveSubsidyClaim(transactionId: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { undertaking =>
      removeSubsidyClaimForm
        .bindFromRequest()
        .fold(
          formWithErrors => handleRemoveSubsidyFormError(formWithErrors, transactionId, undertaking),
          formValue =>
            if (formValue.value.isTrue) handleRemoveSubsidyValidAnswer(transactionId, undertaking)
            else Redirect(routes.ReportedPaymentsController.getReportedPayments).toFuture
        )
    }
  }

  private def getNonHmrcSubsidy(
    transactionId: String,
    reference: UndertakingRef
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): OptionT[Future, NonHmrcSubsidy] = {
    implicit val e: EORI = r.eoriNumber
    escService
      .retrieveSubsidiesForDateRange(reference, timeProvider.today.toSearchRange)
      .toContext
      .flatMap(_.findNonHmrcSubsidy(transactionId).toContext)
  }

  private def handleRemoveSubsidyFormError(
    formWithErrors: Form[FormValues],
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    val result = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
    } yield BadRequest(confirmRemovePage(formWithErrors, nonHmrcSubsidy))
    result.fold(handleMissingSessionData("nonHMRC subsidy"))(identity)
  }

  private def handleRemoveSubsidyValidAnswer(
    transactionId: String,
    undertaking: Undertaking
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    val result: OptionT[Future, Unit] = for {
      reference <- undertaking.reference.toContext
      nonHmrcSubsidy <- getNonHmrcSubsidy(transactionId, reference)
      _ <- escService.removeSubsidy(reference, nonHmrcSubsidy).toContext
      _ = auditService.sendEvent[NonCustomsSubsidyRemoved](
        AuditEvent.NonCustomsSubsidyRemoved(request.authorityId, reference)
      )
    } yield ()
    result.foldF(handleMissingSessionData("nonHMRC subsidy")) { _ =>
      renderReportedPaymentsPage(undertaking, showSuccess = true)
    }
  }

  private def renderReportedPaymentsPage(
    undertaking: Undertaking,
    showSuccess: Boolean = false
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber
    val currentDate = timeProvider.today
    escService
      .retrieveSubsidiesForDateRange(undertaking.reference, currentDate.toSearchRange)
      .map { subsidies =>
        Ok(
          reportedPaymentsPage(
            subsidies.forReportedPaymentsPage,
            undertaking,
            currentDate.toEarliestTaxYearStart,
            currentDate.toTaxYearEnd.minusYears(1),
            currentDate.toTaxYearStart,
            routes.AccountController.getAccountPage.url,
            showSuccessBanner = showSuccess
          )
        )
      }
  }
}
