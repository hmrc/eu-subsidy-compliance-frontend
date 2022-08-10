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

import org.mongodb.scala.result.UpdateResult
import org.scalamock.scalatest.MockFactory
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ConnectorError, EmailVerificationState, VerifyEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.CacheItem

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationSupport { this: MockFactory =>

  val mockEmailVerificationService = mock[EmailVerificationService]

  def mockEmailVerification(eori: EORI)(result: Either[ConnectorError, CacheItem]) =
    (mockEmailVerificationService
      .verifyEori(_: EORI))
      .expects(eori)
      .returning(result.fold(Future.failed, _.toFuture))


  def mockVerificationRequestVerification(eori: EORI, verificationId: String)(result: Either[ConnectorError, UpdateResult]) =
    (mockEmailVerificationService
      .verifyVerificationRequest(_: EORI, _: String))
      .expects(eori, verificationId)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockGetEmailVerification(eori: EORI)(result: Either[ConnectorError, Option[EmailVerificationState]]) =
    (mockEmailVerificationService
      .getEmailVerification(_: EORI))
      .expects(eori)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockAddEmailVerification(eori: EORI)(result: Either[ConnectorError, CacheItem]) =
    (mockEmailVerificationService
      .addVerificationRequest(_: EORI, _: String, _: String))
      .expects(eori, *, *)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockVerifyEmail(email: String)(result: Either[ConnectorError, Option[VerifyEmailResponse]]) =
    (mockEmailVerificationService
      .verifyEmail(_: String, _: String, _: String)(_: HeaderCarrier, _: ExecutionContext, _: RequestHeader))
      .expects(*, email, *, *, *,*)
      .returning(result.fold(Future.failed, _.toFuture))

  def mockEmailVerificationRedirect(verifyEmailResponse: Option[VerifyEmailResponse])(result: Result) =
    (mockEmailVerificationService
      .emailVerificationRedirect(_: Option[VerifyEmailResponse]))
      .expects(verifyEmailResponse)
      .returning(result)
}
