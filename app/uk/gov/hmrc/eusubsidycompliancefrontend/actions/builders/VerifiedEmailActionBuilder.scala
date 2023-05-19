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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
  * Action builder that runs the supplied block only if the user
  *   o is authenticated with GG
  *   o is enrolled for this service in ECC
  *   o has a verified email address
  *
  * The first two conditions are checked by delegating to the EnrolledAction builder.
  *
  * If we are enrolled we then look for a verified email address which then determines if we run the block or not.
  *
  * @param config
  * @param env
  * @param authConnector
  * @param emailVerificationService
  * @param enrolledActionBuilder
  * @param mcc
  * @param executionContext
  */
class VerifiedEmailActionBuilder @Inject() (
  override val config: Configuration,
  override val env: Environment,
  override val authConnector: AuthConnector,
  emailVerificationService: EmailVerificationService,
  enrolledActionBuilder: EnrolledActionBuilder,
  mcc: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[AuthenticatedEnrolledRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with Results
    with AuthRedirects
    with AuthorisedFunctions
    with I18nSupport {

  override val messagesApi: MessagesApi = mcc.messagesApi
  override val parser: BodyParser[AnyContent] = mcc.parsers.anyContent

  // Delegates to EnrolledActionBuilder to handle ECC Enrolment check.
  override def invokeBlock[A](r: Request[A], f: AuthenticatedEnrolledRequest[A] => Future[Result]): Future[Result] =
    enrolledActionBuilder.invokeBlock(
      r,
      { enrolledRequest: AuthenticatedEnrolledRequest[A] =>
        emailVerificationService
          .getEmailVerification(enrolledRequest.eoriNumber)
          .toContext
          .foldF(throw new IllegalStateException("No verified email address found"))(_ => f(enrolledRequest))
      }
    )
}
