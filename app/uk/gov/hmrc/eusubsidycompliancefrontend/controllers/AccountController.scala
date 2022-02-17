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


import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EligibilityJourney, EscService, RetrieveEmailService, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportDeMinimisReminderHelper, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject()(
   mcc: MessagesControllerComponents,
   escActionBuilders: EscActionBuilders,
   store: Store,
   escService: EscService,
   accountPage: AccountPage,
   timeProvider: TimeProvider,
   existingUndertakingPage: ExistingUndertakingPage,
   retrieveEmailService: RetrieveEmailService
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def getAccountPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def getAccountFlow: Future[Result] =   for {
      retrievedUndertaking <- escService.retrieveUndertaking(eori)
      eligibilityJourneyOpt <- store.get[EligibilityJourney]
      eligibilityJourney <- eligibilityJourneyOpt.fold(store.put(EligibilityJourney()))(Future.successful)
      undertakingJourneyOpt <- store.get[UndertakingJourney]
      undertakingJourney <- undertakingJourneyOpt.fold(store.put(UndertakingJourney.fromUndertakingOpt(retrievedUndertaking)))(Future.successful)
      businessEntityJourneyOpt <- store.get[BusinessEntityJourney]
      _ <- businessEntityJourneyOpt.fold(store.put(BusinessEntityJourney.fromUndertakingOpt(retrievedUndertaking)))(Future.successful)
      _ <- if (retrievedUndertaking.isDefined) store.put(retrievedUndertaking.getOrElse(sys.error("Undertaking is Missing"))) else Future.successful(Unit)
    } yield (retrievedUndertaking, eligibilityJourney, undertakingJourney) match {

      case (Some(undertaking), _, _) =>

        val lastDayToReportDate = undertaking.lastSubsidyUsageUpdt.map(_.plusDays(90))
        val lastDayToReportString = lastDayToReportDate.map(_.toDisplayFormat)
        val isTimeToReport = ReportDeMinimisReminderHelper.isTimeToReport(undertaking.lastSubsidyUsageUpdt, timeProvider.today)
        Ok(accountPage(undertaking, !undertaking.getAllNonLeadEORIs().isEmpty, isTimeToReport, lastDayToReportString))

      case (_, eJourney, uJourney) if !eJourney.isComplete && uJourney == UndertakingJourney() =>
        Redirect(routes.EligibilityController.firstEmptyPage())
      case (_, _, uJourney) if !uJourney.isComplete =>
        Redirect(routes.UndertakingController.firstEmptyPage())
      case _ =>
        Redirect(routes.BusinessEntityController.getAddBusinessEntity()) // TODO add this journey into the match
    }
    retrieveEmailService.retrieveEmailByEORI(eori).flatMap {_ match {
        case Some(_) =>  getAccountFlow
        case None => Future.successful(Redirect(routes.UpdateEmailAddressController.updateEmailAddress()))
      }
    }
  }

  def getExistingUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield {
      undertakingOpt match {
        case Some(undertaking) if(undertaking.isLeadEORI(eori)) => Redirect(routes.AccountController.getAccountPage())  //if logged in as lead EORI, redirect to Account home page
        case Some(undertaking) => Ok(existingUndertakingPage(undertaking.name)) //if logged in as non-lead EORI, redirect to existing undertaking page
        case None => Redirect(routes.AccountController.getAccountPage()) //if undertaking not present, then create undertaking
      }
    }
  }

}