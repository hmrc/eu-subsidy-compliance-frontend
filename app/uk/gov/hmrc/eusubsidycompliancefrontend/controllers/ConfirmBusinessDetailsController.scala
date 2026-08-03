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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BeneficiaryIDRequest, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmBusinessDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmMultipleBusinessDetailsPage
import scala.concurrent.Future

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  escService: EscService,
  confirmBusinessDetailsPage: ConfirmBusinessDetailsPage,
  confirmMultipleBusinessDetailsPage: ConfirmMultipleBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("confirmBusinessDetails")

  private def multipleEoris(undertaking: Undertaking): Boolean =
    undertaking.getAllNonLeadEORIs.nonEmpty

  private def isSuspended(undertaking: Undertaking): Boolean =
    undertaking.isAutoSuspended

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>

    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
        escService.beneficiaryIDValidate(beneficiaryIDRequest(request.eoriNumber)).map {
          case Right(Some(resp)) =>
            logger.info(s"Beneficiary ID Response = $resp")
            Ok(confirmMultipleBusinessDetailsPage(confirmBusinessDetailsForm, isSuspended(undertaking), resp))
          case Right(None) =>
            logger.info("No Beneficiary ID Response.")
            InternalServerError("No Beneficiary ID Response")
          case Left(error) =>
            logger.error(s"Error = $error")
            InternalServerError(error.message)
        }
    }
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
      confirmBusinessDetailsForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
            if (multipleEoris(undertaking)) {
              BadRequest(confirmBusinessDetailsPage(formWithErrors, isSuspended(undertaking))).toFuture
            } else {
              BadRequest(confirmBusinessDetailsPage(formWithErrors, isSuspended(undertaking))).toFuture
            },
          form =>
            if (form.value == "yes")
              Redirect(routes.BenNotificationController.showPage()).toFuture
            else Redirect(routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)).toFuture
        )
    }
  }

  def beneficiaryIDRequest(eori: EORI): BeneficiaryIDRequest = {
    BeneficiaryIDRequest(
      idType = "UTID",
      idValue = s"$eori",
      requestType = "R",
      beneficiaryInfo = None
    )
  }
}
