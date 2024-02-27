/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EligibilityFirstEmptyPageController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store
)(implicit
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with ControllerFormHelpers {
  import actionBuilders._

  def firstEmptyPage: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("EligibilityFirstEmptyPageController.firstEmptyPage")
    store
      .get[EligibilityJourney]
      .toContext
      .map(_.firstEmpty.getOrElse(Redirect(routes.UndertakingController.firstEmptyPage)))
      .getOrElse {
        logger.info(
          "EligibilityFirstEmptyPageController.firstEmptyPage redirecting to EligibilityEoriCheckController.getEoriCheck as nothing in journey store"
        )
        // If we get here it must be the first time we've hit the service with an enrolment since there is nothing
        // in the journey store. In this case we route the user to the eoriCheck page.
        Redirect(routes.EligibilityStartUndertakingJourneyController.startUndertakingJourney)
      }
  }

}
