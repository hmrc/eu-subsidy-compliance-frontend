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

import cats.implicits.catsSyntaxOptionId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{BusinessEntityJourney, EscService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

@Singleton
class NoBusinessPresentController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  store: Store,
  override val escService: EscService,
  noBusinessPresentPage: NoBusinessPresentPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {
  import escCDSActionBuilder._

  def getNoBusinessPresent: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      val previous = routes.AccountController.getAccountPage().url
      Ok(noBusinessPresentPage(undertaking.name, previous)).toFuture
    }
  }

  def postNoBusinessPresent: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      for {
        _ <- store.update[BusinessEntityJourney](_.copy(isLeadSelectJourney = true.some))
      } yield Redirect(routes.BusinessEntityController.getAddBusinessEntity())
    }

  }

}
