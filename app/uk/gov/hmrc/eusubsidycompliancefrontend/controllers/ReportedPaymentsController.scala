/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ReportedPaymentsPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ReportedPaymentsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  reportedPaymentsPage: ReportedPaymentsPage,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  def getReportedPayments: Action[AnyContent] = verifiedEori.async { implicit request =>
    logger.info("ReportedPaymentsController.getReportedPayments")
    withLeadUndertaking(renderReportedPaymentsPage(_))
  }

  private[controllers] def renderReportedPaymentsPage(
    undertaking: Undertaking,
    showSuccess: Boolean = false
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent]) = {
    implicit val eori: EORI = request.eoriNumber
    val currentDate = timeProvider.today
    escService
      .retrieveSubsidiesForDateRange(undertaking.reference, currentDate.toSearchRange)
      .map { subsidies =>
        Ok(
          reportedPaymentsPage(
            subsidies.forReportedPaymentsPage,
            undertaking,
            currentDate.toEarliestTaxYearStart,
            currentDate.toTaxYearEnd.minusYears(1),
            currentDate.toTaxYearStart,
            routes.AccountController.getAccountPage.url,
            showSuccessBanner = showSuccess
          )
        )
      }
  }
}
