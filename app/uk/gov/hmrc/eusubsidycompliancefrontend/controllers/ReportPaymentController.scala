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
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ReportPaymentStartPage
import cats.data.OptionT

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportPaymentController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  reportPaymentStartPage: ReportPaymentStartPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers
    with SubsidyJourneySupport {

  import actionBuilders._

  private val reportPaymentForm = formWithSingleMandatoryField("report-payment")

  def startReportPaymentJourney: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      store
        .put[SubsidyJourney](SubsidyJourney())
        .map(_ => Redirect(routes.ReportPaymentController.getReportPayment.url))
    }
  }

  def getReportPayment: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.reportPayment.value
            .fold(reportPaymentForm)(v => reportPaymentForm.fill(FormValues(v)))
        Ok(reportPaymentStartPage(updatedForm, journey.previous))
      }
    }
  }

  def postReportPayment: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidFormSubmission(f: FormValues): OptionT[Future, Result] = {
        val userSelection = f.value.isTrue
        store.update[SubsidyJourney](_.setReportPayment(userSelection)).toContext.map { _ =>
          if (userSelection)
            Redirect(routes.ReportedNoCustomSubsidyController.getReportedNoCustomSubsidyPage)
          else Redirect(routes.NoClaimNotificationController.getNoClaimNotification)
        }
      }

      processFormSubmission[SubsidyJourney] { journey =>
        reportPaymentForm
          .bindFromRequest()
          .fold(
            errors =>
              BadRequest(
                reportPaymentStartPage(
                  errors,
                  journey.previous
                )
              ).toContext,
            handleValidFormSubmission
          )
      }
    }
  }
}
