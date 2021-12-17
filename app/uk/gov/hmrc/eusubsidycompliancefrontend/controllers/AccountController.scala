/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EscConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EligibilityJourney, Store, UndertakingJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  connector: EscConnector,
  accountPage: AccountPage
)(
  implicit val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends
  BaseController(mcc) {

  import escActionBuilders._

  def getAccountPage: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    for {
      retrievedUndertaking <- connector.retrieveUndertaking(eori)
      x <- store.get[EligibilityJourney]
      eligibilityJourney <- x.fold(store.put(EligibilityJourney()))(Future.successful)
      y <- store.get[UndertakingJourney]
      undertakingJourney <- y.fold(store.put(UndertakingJourney.fromUndertakingOpt(retrievedUndertaking)))(Future.successful)
    } yield (retrievedUndertaking, eligibilityJourney, undertakingJourney) match {
      case (Some(undertaking), _, _) =>
        val isLead = undertaking.undertakingBusinessEntity.find(_.businessEntityIdentifier == eori).exists(_.leadEORI)
        val ref = undertaking.reference.getOrElse(throw new IllegalStateException("no ref for retrieved undertaking"))
        Ok(accountPage(ref, isLead))
      case (_, eJourney, uJourney) if !eJourney.isComplete && uJourney == UndertakingJourney() =>
        Redirect(routes.EligibilityController.firstEmptyPage())
      case (_, _, uJourney) if !uJourney.isComplete =>
        Redirect(routes.UndertakingController.firstEmptyPage())
      case _ =>
        Redirect(routes.BusinessEntityController.getAddBusinessEntity()) // TODO add this journey into the match
    }
  }

}