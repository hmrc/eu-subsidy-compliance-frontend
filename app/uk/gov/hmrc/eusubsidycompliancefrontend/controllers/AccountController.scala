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

import cats.FlatMap.nonInheritedOps.toFlatMapOps
import cats.data.OptionT
import cats.implicits.catsSyntaxOptionId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney.Forms.DoYouClaimFormPage
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{EligibilityJourney, NilReturnJourney, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{Undertaking, UndertakingBalance, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.UndertakingStatus
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.*
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.*
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.BigDecimalFormatter.Syntax.toEuros
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.*
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary
import uk.gov.hmrc.eusubsidycompliancefrontend.models.BeneficiaryIDRequest

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

  var suspendedPageFlag = false


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
  
  private def handleUndertakingNotCreated(implicit e: EORI): Future[Result] = {
    logger.info("-------------------------------------------------handleUndertakingNotCreated")
    suspendedPageFlag = false

    val result = getOrCreateJourneys().map {
      case (eligibilityJourney, undertakingJourney) if !eligibilityJourney.isComplete && undertakingJourney.isEmpty =>
        logger.info(
          "Eligibility journey is not complete and undertakingJourney is empty so redirecting to Eligibility first empty page"
        )
        Redirect(routes.EligibilityFirstEmptyPageController.firstEmptyPage)
      case (_, undertakingJourney) if !undertakingJourney.isComplete =>
        logger.info(
          "Eligibility journey is not complete but undertakingJourney is not empty so redirecting to Undertaking first empty page"
        )
        Redirect(routes.UndertakingController.firstEmptyPage)
      case _ =>
        logger.info(
          "Eligibility journey is complete so redirecting to BusinessEntity getAddBusinessEntity"
        )
        Redirect(routes.AddBusinessEntityController.getAddBusinessEntity())
    }
    result.getOrElse(handleMissingSessionData("Account Home - Undertaking not created -"))
  }

  private def handleExistingUndertaking(
    undertaking: Undertaking
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent], eori: EORI): Future[Result] = {
    logger.info("handleExistingUndertaking")

    val isUpdated = undertaking.industrySector.toString.length == 5

    for {
      result <- undertaking.undertakingStatus match {

        case Some(UndertakingStatus.SuspendedUndertaking) =>
          if (isUpdated) {
            proceedToAccountPage(undertaking)
          } else {
            Future.successful(
              Redirect(routes.UndertakingInvalidSectorSuspendedPageController.showPage)
                .addingToSession("suspensionCode" -> "8")
                .addingToSession("sector" -> undertaking.industrySector.toString)
                .addingToSession("reportDue" -> undertaking.lastSubsidyUsageUpdt.getOrElse("").toString)
            )
          }

        case Some(UndertakingStatus.Inactive) =>
          if (isUpdated) {
            proceedToAccountPage(undertaking)
          } else {
            Future.successful(
              Redirect(routes.UndertakingInactivePageController.showPage)
                .addingToSession("suspensionCode" -> "9")
                .addingToSession("sector" -> undertaking.industrySector.toString)
                .addingToSession("reportDue" -> undertaking.lastSubsidyUsageUpdt.getOrElse("").toString)
            )
          }

        case Some(UndertakingStatus.SuspendedAutomated) =>
          if (suspendedPageFlag) {
            proceedToAccountPage(undertaking)
          } else {
            suspendedPageFlag = true
            Future.successful(
              Redirect(routes.UndertakingInvalidSectorSuspendedPageController.showPage)
                .addingToSession("suspensionCode" -> "1")
                .addingToSession("sector" -> undertaking.industrySector.toString)
                .addingToSession("reportDue" -> undertaking.lastSubsidyUsageUpdt.getOrElse("").toString)
            )
          }

        case _ =>
          if (isUpdated) {
            proceedToAccountPage(undertaking)
          } else {
            Future.successful(Redirect(routes.NaceUndertakingCategoryIntroController.showPage))
          }
      }
    } yield result
  }

  private def proceedToAccountPage(
                                    undertaking: Undertaking
                                  )(implicit r: AuthenticatedEnrolledRequest[AnyContent], eori: EORI): Future[Result] = {
    val result = for {
      _ <- getOrCreateJourneys(UndertakingJourney.fromUndertaking(undertaking))
      subsidies <- escService
        .retrieveSubsidiesForDateRange(undertaking.reference, timeProvider.today.toSearchRange)
        .toContext
      result <- escService
        .getUndertakingBalance(eori)
        .flatMap(b => renderAccountPage(undertaking, subsidies, b))
        .toContext
    } yield result

    result.getOrElse {
      logger.info(s"handling missing session data for $undertaking")
      handleMissingSessionData("Account Home - Existing Undertaking -")
    }
  }

  private def getOrCreateJourneys(
                                   undertakingJourney: UndertakingJourney = UndertakingJourney()
                                 )(implicit e: EORI): OptionT[Future, (EligibilityJourney, UndertakingJourney)] = {
    logger.info("getOrCreateJourneys")
    for {
      eligibilityJourney <- store
        .getOrCreate[EligibilityJourney](EligibilityJourney(doYouClaim = DoYouClaimFormPage(true.some)))
        .toContext
      undertakingJourney <- store.getOrCreate[UndertakingJourney](undertakingJourney).toContext
    } yield (eligibilityJourney, undertakingJourney)
  }

  private def renderAccountPage(
                                 undertaking: Undertaking,
                                 undertakingSubsidies: UndertakingSubsidies,
                                 balance: Option[UndertakingBalance]
                               )(implicit
                                 r: AuthenticatedEnrolledRequest[AnyContent]
                               ) = {
    implicit val eori: EORI = r.eoriNumber

    if (undertaking.isManuallySuspended)
      Future.successful(Redirect(routes.UndertakingSuspendedPageController.showPage(undertaking.isLeadEORI(eori)).url))
    else {
      def needRegistrationRedirect: Future[Result] =
        if (undertaking.getAllNonLeadEORIs.nonEmpty)
          Future.successful(Redirect(routes.NeedRegistrationNumberBusinessesController.showPage()))
        else
          Future.successful(Redirect(routes.NeedRegistrationNumberBusinessController.showPage(r.uri)))

      def dashboard: Future[Result] = {
        val today = timeProvider.today

        val lastSubmitted = undertaking.lastSubsidyUsageUpdt.orElse(undertakingSubsidies.lastSubmitted)
        val isTimeToReport = ReportReminderHelpers.isTimeToReport(lastSubmitted, today)
        val dueDate = ReportReminderHelpers.dueDateToReport(lastSubmitted.getOrElse(today)).toDisplayFormat
        val isOverdue = ReportReminderHelpers.isOverdue(lastSubmitted, today)
        val isSuspended = undertaking.isAutoSuspended
        val startDate = today.toEarliestTaxYearStart

        val summary = FinancialDashboardSummary.fromUndertakingSubsidies(
          undertaking,
          undertakingSubsidies,
          balance,
          today
        )

        def updateNilReturnJourney(n: NilReturnJourney): Future[NilReturnJourney] = {
          if (n.displayNotification) store.update[NilReturnJourney](e => e.copy(displayNotification = false))
          else n.toFuture
        }

        var agriOtherFlag: Boolean = true
        if (undertaking.industrySector.toString.take(2).equals(Sector.FishingAndAquaculture.toString)) {
          agriOtherFlag = false
        }
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
              totalSubsidies = summary.overall.total.value.toEuros,
              remainingAmount = summary.undertakingBalanceEUR.value.toEuros,
              currentPeriodStart = startDate.toDisplayFormat,
              isOverAllowance = summary.overall.allowanceExceeded,
              isSuspended = isSuspended,
              scp08IssuesExist = summary.scp08IssuesExist,
              agriOtherFlag = agriOtherFlag
            )
          )
          result.getOrElse(handleMissingSessionData("Nil Return Journey"))
        } else {
          val hasAdminValidatedBen: Boolean  = true
          // bool defines whether user sees notification page, then account or cannotUse servicePage for a member
          // awaiting to see if it should be account page straight away or whether we need to add in notification every time user is routing to account page.

          if (hasAdminValidatedBen) {
            logger.info("showing nonLeadAccountPage for non lead")
            Ok(
              nonLeadAccountPage(
                undertaking = undertaking,
                eori = undertaking.getLeadEORI,
                isLead = false,
                dueDate = dueDate,
                isOverdue = isOverdue,
                lastSubmitted = lastSubmitted.map(_.toDisplayFormat),
                neverSubmitted = undertakingSubsidies.hasNeverSubmitted,
                allowance = BigDecimal(summary.overall.sectorCap.toString()).toEuros,
                totalSubsidies = summary.overall.total.value.toEuros,
                remainingAmount = summary.undertakingBalanceEUR.value.toEuros,
                currentPeriodStart = startDate.toDisplayFormat,
                isSuspended = isSuspended,
                scp08IssuesExist = summary.scp08IssuesExist,
                agriOtherFlag = agriOtherFlag
              )
            ).toFuture
          } else {
            Future.successful(Redirect(routes.CannotUseServiceContactAdministratorController.show()))
          }
        }
      }

      if (undertaking.isLeadEORI(eori))
        escService
          .beneficiaryIDValidate(BeneficiaryIDRequest(idType = "UTID", idValue = s"$eori", requestType = "R", beneficiaryInfo = None))
          .flatMap {
            // SCP22: if any EORI with a known ID has validated=false, route to confirm page. EORIs without an ID (benIDType undefined) are excluded — they are need-registration cases, not confirm cases.
            case Right(None) => needRegistrationRedirect
            case Right(Some(resp)) if resp.beneficiaryInfo.exists(_.exists(bi => !bi.benIDType.isDefined)) =>
              needRegistrationRedirect
            case Right(Some(resp)) if resp.beneficiaryInfo.exists(_.exists(bi => bi.benIDType.isDefined && bi.validated.contains(false))) =>
              Future.successful(Redirect(routes.ConfirmBusinessDetailsController.showPage()))
            case _ => dashboard
          }
      else dashboard

    }
  }
}