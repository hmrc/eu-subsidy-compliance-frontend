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
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{SubsidyRetrieve, Undertaking, UndertakingSubsidies}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EscService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDashboardController @Inject()(
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  escService: EscService,
  store: Store,
  financialDashboardPage: FinancialDashboardPage,
)(implicit val appConfig: AppConfig, ec: ExecutionContext) extends BaseController(mcc) {

  import escActionBuilders._

  // TODO - specify date range
  // this should be start date of second previous tax year to current date
  def getFinancialDashboard: Action[AnyContent] = escAuthentication.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val subsidies: Future[UndertakingSubsidies] = for {
      undertaking <- store.get[Undertaking]
      r = undertaking.flatMap(_.reference).getOrElse(throw new IllegalStateException("No undertaking data on session"))
      // TODO - pass date range
      s = SubsidyRetrieve(r, None)
      subsidies <- escService.retrieveSubsidy(s)
    } yield subsidies

    // TODO - review error cases that should be handled here
    subsidies.map { s =>
      Ok(financialDashboardPage())
    }

  }

}
