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

import cats.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.UndertakingBalance
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax.LocalDateTaxYearOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.FinancialDashboardPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.FinancialDashboardSummary

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

    // The search period covers the current tax year to date, and the previous 2 tax years.
    val result = for {
      undertaking <- escService.retrieveUndertaking(eori).toContext
      r <- undertaking.reference.toContext
      subsidies <- escService.retrieveSubsidiesForDateRange(r, today.toSearchRange).toContext
      summary <-
        if (appConfig.scp08Enabled)
          escService
            .getUndertakingBalance(eori)
            .map(b => FinancialDashboardSummary.fromUndertakingSubsidiesWithBalance(undertaking, subsidies, b, today))
            .toContext
        else FinancialDashboardSummary.fromUndertakingSubsidies(undertaking, subsidies, None, today).toContext
    } yield Ok(financialDashboardPage(summary))

    result
      .getOrElse(handleMissingSessionData("Undertaking"))

  }

}
