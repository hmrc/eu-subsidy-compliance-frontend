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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import com.google.inject.Inject
import play.api.http.Status.CREATED
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Result, WrappedRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.EoriEmailRepository
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureOptionToOptionTOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.RequestSyntax.RequestOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.i18n.Messages
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.VerificationStatus

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
  emailVerificationConnector: EmailVerificationConnector,
  eoriEmailDatastore: EoriEmailRepository,
  servicesConfig: ServicesConfig
) extends TracedLogging {

  def getEmailVerificationStatus(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[VerificationStatus]] =
    emailVerificationConnector.getVerificationStatus(request.authorityId).map { response =>
      response.emails.find(e => e.verified == true && e.locked == false)
    }
  def getEmailVerification(eori: EORI): Future[Option[VerifiedEmail]] = eoriEmailDatastore.getEmailVerification(eori)

  def approveVerificationRequest(key: EORI, verificationId: String)(implicit ec: ExecutionContext): Future[Boolean] =
    eoriEmailDatastore
      .approveVerificationRequest(key, verificationId)
      .map(_.getMatchedCount > 0)

  // Add an email address that's already approved
  def addVerifiedEmail(eori: EORI)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      _ <- addVerificationRequest(eori)
      _ <- verifyEmailForEori(eori)
    } yield ()

  def makeVerificationRequestAndRedirect(
    email: String,
    previousPage: String,
    nextPageUrl: String => String,
    reEnterEmailUrl: String = routes.UndertakingController.getConfirmEmail.url
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    ec: ExecutionContext,
    hc: HeaderCarrier,
    messages: Messages
  ): Future[Result] = for {
    verificationId <- addVerificationRequest(request.eoriNumber)
    verificationResponse <- verifyEmail(
      continueUrl = nextPageUrl(verificationId),
      backUrl = previousPage,
      reEnterEmailUrl = reEnterEmailUrl
    )(
      request.authorityId,
      email
    )
  } yield verificationResponse.fold(Redirect(previousPage))(value =>
    Redirect(generateEmailServiceUrl(value.redirectUri))
  )

  private def verifyEmail(
    continueUrl: String,
    backUrl: String,
    reEnterEmailUrl: String
  )(credId: String, email: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: AuthenticatedEnrolledRequest[AnyContent],
    messages: Messages
  ): Future[Option[EmailVerificationResponse]] =
    emailVerificationConnector
      .verifyEmail(
        EmailVerificationRequest(
          credId = credId,
          continueUrl = request.toRedirectTarget(continueUrl),
          origin = "EU Subsidy Compliance",
          deskproServiceName = None,
          accessibilityStatementUrl = servicesConfig.getString("verify-email.accessibility-statement.url"),
          email = Some(
            Email(
              address = email,
              enterUrl = request.toRedirectTarget(reEnterEmailUrl)
            )
          ),
          lang = None,
          backUrl = Some(request.toRedirectTarget(backUrl)),
          pageTitle = Some(messages("service.name"))
        )
      )
      .map {
        case Left(error) =>
          throw ConnectorError(s"Error sending email:Id", error)
        case Right(value) =>
          value.status match {
            case CREATED => Some(value.json.as[EmailVerificationResponse])
            case _ => None
          }
      }

  private def generateEmailServiceUrl[A](redirectUrl: String)(implicit req: WrappedRequest[A]): String =
    if (req.isLocal) servicesConfig.baseUrl("email-verification-frontend") + redirectUrl
    else redirectUrl

  private def verifyEmailForEori(eori: EORI): Future[CacheItem] = eoriEmailDatastore.verifyEmail(eori)

  private def addVerificationRequest(key: EORI)(implicit ec: ExecutionContext): Future[String] =
    eoriEmailDatastore
      .addVerificationRequest(key, UUID.randomUUID().toString)
      .toContext
      .map(_.verificationId)
      .getOrElse(throw new IllegalStateException("Error storing email verification request"))

}
