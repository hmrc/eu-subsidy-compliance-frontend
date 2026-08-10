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
      Ok(confirmBusinessDetailsPage(confirmBusinessDetailsForm, false)).toFuture
    }

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    escService.getUndertaking(request.eoriNumber).flatMap { undertaking =>
      escService.getBeneficiaryIDValidation(request.eoriNumber.toString, "U", None).map {
        case Right(Some(resp)) =>
          logger.info(s"Beneficiary ID Response = $resp")
          if (multipleEoris(undertaking))
            Ok(confirmMultipleBusinessDetailsPage(confirmBusinessDetailsForm, isSuspended(undertaking), resp))
          else
            Ok(confirmBusinessDetailsPage(confirmBusinessDetailsForm, isSuspended(undertaking), Some(resp)))
        case Right(None) =>
          logger.info("No Beneficiary ID Response.")
          InternalServerError("No Beneficiary ID Response") // need to ask what need to do??
        case Left(error) =>
          logger.error(s"Error = $error")
          InternalServerError(error.message)
      }
    }
  }

  def submitPageNew(): Action[AnyContent] = enrolled.async { implicit request =>
    logger.info("----- completing submit page new -----------")
    confirmBusinessDetailsForm
        .bindFromRequest()
        .fold(
          formWithErrors =>
              BadRequest(confirmBusinessDetailsPage(formWithErrors, false)).toFuture
          ,
          form =>
            if (form.value == "yes") {
              Redirect(routes.UndertakingController.getAboutUndertaking.url).toFuture
            }
              else Redirect(routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPageNew().url)).toFuture
        )
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
              escService
                .getBeneficiaryIDValidation(request.eoriNumber.toString, "U", None)
                .flatMap {
                  case Right(Some(resp)) =>
                    logger.info(s"Beneficiary ID Response = $resp")
                    validateBeneficiaries(resp)
                  case Right(None) =>
                    logger.info("No Beneficiary ID Response from UTID validation.")
                    Redirect(routes.BenNotificationController.showPage()).toFuture // need to update
                  case Left(error) =>
                    logger.error(s"Error while calling Beneficiary ID validation = $error")
                    Redirect(routes.BenNotificationController.showPage()).toFuture // need to update
                }
              Redirect(routes.BenNotificationController.showPage()).toFuture
            } else
              Redirect(
                routes.HMRCEmailController.showPage(routes.ConfirmBusinessDetailsController.showPage().url)
              ).toFuture
        )
    }
  }

  private def validateBeneficiaries(resp: BeneficiaryIDResponse)(implicit hc: HeaderCarrier): Future[Result] = {
    val beneficiaries: Seq[BeneficiaryInfoResp] = resp.beneficiaryInfo.getOrElse(Seq.empty)
    beneficiaries.foreach { beneficiary =>
      logger.info(
        s"Beneficiary record: eori=${beneficiary.eori}, validated=${beneficiary.validated}, benName=${beneficiary.benName}, benIDType=${beneficiary.benIDType}, benIDValue=${beneficiary.benIDValue}"
      )
    }
    val beneficiariesToValidate: Seq[(String, BeneficiaryInfoResp)] =
      beneficiaries.flatMap { beneficiary =>
        beneficiary.eori.collect {
          case eori if beneficiary.validated.contains(false) =>
            eori -> beneficiary
        }
      }
    logger.info(s"Beneficiaries requiring validation count = ${beneficiariesToValidate.size}")

    if (beneficiariesToValidate.isEmpty) {
      logger.info("No unvalidated beneficiaries found. Redirecting to notification page.")
      Redirect(routes.BenNotificationController.showPage()).toFuture
    } else {
      val validationCalls: Seq[Future[Either[ConnectorError, Option[BeneficiaryIDResponse]]]] =
        beneficiariesToValidate.map { case (eori, beneficiary) =>
          logger.info(s"Calling EORI validation service for eori = $eori")
          escService.getBeneficiaryIDValidation(
            id = eori,
            idType = "E",
            beneficiaryInfo = Some(
              BeneficiaryInfo(
                benName = beneficiary.benName,
                benIDType = beneficiary.benIDType,
                benIDValue = beneficiary.benIDValue
              )
            )
          )
        }

      Future.sequence(validationCalls).map { results =>
        logger.info(s"EORI validation results = $results")
        val allCallsSuccessful: Boolean =
          results.forall {
            case Right(Some(_)) =>
              true
            case Right(None) =>
              false
            case Left(error) =>
              logger.error(s"EORI validation failed with error = $error")
              false
          }
        if (allCallsSuccessful) {
          logger.info("All EORI validation calls completed successfully.")
          Redirect(routes.BenNotificationController.showPage())
        } else {
          logger.error("One or more EORI validation calls failed. Redirecting to contact page.")
          Redirect(routes.BenNotificationController.showPage()) // need to update
        }
      }
    }
  }

}
