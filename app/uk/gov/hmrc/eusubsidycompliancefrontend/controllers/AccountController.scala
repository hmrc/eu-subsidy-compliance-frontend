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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  leadAccountPage: LeadAccountPage,
  nonLeadAccountPage: NonLeadAccountPage,
  timeProvider: TimeProvider,
  existingUndertakingPage: ExistingUndertakingPage,
  retrieveEmailService: RetrieveEmailService
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._

  def getAccountPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    retrieveEmailService.retrieveEmailByEORI(eori) flatMap {
      case Some(_) => getUndertakingAndHandleResponse
      case _ => Redirect(routes.UpdateEmailAddressController.updateEmailAddress()).toFuture
    }
  }

  def getExistingUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    escService.retrieveUndertaking(eori).map {
      case Some(undertaking) if undertaking.isLeadEORI(eori) => Redirect(routes.AccountController.getAccountPage())
      case Some(undertaking) => Ok(existingUndertakingPage(undertaking.name))
      case None => Redirect(routes.AccountController.getAccountPage())
    }
  }

  private def getUndertakingAndHandleResponse(implicit r: EscAuthRequest[AnyContent], e: EORI) =
    escService
      .retrieveUndertaking(r.eoriNumber)
      .toContext
      .flatMap(handleExistingUndertaking)
      .orElse(handleUndertakingNotCreated)
      .getOrElse(sys.error("Error during getAccount flow"))

  private def handleUndertakingNotCreated(implicit e: EORI) =
    getOrCreateJourneys().map {
      case (ej, uj) if !ej.isComplete && uj.isEmpty => Redirect(routes.EligibilityController.firstEmptyPage())
      case (_, uj) if !uj.isComplete => Redirect(routes.UndertakingController.firstEmptyPage())
      case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }

  private def handleExistingUndertaking(u: Undertaking)(implicit r: EscAuthRequest[AnyContent], e: EORI) =
    for {
      _ <- store.put(u).toContext
      _ <- getOrCreateJourneys(UndertakingJourney.fromUndertaking(u))
    } yield renderAccountPage(u)

  private def getOrCreateJourneys(u: UndertakingJourney = UndertakingJourney())(implicit  e: EORI) = for {
    ej <- store.get[EligibilityJourney].toContext.flatTapNone(store.put(EligibilityJourney()))
    uj <- store.get[UndertakingJourney].toContext.flatTapNone(store.put(u))
    _ <- store.get[BusinessEntityJourney].toContext.flatTapNone(store.put(BusinessEntityJourney()))
  } yield (ej, uj)

  private def renderAccountPage(undertaking: Undertaking)(implicit r: EscAuthRequest[AnyContent]) = {
    val currentTime = timeProvider.today

    val isTimeToReport = ReportReminderHelpers.isTimeToReport(undertaking.lastSubsidyUsageUpdt, currentTime)
    val dueDate = ReportReminderHelpers.dueDateToReport(undertaking.lastSubsidyUsageUpdt).map(_.toDisplayFormat)
    val isOverdue = ReportReminderHelpers.isOverdue(undertaking.lastSubsidyUsageUpdt, currentTime)

    if (undertaking.isLeadEORI(r.eoriNumber)) {
      Ok(leadAccountPage(
        undertaking,
        undertaking.getAllNonLeadEORIs().nonEmpty,
        isTimeToReport,
        dueDate,
        isOverdue
      ))
    }
    else Ok(nonLeadAccountPage(undertaking))
  }

}
