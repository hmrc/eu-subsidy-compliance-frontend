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

import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.NaceCyaPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TempCyaController @Inject() (
  mcc: MessagesControllerComponents,
  naceCyaPage: NaceCyaPage
)(implicit appConfig: AppConfig)
    extends FrontendController(mcc) {

  def showCya: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Ok(
        naceCyaPage(
          eori = EORI("GB123456789012"),
          industrySector = "General trade",
          naceLevel1 = "C Manufacturing",
          naceLevel2 = "10 Manufacture of food products",
          naceLevel3 = "10.8 Manufacture of other food products",
          naceLevel4 = "10.82 Manufacture of cocoa, chocolate and sugar confectionery",
          emailAddress = "someemail@mail.com",
          otherBusinesses = "Yes",
          previous = routes.TempCyaController.showCya.url
        )
      )
    )
  }

  def postCheckAnswers: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(
      Redirect(routes.TempCyaController.showCya)
        .flashing("success" -> "Form submitted successfully!")
    )
  }
}
