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
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.ClaimDateFormProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.TaxYearSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.AddClaimDatePage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ClaimDateController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  addClaimDatePage: AddClaimDatePage,
  timeProvider: TimeProvider
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers
    with SubsidyJourneySupport {

  import actionBuilders._

  private val claimDateForm = ClaimDateFormProvider(timeProvider).form

  def getClaimDate: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      logger.info("ClaimDateController.getClaimDate")
      renderFormIfEligible { journey =>
        val form = journey.claimDate.value.fold(claimDateForm)(claimDateForm.fill)
        val earliestAllowedClaimDate = (timeProvider.today).minusDays(1095)
        Ok(
          addClaimDatePage(
            form = form,
            previous = journey.previous,
            earliestAllowedClaimDate = earliestAllowedClaimDate
          )
        )
      }
    }
  }

  def postClaimDate: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("ClaimDateController.postClaimDate")
      processFormSubmission[SubsidyJourney] { journey =>
        claimDateForm
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val earliestAllowedClaimDate = timeProvider.today.toEarliestTaxYearStart
              BadRequest(
                addClaimDatePage(
                  formWithErrors,
                  journey.previous,
                  earliestAllowedClaimDate
                )
              ).toContext
            },
            form =>
              store
                .update[SubsidyJourney](_ => journey.setClaimDate(form))
                .flatMap(_.next)
                .toContext
          )
      }
    }
  }
}
