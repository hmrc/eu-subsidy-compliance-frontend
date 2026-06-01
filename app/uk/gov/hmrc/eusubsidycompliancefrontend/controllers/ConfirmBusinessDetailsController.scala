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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmBusinessDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.ConfirmMultipleBusinessDetailsPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmBusinessDetailsController @Inject() (
                                                   mcc: MessagesControllerComponents,
                                                   actionBuilders: ActionBuilders,
                                                   confirmBusinessDetailsPage: ConfirmBusinessDetailsPage,
                                                   confirmMultipleBusinessDetailsPage: ConfirmMultipleBusinessDetailsPage
                                                 )(implicit
                                                   val appConfig: AppConfig,
                                                   val executionContext: ExecutionContext
                                                 ) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmBusinessDetailsForm: Form[FormValues] =
    formWithSingleMandatoryField("confirmBusinessDetails")

  private def multipleEoris: Boolean = true // placeholder
  private def isSuspended: Boolean = true  // placeholder

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    if (multipleEoris) {
      Ok(confirmMultipleBusinessDetailsPage(confirmBusinessDetailsForm, isSuspended)).toFuture
    } else {
      Ok(confirmBusinessDetailsPage(confirmBusinessDetailsForm, isSuspended)).toFuture
    }
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    confirmBusinessDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          if (multipleEoris) {
            BadRequest(confirmMultipleBusinessDetailsPage(formWithErrors, isSuspended)).toFuture
          } else {
            BadRequest(confirmBusinessDetailsPage(formWithErrors, isSuspended)).toFuture
          },
        _ =>
          Redirect(routes.ConfirmBusinessDetailsController.showPage).toFuture
      )
  }
}