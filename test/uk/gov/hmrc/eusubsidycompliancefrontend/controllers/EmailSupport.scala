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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendResult, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait EmailSupport { this: ControllerSpec =>

  val mockEmailService = mock[EmailService]

  def mockRetrieveEmail(eori: EORI)(result: Either[ConnectorError, RetrieveEmailResponse]) =
    (mockEmailService
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori, *, *)
      .returning(result.fold(e => Future.failed(e), _.toFuture))

  def mockRetrieveEmailAddressAndSendEmail(
    eori1: EORI,
    eori2: Option[EORI],
    key: String,
    undertaking: Undertaking,
    undertakingRef: UndertakingRef,
    removeEffectiveDate: Option[String]
  )(result: Either[ConnectorError, EmailSendResult]) =
    (mockEmailService
      .retrieveEmailAddressAndSendEmail(
        _: EORI,
        _: Option[EORI],
        _: String,
        _: Undertaking,
        _: UndertakingRef,
        _: Option[String])(_: HeaderCarrier, _: ExecutionContext, _: AuthenticatedEscRequest[_], _: MessagesApi))
      .expects(eori1, eori2, key, undertaking, undertakingRef, removeEffectiveDate, *, *, *, *)
      .returning(result.fold(Future.failed, _.toFuture))
}
