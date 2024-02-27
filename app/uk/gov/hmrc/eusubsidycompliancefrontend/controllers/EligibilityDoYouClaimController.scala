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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.EligibilityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.RequestSyntax.RequestOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.DoYouClaimPage

@Singleton
class EligibilityDoYouClaimController @Inject() (
  mcc: MessagesControllerComponents,
  doYouClaimPage: DoYouClaimPage,
  actionBuilders: ActionBuilders,
  override val store: Store
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with ControllerFormHelpers {
  import actionBuilders._
  private val doYouClaimUrl = routes.EligibilityDoYouClaimController.getDoYouClaim.url
  private val willYouClaimUrl = routes.EligibilityWillYouClaimController.getWillYouClaim.url

  private val doYouClaimForm = formWithSingleMandatoryField("customswaivers")

  def getDoYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    logger.info("EligibilityDoYouClaimController.getDoYouClaim")
    Ok(doYouClaimPage(doYouClaimForm)).toFuture
  }

  def postDoYouClaim: Action[AnyContent] = notEnrolled.async { implicit request =>
    logger.info("EligibilityDoYouClaimController.postDoYouClaim")
    doYouClaimForm
      .bindFromRequest()
      .fold(
        errors => BadRequest(doYouClaimPage(errors)).toFuture,
        form =>
          if (form.value.isTrue) checkEnrolment
          else
            EligibilityJourney().withDoYouClaim(false).next
      )
  }

  private def checkEnrolment(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    if (request.isFrom(doYouClaimUrl) || request.isFrom(willYouClaimUrl))
      Redirect(appConfig.eccEscSubscribeUrl).toFuture
    else
      Redirect(routes.EligibilityEoriCheckController.getEoriCheck.url).toFuture

}
