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
import play.api.mvc.RequestHeader
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.eusubsidycompliancefrontend.cache.EoriEmailDatastore
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{VerifyEmailResponse, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (
   emailVerificationConnector: EmailVerificationConnector,
   eoriEmailDatastore: EoriEmailDatastore
) extends Logging {

  def verifyEmail(credId: String, email: String, pendingVerificationId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, h: RequestHeader): Future[Option[VerifyEmailResponse]] = {
    emailVerificationConnector
      .verifyEmail(
        VerifyEmailRequest(
          credId = credId,
          continueUrl = routes.UndertakingController.getVerifyEmail(pendingVerificationId).absoluteURL(),
          origin = "EU Subsidy Complience",
          deskproServiceName = None,
          accessibilityStatementUrl = "",
          email = Some(Email(
            address = email,
            enterUrl = ""
          )),
          lang = None,
          backUrl = Some(routes.UndertakingController.getConfirmEmail().absoluteURL()),
          pageTitle = None
        )
      ).map {
      case Left(error) => throw ConnectorError(s"Error sending email:Id", error)
      case Right(value) =>
        value.status match {
          case CREATED => Some(value.json.as[VerifyEmailResponse])
          case _ => None
        }
    }
  }

  def emailVerificationRedirect(verifyEmailResponse: Option[VerifyEmailResponse]) = verifyEmailResponse match {
    case Some(value) => Redirect(emailVerificationConnector.getVerificationJourney(value.redirectUri))
    case None => Redirect(routes.UndertakingController.getConfirmEmail())
  }

  def getEmailVerification(eori: EORI) = eoriEmailDatastore.getEmailVerification(eori)

  def verifyEori(eori: EORI) = eoriEmailDatastore.verifyEmail(eori)

  def verifyVerificationRequest(key: EORI, pendingVerificationCode: String) = eoriEmailDatastore.verifyVerificationRequest(key, pendingVerificationCode)

  def addVerificationRequest(key: EORI, email: String, pendingVerificationId: String) = eoriEmailDatastore.addVerificationRequest(key, email, pendingVerificationId)

}
