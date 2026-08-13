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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.MemberNotificationJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.HowWeUseYourDataPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BeneficiaryNotificationController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  howWeUseYourDataPage: HowWeUseYourDataPage,
  store: Store
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val howWeUseForm = play.api.data.Form(play.api.data.Forms.single("continue" -> play.api.data.Forms.text))

  def showPage(): Action[AnyContent] = enrolled.async { implicit request =>
    Ok(howWeUseYourDataPage(howWeUseForm, routes.UndertakingController.getAddBusiness.url, "new")).toFuture
  }

  def submitPage(): Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[UndertakingJourney].flatMap {
      case Some(journey) if !journey.isSubmitted =>
        store.update[UndertakingJourney](_.setHowWeUseData(true)).map { _ =>
          Redirect(routes.UndertakingController.getCheckAnswers)
        }
      case _ =>
        store.put[MemberNotificationJourney](MemberNotificationJourney(seen = true)).flatMap { _ =>
          Redirect(routes.AccountController.getAccountPage).toFuture
        }
    }
  }
}
