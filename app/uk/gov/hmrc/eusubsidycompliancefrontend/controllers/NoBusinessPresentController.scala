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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, Store}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

@Singleton
class NoBusinessPresentController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
  noBusinessPresentPage: NoBusinessPresentPage
)(implicit val appConfig: AppConfig, executionContext: ExecutionContext)
    extends BaseController(mcc) {
  import escActionBuilders._

  def getNoBusinessPresent: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori = request.eoriNumber
    val previous      = routes.AccountController.getAccountPage().url
    escService.retrieveUndertaking(eori).map {
      _ match {
        case Some(undertaking) => Ok(noBusinessPresentPage(undertaking.name, previous))
        case None              => handleMissingSessionData("Undertaking")
      }
    }
  }

  def postNoBusinessPresent: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    for {
      _ <- store.update[BusinessEntityJourney] { businessEntityJourneyOpt =>
        businessEntityJourneyOpt.map(_.copy(isLeadSelectJourney = true.some))
      }
    } yield (Redirect(routes.BusinessEntityController.getAddBusinessEntity()))

  }

}
