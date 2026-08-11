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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BeneficiaryIDRequest, BeneficiaryInfo, BusinessEntity, FormValues}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmAddBusinessDetailsPage
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmAddBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  escService: EscService,
  store: Store,
  confirmAddBusinessDetailsPage: ConfirmAddBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {
  import actionBuilders._

  private val confirmAddBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("confirmAddBusinessDetails")

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI.EORI = request.eoriNumber
    store.get[BusinessEntityJourney].flatMap {
      case Some(journey) if journey.eori.value.isDefined =>
        val businessEori = journey.eori.value.get
        escService
          .beneficiaryIDValidate(
            BeneficiaryIDRequest(
              idType = "EORI",
              idValue = businessEori.toString,
              requestType = "R",
              beneficiaryInfo = None
            )
          )
          .map {
            case Right(Some(resp)) =>
              Ok(confirmAddBusinessDetailsPage(confirmAddBusinessDetailsForm, Some(resp)))
            case _ =>
              Ok(confirmAddBusinessDetailsPage(confirmAddBusinessDetailsForm, None))
          }
      case _ =>
        Ok(confirmAddBusinessDetailsPage(confirmAddBusinessDetailsForm, None)).toFuture
    }
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI.EORI = request.eoriNumber
    confirmAddBusinessDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(confirmAddBusinessDetailsPage(formWithErrors)).toFuture,
        {
          case FormValues("yes") =>
            store.get[BusinessEntityJourney].flatMap {
              case Some(journey) =>
                journey.eori.value match {
                  case Some(businessEori) =>
                    escService.getUndertaking(eori).flatMap { undertaking =>
                      val businessEntity = BusinessEntity(businessEori, leadEORI = false)
                      for {
                        _ <- escService.addMember(undertaking.reference, businessEntity)

                        beneficiaryResponse <- escService.beneficiaryIDValidate(
                          BeneficiaryIDRequest(
                            idType = "EORI",
                            idValue = businessEori.toString,
                            requestType = "R",
                            beneficiaryInfo = None
                          )
                        )

                        validationResponse <- beneficiaryResponse match {
                          case Right(Some(resp)) =>
                            val beneficiaryInfo =
                              resp.beneficiaryInfo
                                .flatMap(_.find(_.eori.contains(businessEori)))
                                .map { beneficiary =>
                                  BeneficiaryInfo(
                                    benName = beneficiary.benName,
                                    benIDType = beneficiary.benIDType,
                                    benIDValue = beneficiary.benIDValue
                                  )
                                }

                            escService.getBeneficiaryIDValidation(
                              businessEori.toString,
                              "E",
                              beneficiaryInfo
                            )

                          case Right(None) =>
                            Redirect(
                              routes.HMRCEmailController
                                .showPage(routes.AddBusinessEntityController.getAddBusinessEntity().url)
                            ).toFuture

                          case Left(error) =>
                            Redirect(
                              routes.HMRCEmailController
                                .showPage(routes.AddBusinessEntityController.getAddBusinessEntity().url)
                            ).toFuture
                        }

                        result <- validationResponse match {
                          case Right(Some(resp)) =>
                            escService.validateBeneficiaries(resp).map { isValid =>
                              if (isValid) {
                                Redirect(
                                  routes.AddBusinessEntityController.getAddBusinessEntity(businessAdded = Some(true))
                                )
                              } else {
                                Redirect(
                                  routes.HMRCEmailController.showPage(
                                    routes.AddBusinessEntityController
                                      .getAddBusinessEntity()
                                      .url
                                  )
                                )
                              }
                            }

                          case Right(None) =>
                            Redirect(
                              routes.HMRCEmailController.showPage(
                                routes.AddBusinessEntityController
                                  .getAddBusinessEntity()
                                  .url
                              )
                            ).toFuture

                          case Left(error) =>
                            logger.error(s"Validation failed: $error")

                            Redirect(
                              routes.HMRCEmailController.showPage(
                                routes.AddBusinessEntityController
                                  .getAddBusinessEntity()
                                  .url
                              )
                            ).toFuture
                        }

                      } yield result
                      // add member need to validate and redirect
                      // escService.addMember(undertaking.reference, businessEntity).map { _ =>
                      // Redirect(routes.AddBusinessEntityController.getAddBusinessEntity(businessAdded = Some(true)))
                      // }
                    }
                  case None =>
                    Redirect(
                      routes.AddBusinessEntityController.getAddBusinessEntity(businessAdded = Some(true))
                    ).toFuture
                }
              case None =>
                Redirect(routes.AddBusinessEntityController.getAddBusinessEntity(businessAdded = Some(true))).toFuture
            }

          case FormValues("no") =>
            Redirect(
              routes.HMRCEmailController.showPage(
                routes.ConfirmAddBusinessDetailsController.showPage().url
              )
            ).toFuture
        }
      )
  }
}
