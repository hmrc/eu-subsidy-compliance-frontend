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
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.AuthSupport
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.VerificationStatus

import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationServiceSupport { this: MockFactory with AuthSupport =>

  def mockGetEmailVerificationStatus(result: Future[Option[VerificationStatus]]) =
    (mockEmailVerificationService
      .getEmailVerificationStatus(_: AuthenticatedEnrolledRequest[AnyContent], _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(result)

  def mockMakeVerificationRequestAndRedirect(result: Future[Result]) =
    (
      mockEmailVerificationService
        .makeVerificationRequestAndRedirect(
          _: String,
          _: String,
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
