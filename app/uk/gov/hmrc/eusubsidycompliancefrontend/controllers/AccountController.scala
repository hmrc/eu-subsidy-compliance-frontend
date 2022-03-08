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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailType
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  leadAccountPage: LeadAccountPage,
  nonLeadAccountPage: NonLeadAccountPage,
  timeProvider: TimeProvider,
  retrieveEmailService: RetrieveEmailService
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._

  def getAccountPage: Action[AnyContent] = anyAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    retrieveEmailService.retrieveEmailByEORI(eori) flatMap {
      _.emailType match {
        case EmailType.VerifiedEmail => getUndertakingAndHandleResponse
        case EmailType.UnVerifiedEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUnverifiedEmailAddress()).toFuture
        case EmailType.UnDeliverableEmail =>
          Redirect(routes.UpdateEmailAddressController.updateUndeliveredEmailAddress()).toFuture
      }
    }
  }

  private def getUndertakingAndHandleResponse(implicit r: AuthenticatedEscRequest[AnyContent], e: EORI): Future[Result] = {
    escService.retrieveUndertaking(r.eoriNumber) flatMap {
      case Some(u) => handleExistingUndertaking(u)
      case None => handleUndertakingNotCreated
    }

  private def handleUndertakingNotCreated(implicit e: EORI): Future[Result] = {
    val result = getOrCreateJourneys().map {
      case (ej, uj) if !ej.isComplete && uj.isEmpty => Redirect(routes.EligibilityController.firstEmptyPage())
      case (_, uj) if !uj.isComplete => Redirect(routes.UndertakingController.firstEmptyPage())
      case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }
    result.getOrElse(handleMissingSessionData("Account Home - Undertaking not created -"))
  }

  private def handleExistingUndertaking(u: Undertaking)(implicit r: AuthenticatedEscRequest[AnyContent], e: EORI): Future[Result] = {
    val result = for {
      _ <- store.put(u).toContext
      _ <- getOrCreateJourneys(UndertakingJourney.fromUndertaking(u))
    } yield renderAccountPage(u)

    result.getOrElse(handleMissingSessionData("Account Home - Existing Undertaking -"))
  }

  private def getOrCreateJourneys(u: UndertakingJourney = UndertakingJourney())(implicit e: EORI) =
    for {
      ej <- store.get[EligibilityJourney].toContext.orElse(store.put(EligibilityJourney()).toContext)
      uj <- store.get[UndertakingJourney].toContext.orElse(store.put(u).toContext)
      _ <- store.get[BusinessEntityJourney].toContext.orElse(store.put(BusinessEntityJourney()).toContext)
    } yield (ej, uj)

  private def renderAccountPage(undertaking: Undertaking)(implicit r: AuthenticatedEscRequest[AnyContent]) = {
    val currentTime = timeProvider.today

    val isTimeToReport = ReportReminderHelpers.isTimeToReport(undertaking.lastSubsidyUsageUpdt, currentTime)
    val dueDate = ReportReminderHelpers.dueDateToReport(undertaking.lastSubsidyUsageUpdt).map(_.toDisplayFormat)
    val isOverdue = ReportReminderHelpers.isOverdue(undertaking.lastSubsidyUsageUpdt, currentTime)

    if (undertaking.isLeadEORI(r.eoriNumber)) {
      Ok(
        leadAccountPage(
          undertaking,
          undertaking.getAllNonLeadEORIs().nonEmpty,
          isTimeToReport,
          dueDate,
          isOverdue
        )
      )
    } else Ok(nonLeadAccountPage(undertaking))
  }

}
