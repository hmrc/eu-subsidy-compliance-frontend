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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EscRequestVerifiedEmailActionBuilder @Inject()(
                                                      val config: Configuration,
                                                      val env: Environment,
                                                      val authConnector: AuthConnector,
                                                      val emailVerificationService: EmailVerificationService,
                                                      mcc: ControllerComponents
  )(implicit val executionContext: ExecutionContext, appConfig: AppConfig)
    extends ActionBuilder[AuthenticatedEscRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with Results
    with AuthRedirects
    with AuthorisedFunctions
    with I18nSupport {

  private val EccEnrolmentKey = "HMRC-ESC-ORG"
  private val EnrolmentIdentifier = "EORINumber"

  val messagesApi: MessagesApi = mcc.messagesApi

  val parser: BodyParser[AnyContent] = mcc.parsers.anyContent

  override def invokeBlock[A](
    request: Request[A],
    block: AuthenticatedEscRequest[A] => Future[Result]
  ): Future[Result] =
    authorised()
      .retrieve[Option[Credentials] ~ Option[String] ~ Enrolments](
        Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments
      ) {
        case Some(credentials) ~ Some(groupId) ~ enrolments =>
          (enrolments.getEnrolment(EccEnrolmentKey)) match {
            case (Some(eccEnrolment)) =>
              val eori = eccEnrolment
                .getIdentifier(EnrolmentIdentifier)
                .fold(throw new IllegalStateException("No EORI against enrollment"))(e => EORI(e.value))
                emailVerificationService.getEmailVerification(eori).flatMap {
                  case Some(_) => block(AuthenticatedEscRequest(credentials.providerId, groupId, request, eori))
                  case None => throw new IllegalStateException("Email not valid")
                }
            case _ => Redirect(routes.EligibilityController.getCustomsWaivers()).toFuture
          }
        case _ ~ _ => Future.failed(throw InternalError())
      }(hc(request), executionContext)
      .recover(handleFailure(request))

  private def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      Redirect(appConfig.ggSignInUrl, Map("continue" -> Seq(request.uri), "origin" -> Seq(origin)))
    case _: InsufficientEnrolments => Redirect(appConfig.eccEscSubscribeUrl)
  }
}
