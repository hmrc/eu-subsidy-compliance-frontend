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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.ConfirmDetailsPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.{NaceLevel4, NaceLevel4Catalogue}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NACECheckDetailsController @Inject()(
                                            mcc: MessagesControllerComponents,
                                            store: Store,
                                            actionBuilders: ActionBuilders,
                                            naceCYAView: ConfirmDetailsPage
                                          )(implicit ec: ExecutionContext, appConfig: AppConfig) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmDetailsForm: Form[FormValues] = formWithSingleMandatoryField("confirmDetails")

  def getCheckDetails: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    store.get[UndertakingJourney].map { journeyOpt =>
      val sector = Sector.agriculture
      val naceLevel1 = "User's selection of L1"
      val naceLevel2 = "User's radio selection of L2"
      val naceLevel3 = "User's radio selection of L3"
      val naceLevel4 = "User's radio selection of L4"

      val naceLevel4Code: String = journeyOpt
        .flatMap(_.sector.value.map(_.toString))
        .getOrElse(throw new IllegalStateException("Missing Level 4 code in UndertakingJourney"))

      val naceLevel4Notes = NaceLevel4Catalogue.fromMessages(naceLevel4Code)
        .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceLevel4Code"))

      val previous = routes.AccountController.getAccountPage.url

      Ok(naceCYAView(
        confirmDetailsForm,
        sector,
        naceLevel1,
        naceLevel2,
        naceLevel3,
        naceLevel4,
        naceLevel4Notes,
        previous
      ))
    }
  }

  def postCheckDetails: Action[AnyContent] = Action { implicit request =>
    confirmDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val sector = Sector.agriculture
          val naceLevel1 = "User's selection of L1"
          val naceLevel2 = "User's radio selection of L2"
          val naceLevel3 = "User's radio selection of L3"
          val naceLevel4 = "User's radio selection of L4"

          val naceLvl4Notes: NaceLevel4 =
            NaceLevel4Catalogue
              .fromMessages(naceLevel4)
              .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceLevel4"))

          val previous = routes.AccountController.getAccountPage.url

          BadRequest(naceCYAView(formWithErrors, sector, naceLevel1, naceLevel2, naceLevel3, naceLevel4, naceLvl4Notes, previous))
        },
        form => {
          // currently just reloads page upon successful submission
          Redirect(routes.NACECheckDetailsController.getCheckDetails)
        }
      )
  }
}