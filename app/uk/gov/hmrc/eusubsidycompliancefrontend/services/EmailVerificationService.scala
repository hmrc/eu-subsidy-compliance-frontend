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
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.mvc.Call
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

// TODO - this needs to support both BusinessEntity promote and Undertaking create flows
@Singleton
class EmailVerificationService @Inject() (
   emailVerificationConnector: EmailVerificationConnector,
   eoriEmailDatastore: EoriEmailDatastore,
   servicesConfig: ServicesConfig
) extends Logging {

  lazy private val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")

  def useAbsoluteUrls: Boolean = emailVerificationBaseUrl.contains("localhost")

  def verifyEmail(verifyEmailUrl: String, confirmEmailUrl: String)(credId: String, email: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EmailVerificationResponse]] = {
    emailVerificationConnector
      .verifyEmail(
        EmailVerificationRequest(
          credId = credId,
          // TODO - this needs to take a parameter - also provide syntax to get the URL?
          // TODO - fix handling of abs vs relative urls
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
          // TODO - this needs to take a parameter - also provide syntax to get the URL?
          backUrl =
            // TODO - fix handling of abs vs rel urls
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

  def emailVerificationRedirect(confirmEmailCall: Call)(verifyEmailResponse: Option[EmailVerificationResponse]) = verifyEmailResponse match {
    case Some(value) => Redirect(emailVerificationConnector.getVerificationJourney(value.redirectUri))
    case None => Redirect(confirmEmailCall)
  }

  def getEmailVerification(eori: EORI) = eoriEmailDatastore.getEmailVerification(eori)

  def verifyEori(eori: EORI) = eoriEmailDatastore.verifyEmail(eori)

  def approveVerificationRequest(key: EORI, verificationId: String) = eoriEmailDatastore.approveVerificationRequest(key, verificationId)

  def addVerificationRequest(key: EORI, email: String)(implicit ec: ExecutionContext): Future[String] = {
    val verificationId = UUID.randomUUID().toString
    eoriEmailDatastore
      .addVerificationRequest(key, email, verificationId)
      .map {
        // TODO - review this error string
        _.fold(throw new IllegalArgumentException("Fallen over"))(_.verificationId)
      }
  }

}
