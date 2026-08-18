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
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.{BusinessEntityJourney, SubsidyJourney}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.NeedRegistrationNumberBusinessPage
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
@Singleton
class NeedRegistrationNumberBusinessController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  needRegistrationNumberBusinessPage: NeedRegistrationNumberBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc) {
  import actionBuilders._
  def showPage(previous: String): Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[BusinessEntityJourney].flatMap {
      case Some(journey) if journey.eori.value.isDefined =>
        Ok(needRegistrationNumberBusinessPage(journey.eori.value.get.toString, previous)).toFuture
      case _ =>
        store.get[SubsidyJourney].map {
          case Some(journey) if journey.addClaimEori.value.flatMap(_.value).isDefined =>
            Ok(needRegistrationNumberBusinessPage(journey.addClaimEori.value.flatMap(_.value).get, previous))
          case _ =>
            Ok(needRegistrationNumberBusinessPage(eori.toString, previous))
        }
    }
  }
}
