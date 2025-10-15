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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.NaceUndertakingCategoryIntroPage

import javax.inject.Inject
import scala.concurrent.Future

class NaceUndertakingCategoryIntroController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  NaceUndertakingCategoryIntroPage: NaceUndertakingCategoryIntroPage
)(implicit
  val appConfig: AppConfig
) extends BaseController(mcc) {

  import actionBuilders._

  def showPage: Action[AnyContent] = enrolled.async { implicit request =>
    val eori = request.eoriNumber
    Future.successful(Ok(NaceUndertakingCategoryIntroPage(eori)))
  }

  def continue: Action[AnyContent] = enrolled.async { implicit request =>
    Future.successful(
      // Needs updating to take user to /undertaking-industry-sector
      Redirect(routes.UndertakingController.getSector)
    )
  }
}
