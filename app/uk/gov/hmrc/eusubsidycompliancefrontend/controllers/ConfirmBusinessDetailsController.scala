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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BeneficiaryIDRequest, BeneficiaryIDResponse, BeneficiaryInfo, BeneficiaryInfoResp, ConnectorError, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmBusinessDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmMultipleBusinessDetailsPage
import uk.gov.hmrc.http.HeaderCarrier

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

  def showPageNew(): Action[AnyContent] = enrolled.async { implicit request =>
    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
      escService.getBeneficiaryIDValidation(undertaking.reference.toString, "U", None).map {
        case Right(Some(resp)) =>
          logger.info(s"Beneficiary ID Response = $resp")
          if (resp.beneficiaryInfo.getOrElse(Seq.empty).exists(_.validated.contains(false))) {
            Ok(confirmBusinessDetailsPage(confirmBusinessDetailsForm, false, Some(resp)))
          } else {
            Redirect(routes.AccountController.getAccountPage)
          }
        case Right(None) =>
          logger.info("No Beneficiary ID Response.")
          Redirect(
            routes.HMRCEmailController.showPage(
              routes.ConfirmBusinessDetailsController.showPage().url
            )
          )
        case Left(error) =>
          logger.error(s"Error = $error")
          Redirect(
            routes.HMRCEmailController.showPage(
              routes.ConfirmBusinessDetailsController.showPage().url
            )
          )
      }
    }
  }

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
      escService.getBeneficiaryIDValidation(undertaking.reference.toString, "U", None).map {
        case Right(Some(resp)) =>
          logger.info(s"Beneficiary ID Response = $resp")
          if (resp.beneficiaryInfo.getOrElse(Seq.empty).exists(_.validated.contains(false))) {
            Ok(
              confirmMultipleBusinessDetailsPage(
                confirmBusinessDetailsForm,
                isSuspended(undertaking),
                resp,
                request.eoriNumber.toString
              )
            )
          } else {
            Redirect(routes.AccountController.getAccountPage)
          }
        case Right(None) =>
          logger.info("No Beneficiary ID Response.")
          Redirect(
            routes.HMRCEmailController.showPage(
              routes.ConfirmBusinessDetailsController.showPage().url
            )
          )
        case Left(error) =>
          logger.error(s"Error = $error")
          Redirect(
            routes.HMRCEmailController.showPage(
              routes.ConfirmBusinessDetailsController.showPage().url
            )
          )
      }
    }
  }

  def submitPageNew(): Action[AnyContent] = enrolled.async { implicit request =>
    logger.info("----- completing submit page new -----------")
    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
      confirmBusinessDetailsForm
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(confirmBusinessDetailsPage(formWithErrors, false)).toFuture,
          form =>
            if (form.value == "yes") {
              escService.getBeneficiaryIDValidation(undertaking.reference.toString, "U", None).flatMap {
                case Right(Some(resp)) =>
                  logger.info(s"Beneficiary ID Response = $resp")
                  escService.validateBeneficiaries(resp).map { allValidationsSuccessful =>
                    if (allValidationsSuccessful) {
                      logger.info("All beneficiary validations successful. Redirecting to BenNotificationController.")
                      Redirect(routes.UndertakingController.getAboutUndertaking.url)
                    } else {
                      logger.info("Beneficiary validations failed.")
                      Redirect(
                        routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPageNew().url)
                      )
                    }
                  }
                case Right(None) =>
                  logger.info("No Beneficiary ID Response from UTID validation.")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
                  ).toFuture
                case Left(error) =>
                  logger.error(s"Error while calling Beneficiary ID validation = $error")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
                  ).toFuture
              }

            } else
              Redirect(
                routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPageNew().url)
              ).toFuture
        )
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
            if (form.value == "yes") {

              escService.getBeneficiaryIDValidation(undertaking.reference.toString, "U", None).flatMap {
                case Right(Some(resp)) =>
                  logger.info(s"Beneficiary ID Response = $resp")
                  escService.validateBeneficiaries(resp).map { allValidationsSuccessful =>
                    if (allValidationsSuccessful) {
                      logger.info("All beneficiary validations successful. Redirecting to BenNotificationController.")
                      Redirect(routes.BenNotificationController.showPage())
                    } else {
                      logger.info("Beneficiary validations failed.")
                      Redirect(
                        routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
                      )
                    }
                  }
                case Right(None) =>
                  logger.info("No Beneficiary ID Response from UTID validation.")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
                  ).toFuture
                case Left(error) =>
                  logger.error(s"Error while calling Beneficiary ID validation = $error")
                  Redirect(
                    routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
                  ).toFuture
              }
            } else {
              Redirect(
                routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
              ).toFuture
            }
        )
    }
  }

}
