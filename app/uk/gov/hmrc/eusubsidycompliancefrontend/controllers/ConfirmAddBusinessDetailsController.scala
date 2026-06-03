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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmAddBusinessDetailsPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmAddBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  confirmAddBusinessDetailsPage: ConfirmAddBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmAddBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("confirmAddBusinessDetails")

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(confirmAddBusinessDetailsPage(confirmAddBusinessDetailsForm)).toFuture
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    confirmAddBusinessDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(confirmAddBusinessDetailsPage(formWithErrors)).toFuture,
        _ => Redirect(routes.ConfirmAddBusinessDetailsController.showPage).toFuture
      )
  }
}
