/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.util.{ReportReminderHelpers, TimeProvider}

import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.UndertakingInvalidSectorSuspendedPage

import javax.inject.Inject
import scala.concurrent.Future

class UndertakingInvalidSectorSuspendedPageController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  undertakingSuspendedPage: UndertakingInvalidSectorSuspendedPage,
  timeProvider: TimeProvider,
  val store: Store

)(implicit val appConfig: AppConfig)
    extends BaseController(mcc) {

  import actionBuilders._



  def showPage: Action[AnyContent] = enrolled { implicit request =>
    val dueDate = ReportReminderHelpers.dueDateToReport(timeProvider.today).toString

    request.session.get("suspensionCode") match {
      case Some(code) =>
        Ok(undertakingSuspendedPage(code, dueDate))

      case None =>
        Redirect(routes.AccountController.getAccountPage)
    }
  }

  def continue: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.put[UndertakingJourney](UndertakingJourney())
    store.update[UndertakingJourney](_.copy(mode = appConfig.UpdateNaceMode))

    Future.successful(
      Redirect(routes.NaceUndertakingCategoryIntroController.showPage)
    )
  }

}
