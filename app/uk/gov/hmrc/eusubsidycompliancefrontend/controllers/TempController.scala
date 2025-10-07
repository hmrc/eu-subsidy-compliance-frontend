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
import play.api.data.Forms._
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.realestate.FeeContractLvl4Page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TempController @Inject()(
                                mcc: MessagesControllerComponents,
                                realEstateLvl3Page: FeeContractLvl4Page
                              )(implicit val appConfig: AppConfig, ec: ExecutionContext) extends MessagesBaseController {

  override def controllerComponents: MessagesControllerComponents = mcc

  def tempRealEstateLvl3(): Action[AnyContent] = Action { implicit request =>
    Ok(realEstateLvl3Page(form = Form(mapping("realEstate3" -> text)(identity)(Some(_))), isUpdate = false))
  }
}