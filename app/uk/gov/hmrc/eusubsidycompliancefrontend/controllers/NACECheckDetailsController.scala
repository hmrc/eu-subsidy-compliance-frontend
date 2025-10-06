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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.ConfirmDetailsPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NACECheckDetailsController @Inject()(
                                            mcc: MessagesControllerComponents,
                                            naceCYAView: ConfirmDetailsPage
                                          )(implicit ec: ExecutionContext, appConfig: AppConfig) extends FrontendController(mcc) {

  def getCheckDetails: Action[AnyContent] = Action { implicit request =>
    // Placeholder values for rendering
    val sector = Sector.agriculture
    val naceLevel1 = "User's selection of L1"
    val naceLevel2 = "User's radio selection of L2"
    val naceLevel3 = "User's radio selection of L3"
    val naceLevel4 = "User's radio selection of L4"
    val previous = routes.UndertakingController.getUndertakingHome

    Ok(naceCYAView(sector, naceLevel1, naceLevel2, naceLevel3, naceLevel4, previous))
  }
}