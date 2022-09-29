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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import com.google.inject.Inject
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.mvc.{AnyContent, Call}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

// TODO - review public apis here
// TODO - fix relative/abs url handling
@Singleton
class EmailVerificationService @Inject() (
   emailVerificationConnector: EmailVerificationConnector,
   eoriEmailDatastore: EoriEmailDatastore,
   servicesConfig: ServicesConfig
) extends Logging {

  lazy private val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")

  lazy val useAbsoluteUrls: Boolean = emailVerificationBaseUrl.contains("localhost")

  private def verifyEmail(verifyEmailUrl: String, confirmEmailUrl: String)(credId: String, email: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EmailVerificationResponse]] = {
    emailVerificationConnector
      .verifyEmail(
        EmailVerificationRequest(
          credId = credId,
          continueUrl =
            if(useAbsoluteUrls) verifyEmailUrl
            else verifyEmailUrl,
          origin = "EU Subsidy Compliance",
          deskproServiceName = None,
          accessibilityStatementUrl = "",
          email = Some(Email(
            address = email,
            enterUrl = ""
          )),
          lang = None,
          backUrl =
            if(useAbsoluteUrls) Some(confirmEmailUrl)
            else Some(confirmEmailUrl),
          pageTitle = None
        )
      ).map {
      case Left(error) => throw ConnectorError(s"Error sending email:Id", error)
      case Right(value) =>
        value.status match {
          case CREATED => Some(value.json.as[EmailVerificationResponse])
          case _ => None
        }
    }
  }

  // TODO - fold?
  private def emailVerificationRedirect(confirmEmailCall: Call)(verifyEmailResponse: Option[EmailVerificationResponse]) = verifyEmailResponse match {
    case Some(value) => Redirect(emailVerificationConnector.getVerificationJourney(value.redirectUri))
    case None => Redirect(confirmEmailCall)
  }

  def getEmailVerification(eori: EORI): Future[Option[VerifiedEmail]] = eoriEmailDatastore.getEmailVerification(eori)

  private def verifyEori(eori: EORI): Future[CacheItem] = eoriEmailDatastore.verifyEmail(eori)

  // TODO - this exposes mongo internals - return a boolean instead?
  def approveVerificationRequest(key: EORI, verificationId: String): Future[UpdateResult] =
    eoriEmailDatastore.approveVerificationRequest(key, verificationId)

  private def addVerificationRequest(key: EORI, email: String)(implicit ec: ExecutionContext): Future[String] = {
    val verificationId = UUID.randomUUID().toString
    eoriEmailDatastore
      .addVerificationRequest(key, email, verificationId)
      .map {
        // TODO - review this error string
        _.fold(throw new IllegalArgumentException("Error storing email verification request"))(_.verificationId)
      }
  }

  // Add an email address that's already approved
  def addVerifiedEmail(eori: EORI, emailAddress: String)(implicit ec: ExecutionContext): Future[Unit] =
    for {
      _ <- addVerificationRequest(eori, emailAddress)
      _ <- verifyEori(eori)
    } yield ()


  def makeVerificationRequestAndRedirect(email: String, previousPage: Call, nextPageUrl: String => String,
  )(implicit request: AuthenticatedEnrolledRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier) = for {
    verificationId <- addVerificationRequest(request.eoriNumber, email)
    verificationResponse <- verifyEmail(nextPageUrl(verificationId), previousPage.url)(request.authorityId, email)
  } yield emailVerificationRedirect(previousPage)(verificationResponse)

}
