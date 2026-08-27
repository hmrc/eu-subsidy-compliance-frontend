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

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ExistingAdminConfirmAddBusinessDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BeneficiaryIDRequest, BeneficiaryIDResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.SubsidyJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ExistingAdminConfirmAddBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  escService: EscService,
  store: Store,
  existingAdminConfirmAddBusinessDetailsPage: ExistingAdminConfirmAddBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders.*

  private val existingAdminConfirmAddBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("existingAdminConfirmAddBusinessDetails")

  // Should i make a new page for existing admin even tho its identical? Also back to confirm details link on email page sends to the other confirm details url???
  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI.EORI = request.eoriNumber
    store.get[SubsidyJourney].flatMap {
      case Some(journey) if journey.addClaimEori.value.flatMap(_.value).isDefined =>
        val claimEori = journey.addClaimEori.value.flatMap(_.value).get
        escService
          .beneficiaryIDValidate(
            BeneficiaryIDRequest(idType = "EORI", idValue = claimEori, requestType = "R", beneficiaryInfo = None)
          )
          .map {
            case Right(Some(resp)) if resp.beneficiaryInfo.exists(_.exists(_.benIDType.isDefined)) =>
              Ok(existingAdminConfirmAddBusinessDetailsPage(existingAdminConfirmAddBusinessDetailsForm, Some(resp)))
            case _ =>
              Redirect(
                routes.NeedRegistrationNumberBusinessController
                  .showPage(routes.AddClaimBusinessController.getAddClaimBusiness.url)
              )
          }
      case _ =>
        Redirect(
          routes.NeedRegistrationNumberBusinessController
            .showPage(routes.AddClaimBusinessController.getAddClaimBusiness.url)
        ).toFuture
    }
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    existingAdminConfirmAddBusinessDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          BadRequest(
            existingAdminConfirmAddBusinessDetailsPage(formWithErrors)
          ).toFuture,
        {
          case FormValues("yes") =>
            Redirect(
              routes.AddClaimPublicAuthorityController.getAddClaimPublicAuthority
            ).toFuture

          case FormValues("no") =>
            Redirect(
              routes.HMRCEmailController.showPage(
                routes.ExistingAdminConfirmAddBusinessDetailsController.showPage().url
              )
            ).toFuture
        }
      )
  }
}
