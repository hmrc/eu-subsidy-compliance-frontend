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

import cats.implicits.catsSyntaxOptionId
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NoBusinessPresentController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  override val escService: EscService,
  noBusinessPresentPage: NoBusinessPresentPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import actionBuilders._

  def getNoBusinessPresent: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      logger.info("NoBusinessPresentController.getNoBusinessPresent showing noBusinessPresentPage")

      Ok(noBusinessPresentPage(routes.AccountController.getAccountPage.url)).toFuture
    }
  }

  def postNoBusinessPresent: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("NoBusinessPresentController.postNoBusinessPresent")

    withLeadUndertaking { _ =>
      store
        .update[BusinessEntityJourney](_.copy(isLeadSelectJourney = true.some))
        .map { businessEntityJourney: BusinessEntityJourney =>
          logger.info(
            s"NoBusinessPresentController.postNoBusinessPresent redirecting to businessEntityJourney eori:${businessEntityJourney.eori}"
          )

          Redirect(routes.AddBusinessEntityController.getAddBusinessEntity())
        }
    }
  }

}
