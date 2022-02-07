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
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.retrieve.v2._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, _}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EscRequestActionBuilder @Inject() (
  val config: Configuration,
  val env: Environment,
  val authConnector: AuthConnector,
  mcc: ControllerComponents
)(implicit val executionContext: ExecutionContext, appConfig: AppConfig)
    extends ActionBuilder[EscAuthRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with Results
    with AuthRedirects
    with AuthorisedFunctions
    with I18nSupport {

  val logger: Logger = Logger(this.getClass.getName)

  val messagesApi: MessagesApi = mcc.messagesApi

  val parser: BodyParser[AnyContent] = mcc.parsers.anyContent

  override def invokeBlock[A](request: Request[A], block: EscAuthRequest[A] => Future[Result]): Future[Result] =
    authorised(Enrolment("HMRC-ESC-ORG"))
      .retrieve[Option[Credentials] ~ Option[String] ~ Enrolments](
        Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments
      ) {
        case Some(information) ~ Some(groupId) ~ enrolments =>
          enrolments
            .getEnrolment("HMRC-ESC-ORG")
            .map(x => x.getIdentifier("EORINumber"))
            .flatMap(y => y.map(z => z.value))
            .map(x => EORI(x)) match {
            case Some(eori) => block(EscAuthRequest(information.providerId, groupId, request, eori))
            case _          => throw new IllegalStateException("no eori provided")
          }
        case _ ~ _                                          => Future.failed(throw InternalError())
      }(hc(request), executionContext)
      .recover(handleFailure(request))

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession        =>
      Redirect(appConfig.ggSignInUrl, Map("continue" -> Seq(request.uri), "origin" -> Seq(origin)))
    case _: InsufficientEnrolments =>
      Redirect(appConfig.eccEscSubscribeUrl)
  }
}
