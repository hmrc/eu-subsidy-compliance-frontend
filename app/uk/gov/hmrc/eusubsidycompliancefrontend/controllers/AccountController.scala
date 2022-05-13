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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscInitialActionBuilder
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{SubsidyRetrieve, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject() (
  mcc: MessagesControllerComponents,
  escInitialActionBuilders: EscInitialActionBuilder,
  store: Store,
  escService: EscService,
  leadAccountPage: LeadAccountPage,
  nonLeadAccountPage: NonLeadAccountPage,
  timeProvider: TimeProvider
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escInitialActionBuilders._

  private val dueDays = 90

  def getAccountPage: Action[AnyContent] =
    withAuthenticatedUser.async { implicit request =>
      implicit val eori: EORI = request.eoriNumber
      escService.retrieveUndertaking(eori) flatMap {
        case Some(u) => handleExistingUndertaking(u)
        case None => handleUndertakingNotCreated
      }
    }

  private def handleUndertakingNotCreated(implicit e: EORI): Future[Result] = {
    val result = getOrCreateJourneys().map {
      case (ej, uj) if !ej.isComplete && uj.isEmpty =>
        if (ej.eoriCheck.value.contains(true)) {
          Redirect(routes.EligibilityController.firstEmptyPage())
        } else {
          Redirect(routes.EligibilityController.getCustomsWaivers())
        }
      case (_, uj) if !uj.isComplete => Redirect(routes.UndertakingController.firstEmptyPage())
      case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }
    result.getOrElse(handleMissingSessionData("Account Home - Undertaking not created -"))
  }

  private def handleExistingUndertaking(
    u: Undertaking
  )(implicit r: AuthenticatedEscRequest[AnyContent], e: EORI): Future[Result] = {
    val result = for {
      _ <- getOrCreateJourneys(UndertakingJourney.fromUndertaking(u))
      result <- renderAccountPage(u).toContext
    } yield result

    result.getOrElse(handleMissingSessionData("Account Home - Existing Undertaking -"))
  }

  private def getOrCreateJourneys(u: UndertakingJourney = UndertakingJourney())(implicit e: EORI) =
    for {
      ej <- store.getOrCreate[EligibilityJourney](EligibilityJourney()).toContext
      uj <- store.getOrCreate[UndertakingJourney](u).toContext
    } yield (ej, uj)

  private def renderAccountPage(undertaking: Undertaking)(implicit r: AuthenticatedEscRequest[AnyContent]) = {
    implicit val eori: EORI = r.eoriNumber

    val currentDay = timeProvider.today

    val isTimeToReport = ReportReminderHelpers.isTimeToReport(undertaking.lastSubsidyUsageUpdt, currentDay)
    val dueDate = ReportReminderHelpers.dueDateToReport(undertaking.lastSubsidyUsageUpdt).map(_.toDisplayFormat)
    val isOverdue = ReportReminderHelpers.isOverdue(undertaking.lastSubsidyUsageUpdt, currentDay)

    def updateNilReturnJourney(n: NilReturnJourney): Future[NilReturnJourney] =
      if (n.displayNotification) store.update[NilReturnJourney](e => e.copy(displayNotification = false))
      else n.toFuture

    if (undertaking.isLeadEORI(eori)) {
      val result = for {
        nilReturnJourney <- store.getOrCreate[NilReturnJourney](NilReturnJourney()).toContext
        _ <- updateNilReturnJourney(nilReturnJourney).toContext
        undertakingReference <- undertaking.reference.toContext
        searchRange = Some((currentDay.toEarliestTaxYearStart, currentDay))
        retrieveRequest = SubsidyRetrieve(undertakingReference, searchRange)
        subsidies <- escService.retrieveSubsidy(retrieveRequest).toContext
        _ = println(" subsidies ::" + subsidies)
        _ = println(" undertaking ::" + undertaking)
      } yield Ok(
        leadAccountPage(
          undertaking,
          undertaking.getAllNonLeadEORIs().nonEmpty,
          isTimeToReport,
          dueDate,
          isOverdue,
          nilReturnJourney.displayNotification,
          currentDay.plusDays(dueDays).toDisplayFormat,
          subsidies.hasNeverSubmitted
        )
      )

      result.getOrElse(handleMissingSessionData("Nil Return Journey"))

    } else Ok(nonLeadAccountPage(undertaking)).toFuture
  }

}
