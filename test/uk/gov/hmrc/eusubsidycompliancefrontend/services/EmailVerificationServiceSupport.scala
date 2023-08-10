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

import org.scalamock.scalatest.MockFactory
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.AuthSupport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, VerifiedEmail}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationServiceSupport { this: MockFactory with AuthSupport =>

  def mockApproveVerification(eori: EORI, verificationId: String)(result: Either[ConnectorError, Boolean]) =
    (mockEmailVerificationService
      .approveVerificationRequest(_: EORI, _: String)(_: ExecutionContext))
      .expects(eori, verificationId, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockGetEmailVerification(eori: EORI)(result: Either[ConnectorError, Option[VerifiedEmail]]) =
    (mockEmailVerificationService
      .getEmailVerification(_: EORI))
      .expects(eori)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockAddVerifiedEmail(eori: EORI, emailAddress: String)(result: Future[Unit] = ().toFuture) =
    (mockEmailVerificationService
      .addVerifiedEmail(_: EORI, _: String)(_: ExecutionContext))
      .expects(eori, emailAddress, *)
      .returning(result)

  def mockMakeVerificationRequestAndRedirect(result: Future[Result]) =
    (
      mockEmailVerificationService
        .makeVerificationRequestAndRedirect(
          _: String,
          _: Call,
          _: String => String,
          _: String
        )(
          _: AuthenticatedEnrolledRequest[AnyContent],
          _: ExecutionContext,
          _: HeaderCarrier,
          _: Messages
        )
      )
      .expects(*, *, *, *, *, *, *, *)
      .returning(result)
}
