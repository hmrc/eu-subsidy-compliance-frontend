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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.i18n.MessagesApi
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEscRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailParameters, EmailSendResult, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailAddress}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{RetrieveEmailService, SendEmailService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait EmailSupport { this: ControllerSpec =>

  val mockRetrieveEmailService: RetrieveEmailService = mock[RetrieveEmailService]
  val mockSendEmailService = mock[SendEmailService]

  def mockRetrieveEmail(eori: EORI)(result: Either[ConnectorError, RetrieveEmailResponse]) =
    (mockRetrieveEmailService
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockSendEmail(emailAddress: EmailAddress, emailParameters: EmailParameters, templateId: String)(
    result: Either[ConnectorError, EmailSendResult]
  ) =
    (mockSendEmailService
      .sendEmail(_: EmailAddress, _: EmailParameters, _: String)(_: HeaderCarrier))
      .expects(emailAddress, emailParameters, templateId, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))
}
