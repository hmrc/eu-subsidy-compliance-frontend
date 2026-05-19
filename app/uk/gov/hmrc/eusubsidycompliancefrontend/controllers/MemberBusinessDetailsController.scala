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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.BusinessEntity
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.MemberBusinessDetailsPage

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MemberBusinessDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  escService: EscService,
  memberBusinessDetailsPage: MemberBusinessDetailsPage
)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  def showPage: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    escService
      .retrieveUndertaking(eori)
      .toContext
      .foldF(
        // If no undertaking found, show page with stub data for visual testing
        // TODO: Replace stub data fallback with redirect once navigation is wired
        Future.successful(Ok(memberBusinessDetailsPage(
          BusinessEntity(eori, leadEORI = false),
          eori
        )))
      ) { undertaking =>
        val businessEntity = undertaking.getBusinessEntityByEORI(eori)
        Future.successful(Ok(memberBusinessDetailsPage(businessEntity, eori)))
      }
  }

}
