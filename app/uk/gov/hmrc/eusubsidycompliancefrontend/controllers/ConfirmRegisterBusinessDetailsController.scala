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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmRegisterBusinessDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BeneficiaryIDRequest, BeneficiaryIDResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmRegisterBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  escService: EscService,
  confirmRegisterBusinessDetailsPage: ConfirmRegisterBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmRegisterBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("confirmRegisterBusinessDetails")

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    escService
      .beneficiaryIDValidate(
        BeneficiaryIDRequest(
          idType = "EORI",
          idValue = request.eoriNumber.toString,
          requestType = "R",
          beneficiaryInfo = None
        )
      )
      .map {
        case Right(Some(resp)) =>
          logger.info(s"Beneficiary ID Response = $resp")
          Ok(confirmRegisterBusinessDetailsPage(confirmRegisterBusinessDetailsForm, Some(resp)))
        case Right(None) =>
          logger.info("No Beneficiary ID Response.")
          Redirect(
            routes.NeedRegistrationNumberBusinessController
              .showPage(routes.ConfirmRegisterBusinessDetailsController.showPage().url)
          )
        case Left(error) =>
          logger.error(s"Error = $error")
          Redirect(
            routes.NeedRegistrationNumberBusinessController.showPage(
              routes.ConfirmRegisterBusinessDetailsController.showPage().url
            )
          )
        case _ =>
          Ok(confirmRegisterBusinessDetailsPage(confirmRegisterBusinessDetailsForm, None))
      }
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    confirmRegisterBusinessDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(confirmRegisterBusinessDetailsPage(formWithErrors)).toFuture,
        formValues =>
          formValues.value match {
            case "yes" => {
              escService.getBeneficiaryIDValidation(request.eoriNumber.toString, "E", None).flatMap {
                case Right(Some(resp)) =>
                  logger.info(s"Beneficiary ID Response = $resp")
                  escService.validateBeneficiaries(resp).map { allValidationsSuccessful =>
                    if (allValidationsSuccessful) {
                      logger.info("All beneficiary validations successful. Redirecting to BenNotificationController.")
                      Redirect(routes.UndertakingController.getAboutUndertaking.url)
                    } else {
                      logger.info("Beneficiary validations failed.")
                      Redirect(
                        routes.HMRCEmailController
                          .showPage(routes.ConfirmRegisterBusinessDetailsController.showPage().url)
                      )
                    }
                  }
                case Right(None) =>
                  logger.info("No Beneficiary ID Response from UTID validation.")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmRegisterBusinessDetailsController.showPage().url)
                  ).toFuture
                case Left(error) =>
                  logger.error(s"Error while calling Beneficiary ID validation = $error")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmRegisterBusinessDetailsController.showPage().url)
                  ).toFuture
              }
            }
            case "no" =>
              Redirect(
                routes.HMRCEmailController.showPage(
                  routes.ConfirmRegisterBusinessDetailsController.showPage().url
                )
              ).toFuture
          }
      )
  }
}
