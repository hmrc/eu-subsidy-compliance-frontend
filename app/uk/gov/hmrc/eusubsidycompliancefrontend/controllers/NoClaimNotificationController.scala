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
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyNilReturn
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, NilSubmissionDate, SubsidyRetrieve, SubsidyUpdate}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EscService, NilReturnJourney, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NoClaimNotificationController @Inject() (
                                                mcc: MessagesControllerComponents,
                                                actionBuilders: ActionBuilders,
                                                store: Store,
                                                override val escService: EscService,
                                                auditService: AuditService,
                                                timeProvider: TimeProvider,
                                                noClaimNotificationPage: NoClaimNotificationPage,
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {
  import actionBuilders._

  def getNoClaimNotification: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      retrieveSubsidies(undertaking.reference).toContext
        .foldF(handleMissingSessionData("No claim notification - subsidies -")) { undertakingSubsidies =>
          val previous = routes.AccountController.getAccountPage().url
          implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)
          val lastSubmitted: Option[LocalDate] = undertakingSubsidies.nonHMRCSubsidyUsage.map(_.submissionDate) match {
            case Nil => undertaking.lastSubsidyUsageUpdt
            case  a  => Some(a.max)
          }
          val today = timeProvider.today
          val startDate = today.toEarliestTaxYearStart
          val summary = FinancialDashboardSummary.fromUndertakingSubsidies(
            undertaking,
            undertakingSubsidies,
            today.toEarliestTaxYearStart,
            today.toTaxYearEnd
          )

          // todo: neverSubmitted boolean, neverSubmittedTaxYearDate string & lastSubmitted string hardcoded here
          Ok(
            noClaimNotificationPage(
              noClaimForm,
              previous,
              undertakingSubsidies.hasNeverSubmitted, 
              startDate.toDisplayFormat,
              lastSubmitted.map(_.toDisplayFormat),
            )
          ).toFuture
        }
    }
  }

  private def retrieveSubsidies(r: UndertakingRef)(implicit request: AuthenticatedEnrolledRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber

    val searchRange = timeProvider.today.toSearchRange.some

    escService
      .retrieveSubsidy(SubsidyRetrieve(r, searchRange))
      .map(Option(_))
      .fallbackTo(Option.empty.toFuture)
  }

  def postNoClaimNotification: Action[AnyContent] = verifiedEmail.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      val previous = routes.AccountController.getAccountPage().url

      def handleValidNoClaim(): Future[Result] = {
        val nilSubmissionDate = timeProvider.today.plusDays(1)
        val result = for {
          reference <- undertaking.reference.toContext
          _ <- store.update[NilReturnJourney](e => e.copy(displayNotification = true)).toContext
          _ <- escService.createSubsidy(SubsidyUpdate(reference, NilSubmissionDate(nilSubmissionDate))).toContext
          _ = auditService
            .sendEvent(
              NonCustomsSubsidyNilReturn(request.authorityId, eori, reference, nilSubmissionDate)
            )
        } yield Redirect(routes.AccountController.getAccountPage())

        result.getOrElse(handleMissingSessionData("Undertaking ref"))
      }

      noClaimForm
        .bindFromRequest()
        .fold(
          // todo: hasSubmitted boolean, neverSubmittedTaxYearDate string & lastSubmitted string hardcoded here
          errors => BadRequest(noClaimNotificationPage(errors, previous, true, "[todo: neverReportedPayment date string]", "[todo: lastSubmitted date string]")).toFuture,
          handleValidNoClaim
        )
    }
  }

  private val noClaimForm: Form[FormValues] = formWithSingleMandatoryField("noClaimNotification")

}
