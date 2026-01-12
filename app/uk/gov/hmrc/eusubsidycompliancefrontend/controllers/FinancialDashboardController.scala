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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.UndertakingBalance
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter.Syntax.DateOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class FinancialDashboardController @Inject() (
  actionBuilders: ActionBuilders,
  escService: EscService,
  financialDashboardPage: FinancialDashboardPage,
  mcc: MessagesControllerComponents,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends BaseController(mcc) {

  import actionBuilders._

  def getFinancialDashboard: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    logger.info("FinancialDashboardController.getFinancialDashboard")

    val today = timeProvider.today

    val minusDays = timeProvider.getMinusDaysForVal(appConfig.minusDays).toDisplayFormat

    // The search period covers the current tax year to date, and the previous 2 tax years.
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
      undertaking = undertakingOpt match {
        case Some(u) => u
        case None => throw new IllegalStateException(s"Undertaking for EORI: $eori not found")
      }
      subsidies <- escService.retrieveSubsidiesForDateRange(undertaking.reference, today.toSearchRange)
      balanceOpt: Option[UndertakingBalance] <- escService.getUndertakingBalance(eori)
      summary = FinancialDashboardSummary.fromUndertakingSubsidies(undertaking, subsidies, balanceOpt, today)
      sector = summary.overall.sector.toString.take(2)
      industrySectorKey: String = sector match {
        case "01" => "agriculture"
        case "03" => "fisheryAndAquaculture"
        case _ => "generalTrade"
      }
    } yield
      if (undertaking.isManuallySuspended)
        Redirect(routes.UndertakingSuspendedPageController.showPage(undertaking.isLeadEORI(eori)).url)
      else Ok(financialDashboardPage(eori, summary, industrySectorKey, minusDays))

  }

}
