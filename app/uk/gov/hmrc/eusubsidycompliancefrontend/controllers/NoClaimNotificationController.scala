/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NoClaimNotificationController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  escService: EscService,
  noClaimNotificationPage: NoClaimNotificationPage,
  noClaimConfirmationPage: NoClaimConfirmationPage
)(implicit val appConfig: AppConfig, executionContext: ExecutionContext)
    extends BaseController(mcc) {
  import escActionBuilders._

  def getNoClaimNotification: Action[AnyContent] = escAuthentication.async { implicit request =>
    val eori = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield undertakingOpt match {
      case Some(undertaking) =>
        Ok(noClaimNotificationPage(noClaimForm, previous, undertaking.name))
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postNoClaimNotification: Action[AnyContent] = escAuthentication.async { implicit request =>
    val eori = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield undertakingOpt match {
      case Some(undertaking) =>
        noClaimForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(noClaimNotificationPage(errors, previous, undertaking.name)),
            _ => Redirect(routes.NoClaimNotificationController.getNoClaimConfirmation())
          )
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def getNoClaimConfirmation: Action[AnyContent] = escAuthentication.async { implicit request =>
    val eori = request.eoriNumber
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield undertakingOpt match {
      case Some(undertaking) => Ok(noClaimConfirmationPage(undertaking.name))
      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  lazy val noClaimForm: Form[FormValues] = Form(
    mapping("noClaimNotification" -> mandatory("noClaimNotification"))(FormValues.apply)(FormValues.unapply)
  )

}
