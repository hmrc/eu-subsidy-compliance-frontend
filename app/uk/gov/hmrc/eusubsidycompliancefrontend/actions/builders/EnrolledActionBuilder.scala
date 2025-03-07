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
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders.EscActionBuilder.{EccEnrolmentIdentifier, EccEnrolmentKey}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Action builder that runs the supplied block only if the user is authenticated with GG and already enrolled for this
  * service in ECC.
  *
  * If there is no enrolment we redirect the user to the first page of the new user journey since we can only assume
  * that they are a new user to the service.
  *
  * @param config
  * @param env
  * @param authConnector
  * @param mcc
  * @param executionContext
  * @param appConfig
  */
class EnrolledActionBuilder @Inject() (
  override val authConnector: AuthConnector,
  mcc: ControllerComponents
)(implicit val executionContext: ExecutionContext, appConfig: AppConfig)
    extends ActionBuilder[AuthenticatedEnrolledRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with Results
    with AuthorisedFunctions
    with I18nSupport
    with EscActionBuilder {

  override val messagesApi: MessagesApi = mcc.messagesApi
  override val parser: BodyParser[AnyContent] = mcc.parsers.anyContent

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedEnrolledRequest[A] => Future[Result]
  ): Future[Result] =
    authorised()
      .retrieve[Option[Credentials] ~ Option[String] ~ Enrolments ~ Option[AffinityGroup]](
        Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments and Retrievals.affinityGroup
      ) {
        case Some(_) ~ Some(_) ~ _ ~ Some(affinityGroup) if affinityGroup == Agent =>
          Redirect(routes.AgentNotAllowedController.showPage.url).toFuture
        case Some(credentials) ~ Some(groupId) ~ enrolments ~ Some(_) =>
          enrolments.getEnrolment(EccEnrolmentKey) match {
            case Some(eccEnrolment) =>
              val identifier: String = eccEnrolment
                .getIdentifier(EccEnrolmentIdentifier)
                .fold(throw new IllegalStateException("no eori provided"))(_.value)
              block(AuthenticatedEnrolledRequest(credentials.providerId, groupId, request, EORI(identifier)))
            case _ => Redirect(routes.EligibilityDoYouClaimController.getDoYouClaim.url).toFuture
          }
        case _ ~ _ => Future.failed(throw InternalError())
      }(hc(request), executionContext)
      .recover(handleFailure(request, appConfig))

}
