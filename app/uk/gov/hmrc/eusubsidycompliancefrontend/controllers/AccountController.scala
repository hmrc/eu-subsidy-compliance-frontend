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

import cats.data.OptionT
import cats.implicits.catsSyntaxOptionId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms.DoYouClaimFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, NilReturnJourney, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax.BigDecimalOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  escService: EscService,
  leadAccountPage: LeadAccountPage,
  nonLeadAccountPage: NonLeadAccountPage,
  timeProvider: TimeProvider
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  def getAccountPage: Action[AnyContent] = {
    enrolled.async { implicit request =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("showing get account page")
      escService
        .retrieveUndertaking(eori)
        .toContext
        .foldF(handleUndertakingNotCreated)(handleExistingUndertaking)
    }
  }

  private def handleUndertakingNotCreated(implicit e: EORI, hc: HeaderCarrier): Future[Result] = {
    logger.info("handleUndertakingNotCreated")
    val result = getOrCreateJourneys().map {
      case (eligibilityJourney, undertakingJourney) if !eligibilityJourney.isComplete && undertakingJourney.isEmpty =>
        logger.info(
          "Eligibility journey is not complete but and undertakingJourney is empty so redirecting to Eligibility first empty page"
        )
        Redirect(routes.EligibilityController.firstEmptyPage)
      case (_, undertakingJourney) if !undertakingJourney.isComplete =>
        logger.info(
          "Eligibility journey is not complete but and undertakingJourney is not empty so redirecting to Undertaking first empty page"
        )
        Redirect(routes.UndertakingController.firstEmptyPage)
      case _ =>
        logger.info(
          "Eligibility journey is complete so redirecting to BusinessEntity getAddBusinessEntity"
        )
        Redirect(routes.BusinessEntityController.getAddBusinessEntity)
    }
    result.getOrElse(handleMissingSessionData("Account Home - Undertaking not created -"))
  }

  private def handleExistingUndertaking(
    undertaking: Undertaking
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent], e: EORI): Future[Result] = {
    logger.info("handleExistingUndertaking")

    val result = for {
      _ <- getOrCreateJourneys(UndertakingJourney.fromUndertaking(undertaking))
      subsidies <- escService.retrieveAllSubsidies(undertaking.reference).toContext
      result <- renderAccountPage(undertaking, subsidies).toContext
    } yield result

    result.getOrElse {
      logger.info(s"handling missing session data for $undertaking")
      handleMissingSessionData("Account Home - Existing Undertaking -")
    }
  }

  private def getOrCreateJourneys(
    undertakingJourney: UndertakingJourney = UndertakingJourney()
  )(implicit e: EORI, hc: HeaderCarrier): OptionT[Future, (EligibilityJourney, UndertakingJourney)] = {
    logger.info("getOrCreateJourneys")
    for {
      // At this point the user has an ECC enrolment so they must be eligible to use the service.
      eligibilityJourney <- store
        .getOrCreate[EligibilityJourney](EligibilityJourney(doYouClaim = DoYouClaimFormPage(true.some)))
        .toContext
      undertakingJourney <- store.getOrCreate[UndertakingJourney](undertakingJourney).toContext
    } yield (eligibilityJourney, undertakingJourney)
  }

  private def renderAccountPage(undertaking: Undertaking, undertakingSubsidies: UndertakingSubsidies)(implicit
    r: AuthenticatedEnrolledRequest[AnyContent]
  ) = {
    implicit val eori: EORI = r.eoriNumber

    val today = timeProvider.today

    val lastSubmitted = undertakingSubsidies.lastSubmitted.orElse(undertaking.lastSubsidyUsageUpdt)
    val isTimeToReport = ReportReminderHelpers.isTimeToReport(lastSubmitted, today)
    val dueDate = ReportReminderHelpers.dueDateToReport(lastSubmitted.getOrElse(today)).toDisplayFormat
    val isOverdue = ReportReminderHelpers.isOverdue(lastSubmitted, today)
    val startDate = today.toEarliestTaxYearStart

    val summary = FinancialDashboardSummary.fromUndertakingSubsidies(
      undertaking,
      undertakingSubsidies,
      today
    )

    def updateNilReturnJourney(n: NilReturnJourney): Future[NilReturnJourney] =
      if (n.displayNotification) store.update[NilReturnJourney](e => e.copy(displayNotification = false))
      else n.toFuture

    if (undertaking.isLeadEORI(eori)) {
      logger.info("showing account page for lead")
      val result = for {
        nilReturnJourney <- store.getOrCreate[NilReturnJourney](NilReturnJourney()).toContext
        _ <- updateNilReturnJourney(nilReturnJourney).toContext
      } yield Ok(
        leadAccountPage(
          undertaking = undertaking,
          eori = eori,
          isNonLeadEORIPresent = undertaking.getAllNonLeadEORIs.nonEmpty,
          isTimeToReport = isTimeToReport,
          dueDate = dueDate,
          isOverdue = isOverdue,
          isNilReturnDoneRecently = nilReturnJourney.displayNotification,
          lastSubmitted = lastSubmitted.map(_.toDisplayFormat),
          neverSubmitted = undertakingSubsidies.hasNeverSubmitted,
          allowance = BigDecimal(summary.overall.sectorCap.toString()).toEuros,
          totalSubsidies = summary.overall.total.toEuros,
          remainingAmount = summary.overall.allowanceRemaining.toEuros,
          currentPeriodStart = startDate.toDisplayFormat,
          isOverAllowance = summary.overall.allowanceExceeded
        )
      )

      result.getOrElse(handleMissingSessionData("Nil Return Journey"))
    } else {
      logger.info("showing nonLeadAccountPage for non lead")
      Ok(
        nonLeadAccountPage(
          undertaking,
          undertaking.getLeadEORI,
          dueDate,
          isOverdue,
          lastSubmitted.map(_.toDisplayFormat),
          undertakingSubsidies.hasNeverSubmitted,
          BigDecimal(summary.overall.sectorCap.toString()).toEuros,
          summary.overall.total.toEuros,
          summary.overall.allowanceRemaining.toEuros,
          startDate.toDisplayFormat
        )
      ).toFuture
    }
  }

}
