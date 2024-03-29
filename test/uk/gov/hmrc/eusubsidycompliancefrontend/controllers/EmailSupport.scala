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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailSendResult, EmailTemplate, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.VerifiedStatus.VerifiedStatus
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

  def mockRetrieveVerifiedEmailAddressByEORI(eori: EORI)(result: Future[String]) =
    (mockEmailService
      .retrieveVerifiedEmailAddressByEORI(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori, *, *)
      .returning(result)

  def mockSendEmail(
    eori1: EORI,
    key: EmailTemplate,
    undertaking: Undertaking
  )(result: Either[ConnectorError, EmailSendResult]) =
    (mockEmailService
      .sendEmail(_: EORI, _: EmailTemplate, _: Undertaking)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori1, key, undertaking, *, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockSendEmail(
    eori1: EORI,
    eori2: EORI,
    key: EmailTemplate,
    undertaking: Undertaking
  )(result: Either[ConnectorError, EmailSendResult]) =
    (mockEmailService
      .sendEmail(_: EORI, _: EORI, _: EmailTemplate, _: Undertaking)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori1, eori2, key, undertaking, *, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockSendEmail(
    eori1: EORI,
    key: EmailTemplate,
    undertaking: Undertaking,
    removeEffectiveDate: String
  )(result: Either[ConnectorError, EmailSendResult]) =
    (mockEmailService
      .sendEmail(_: EORI, _: EmailTemplate, _: Undertaking, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori1, key, undertaking, removeEffectiveDate, *, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockSendEmail(
    eori1: EORI,
    eori2: EORI,
    key: EmailTemplate,
    undertaking: Undertaking,
    removeEffectiveDate: String
  )(result: Either[ConnectorError, EmailSendResult]) =
    (mockEmailService
      .sendEmail(_: EORI, _: EORI, _: EmailTemplate, _: Undertaking, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori1, eori2, key, undertaking, removeEffectiveDate, *, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockUpdateEmailForEori(eori: EORI, emailAddress: String)(result: Future[Unit]) =
    (mockEmailService
      .updateEmailForEori(_: EORI, _: String)(_: HeaderCarrier))
      .expects(eori, emailAddress, *)
      .returning(result)

  def mockHasVerifiedEmail(eori: EORI)(result: Future[Option[VerifiedStatus]]) =
    (mockEmailService
      .hasVerifiedEmail(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(eori, *, *)
      .returning(result)
}
