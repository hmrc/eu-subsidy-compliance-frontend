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
import uk.gov.hmrc.eusubsidycompliancefrontend.logging.TracedLogging
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax.FutureToOptionTOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.RequestSyntax.RequestOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
  emailVerificationConnector: EmailVerificationConnector,
  escService: EscService,
  servicesConfig: ServicesConfig
) extends TracedLogging {

  def getEmailVerification(
    eori: EORI
  )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[VerifiedEmail]] =
    escService.getEmailVerification(eori)

  def approveVerificationRequest(key: EORI, verificationId: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Boolean] =
    escService
      .approveEmailByVerificationId(key, verificationId)
      .map(_ => true)

  // Add an email address that's already approved
  def addVerifiedEmail(eori: EORI, emailAddress: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Boolean] =
    for {
      _ <- addVerificationRequest(eori, emailAddress)
      _ <- verifyEmailForEori(eori)
    } yield true

  private def addVerificationRequest(key: EORI, email: String)(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[String] =
    escService
      .startVerification(key, email)
      .toContext
      .map(_.verificationId)
      .getOrElse(throw new IllegalStateException("Error storing email verification request"))

  private def verifyEmailForEori(eori: EORI)(implicit
    hc: HeaderCarrier
  ): Future[VerifiedEmail] =
    escService.approveEmailByEori(eori)

  def makeVerificationRequestAndRedirect(
    email: String,
    previousPage: Call,
    nextPageUrl: String => String
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Result] = {
    val credId = request.authorityId
    for {
      verificationId <- addVerificationRequest(request.eoriNumber, email)
      verificationResponse <- verifyEmail(
        verifyEmailUrl = nextPageUrl(verificationId),
        confirmEmailUrl = previousPage.url,
        credId = credId,
        email = email
      )
    } yield verificationResponse.fold {
      Redirect(previousPage)
    }(value => Redirect(generateEmailServiceUrl(value.redirectUri)))
  }

  private def verifyEmail(verifyEmailUrl: String, confirmEmailUrl: String, credId: String, email: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: AuthenticatedEnrolledRequest[AnyContent]
  ): Future[Option[EmailVerificationResponse]] = {
    val redirectVerifyEmailUrl = request.toRedirectTarget(verifyEmailUrl)
    val backUrl = request.toRedirectTarget(confirmEmailUrl)

    println(s"backUrl $backUrl")

    emailVerificationConnector
      .verifyEmail(
        EmailVerificationRequest.createVerifyEmailRequest(
          credId = credId,
          redirectVerifyEmailUrl = redirectVerifyEmailUrl,
          email = email,
          backUrl = backUrl
        )
      )
      .map {
        case Left(error) =>
          println(s"$error error")
          throw ConnectorError(s"Error sending email:Id", error)
        case Right(value) =>
          value.status match {
            case CREATED => Some(value.json.as[EmailVerificationResponse])
            case _ => None
          }
      }
  }

  private def generateEmailServiceUrl[A](redirectUrl: String)(implicit req: WrappedRequest[A]): String =
    if (req.isLocal) servicesConfig.baseUrl("email-verification-frontend") + redirectUrl
    else redirectUrl

}
