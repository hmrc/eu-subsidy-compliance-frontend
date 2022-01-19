/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.HelloWorldPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HelloWorldController @Inject()(
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage,
  escActionBuilders: EscActionBuilders
)(
  implicit val appConfig: AppConfig
) extends
  FrontendController(mcc)
  with I18nSupport
{
  import escActionBuilders._

  def helloWorld: Action[AnyContent] = escAuthentication.async { implicit request =>

    implicit val foo: EORI = request.eoriNumber

    // TODO in all but the first steps we have to get the model first
//    store.put(UndertakingJourneyModel(eori = foo))

    // TODO .. like so, here we pass in the copy function to update the underlying case class
//    store.update[UndertakingJourneyModel]({x => x.map(_.copy(eori = EORI("XI123456789013")))})

    Future.successful(Ok(helloWorldPage(foo)))
  }
}