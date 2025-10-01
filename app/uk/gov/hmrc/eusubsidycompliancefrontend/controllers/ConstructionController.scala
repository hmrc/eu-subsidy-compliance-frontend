/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.construction.CivilEngineeringLvl3Page

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class ConstructionController @Inject() (
                                             mcc: MessagesControllerComponents,
                                             actionBuilders: ActionBuilders,
                                             civilEngineeringLvl3Page: CivilEngineeringLvl3Page
                                           )(implicit
                                             val appConfig: AppConfig
                                           ) extends BaseController(mcc){

  import actionBuilders._
  override val messagesApi: MessagesApi = mcc.messagesApi

  private val undertakingSectorForm: Form[FormValues] = formWithSingleMandatoryField("construction")

  def loadPage(isUpdate : Boolean = false, userAnswer : String) : Action[AnyContent] = enrolled.async { implicit request =>
    userAnswer match {
      case "42" => Ok(civilEngineeringLvl3Page(undertakingSectorForm, isUpdate)).toFuture
    }
  }

  //submitPage()

}