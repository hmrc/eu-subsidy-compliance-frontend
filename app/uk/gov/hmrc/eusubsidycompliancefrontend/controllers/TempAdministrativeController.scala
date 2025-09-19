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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.administrative.AdministrativeLvl2Page
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TempAdministrativeController @Inject()(
                                              mcc: MessagesControllerComponents,
                                              administrativeLvl2Page: AdministrativeLvl2Page
                                            )(implicit appConfig: AppConfig) extends BaseController(mcc) {

  private val tempForm = Form(
    single("undertakingSector" -> optional(text))
  )

  def showAdministrative(): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(administrativeLvl2Page(
        form = tempForm,
        previous = "/temp-back-link", // Use plain String instead of Journey.Uri
        undertakingName = "Test Undertaking",
        isUpdate = false
      ))
    )
  }

  def submitAdministrative(): Action[AnyContent] = Action.async { implicit request =>
    tempForm.bindFromRequest().fold(
      formWithErrors => Future.successful(
        BadRequest(administrativeLvl2Page(
          form = formWithErrors,
          previous = "/temp-back-link", // Use plain String
          undertakingName = "Test Undertaking",
          isUpdate = false
        ))
      ),
      validData => Future.successful(
        Ok(s"Form submitted with value: ${validData.getOrElse("No selection")}")
      )
    )
  }
}