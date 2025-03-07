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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions, Enrolments, InternalError}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders.EscActionBuilder.EccEnrolmentKey
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Action builder that runs the supplied block only if the user is authenticated with GG and is not enrolled for this
  * service in ECC.
  *
  * If they are enrolled we redirect to / where the AccountController will figure out where to send the user according
  * to their current state.
  *
  * See AccountController.getAccountPage
  *
  * @param config
  * @param env
  * @param authConnector
  * @param mcc
  * @param executionContext
  * @param appConfig
  */
class NotEnrolledActionBuilder @Inject() (
  override val authConnector: AuthConnector,
  mcc: ControllerComponents
)(implicit val executionContext: ExecutionContext, appConfig: AppConfig)
    extends ActionBuilder[AuthenticatedRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with Results
    with AuthorisedFunctions
    with I18nSupport
    with EscActionBuilder {

  override val messagesApi: MessagesApi = mcc.messagesApi
  override val parser: BodyParser[AnyContent] = mcc.parsers.anyContent

  private val retrievals =
    Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments and Retrievals.affinityGroup

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedRequest[A] => Future[Result]
  ): Future[Result] =
    authorised()
      .retrieve[Option[Credentials] ~ Option[String] ~ Enrolments ~ Option[AffinityGroup]](retrievals) {
        case Some(_) ~ Some(_) ~ _ ~ Some(affinityGroup) if affinityGroup == Agent =>
          Redirect(routes.AgentNotAllowedController.showPage.url).toFuture
        case Some(credentials) ~ Some(groupId) ~ enrolments ~ Some(_) =>
          enrolments
            .getEnrolment(EccEnrolmentKey)
            // Execute the block if the user is not enrolled...
            .fold(block(AuthenticatedRequest(credentials.providerId, groupId, request))) { _ =>
              // ...otherwise redirect to account home which will figure out where to send the user.
              Redirect(routes.AccountController.getAccountPage).toFuture
            }
        case _ ~ _ => Future.failed(throw InternalError())
      }(hc(request), executionContext)
      .recover(handleFailure(request, appConfig))

}
