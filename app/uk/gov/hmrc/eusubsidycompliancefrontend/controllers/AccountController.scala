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

import cats.data.OptionT
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.ReportDeMinimisReminderHelper.{dueDateToReport, isOverdue, isTimeToReport}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
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
  existingUndertakingPage: ExistingUndertakingPage,
  retrieveEmailService: RetrieveEmailService
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._

  def getAccountPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    retrieveEmailService
      .retrieveEmailByEORI(eori).flatMap {
      case Some(_) => getUndertakingAndHandleResponse
      case None => Redirect(routes.UpdateEmailAddressController.updateEmailAddress()).toFuture
    }
  }

  private def getUndertakingAndHandleResponse(implicit r: EscAuthRequest[AnyContent], eori: EORI) =
    escService
      .retrieveUndertaking(r.eoriNumber)
      .toContext
      .flatTransform {
        case Some(u) => handleExistingUndertaking(u)
        case None => handleUndertakingNotCreated
      } getOrElse(sys.error("Error during getAccount flow"))

  private def handleUndertakingNotCreated(implicit r: EscAuthRequest[AnyContent], eori: EORI) = {
    // TODO - are these steps really necessary or can they be handled in their respective controllers?
    val result = for {
      eligibilityJourney <- store.get[EligibilityJourney].toContext.orElse(store.put(EligibilityJourney()).toContext)
      undertakingJourney <- store.get[UndertakingJourney].toContext.orElse(store.put(UndertakingJourney()).toContext)
      // TODO - why are we doing this here?
      _ <- store.get[BusinessEntityJourney].toContext.orElse(store.put(BusinessEntityJourney()).toContext)
    } yield (eligibilityJourney, undertakingJourney)

    result.map {
      case (ej, uj) if !ej.isComplete && uj.isEmpty => Redirect(routes.EligibilityController.firstEmptyPage())
      case (_, uj) if !uj.isComplete => Redirect(routes.UndertakingController.firstEmptyPage())
      case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }.value

  }

  // TODO - need to allow empty undertaking where the eligibility journey has not been completed yet
  private def handleExistingUndertaking(undertaking: Undertaking)(implicit r: EscAuthRequest[AnyContent], eori: EORI) = {
    val result: OptionT[Future, (Undertaking, EligibilityJourney, UndertakingJourney)] = for {
      _ <- store.put(undertaking).toContext
      // TODO - are these all necessary here? Can they be handled elsewhere?
      eligibilityJourney <- store.get[EligibilityJourney].toContext.orElse(store.put(EligibilityJourney()).toContext)
      undertakingJourney <- store.get[UndertakingJourney].toContext.orElse(store.put(UndertakingJourney.fromUndertaking(undertaking)).toContext)
      // TODO - why are we doing this here?
      _ <- store.get[BusinessEntityJourney].toContext.orElse(store.put(BusinessEntityJourney()).toContext)
    } yield (undertaking, eligibilityJourney, undertakingJourney)

    result
      .map(_ => renderAccountPage(undertaking))
      .value
  }

  private def renderAccountPage(undertaking: Undertaking)(implicit r: EscAuthRequest[AnyContent]) = {
    val currentTime = timeProvider.today

    if (undertaking.isLeadEORI(r.eoriNumber)) {
      Ok(leadAccountPage(
        undertaking,
        undertaking.getAllNonLeadEORIs().nonEmpty,
        isTimeToReport(undertaking.lastSubsidyUsageUpdt, currentTime),
        dueDateToReport(undertaking.lastSubsidyUsageUpdt).map(_.toDisplayFormat),
        isOverdue(undertaking.lastSubsidyUsageUpdt, currentTime)
      ))
    }
    else Ok(nonLeadAccountPage(undertaking))
  }

  def getExistingUndertaking: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    escService.retrieveUndertaking(eori).map {
      case Some(undertaking) if undertaking.isLeadEORI(eori) => Redirect(routes.AccountController.getAccountPage())
      case Some(undertaking) => Ok(existingUndertakingPage(undertaking.name))
      case None => Redirect(routes.AccountController.getAccountPage())
    }
  }

}
