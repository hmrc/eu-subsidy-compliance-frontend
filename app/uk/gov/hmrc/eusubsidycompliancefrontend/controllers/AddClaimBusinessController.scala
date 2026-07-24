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

import cats.data.OptionT
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.AddClaimBusinessPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddClaimBusinessController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  addClaimBusinessPage: AddClaimBusinessPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends uk.gov.hmrc.eusubsidycompliancefrontend.controllers.BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers
    with SubsidyJourneySupport {

  import actionBuilders._

  private val addClaimBusinessForm = formWithSingleMandatoryField("add-claim-business")

  // Temporary until the ID lookup API is implemented
  private val businessIdFound = true

  def getAddClaimBusiness: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      renderFormIfEligible { journey =>
        val updatedForm =
          journey.addClaimBusiness.value
            .fold(addClaimBusinessForm)(v => addClaimBusinessForm.fill(FormValues(v)))
        Ok(addClaimBusinessPage(updatedForm, journey.previous))
      }
    }
  }

  def postAddClaimBusiness: Action[AnyContent] = subsidyJourney.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber

      def handleValidFormSubmission(f: FormValues): OptionT[Future, Result] =
        for {
          updatedJourney <- store
            .update[SubsidyJourney](_.setAddBusiness(f.value.isTrue))
            .toContext

          addBusiness = updatedJourney.getAddBusiness
        } yield {
          if (!addBusiness) {
            Redirect(routes.AddClaimEoriController.getAddClaimEori)
          } else if (businessIdFound) {
            Redirect(
              routes.ExistingAdminConfirmAddBusinessDetailsController.showPage()
            )
          } else {
            Redirect(
              routes.NeedRegistrationNumberBusinessController.showPage(
                routes.AddClaimBusinessController.getAddClaimBusiness.url
              )
            )
          }
        }
      processFormSubmission[SubsidyJourney] { journey =>
        addClaimBusinessForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(addClaimBusinessPage(errors, journey.previous)).toContext,
            handleValidFormSubmission
          )
      }
    }
  }
}
