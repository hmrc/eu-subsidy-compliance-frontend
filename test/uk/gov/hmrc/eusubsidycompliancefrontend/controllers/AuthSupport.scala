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

import cats.implicits.catsSyntaxOptionId
import org.scalamock.handlers.CallHandler1
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.eusubsidycompliancefrontend.models.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{EmailService, EmailVerificationService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthSupport { this: ControllerSpec with EmailSupport =>

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  protected val authRetrievals: Retrieval[Option[Credentials] ~ Option[String] ~ Enrolments] =
    Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments

  def mockAuth[R](predicate: Predicate, retrieval: Retrieval[R])(
    result: Future[R]
  ): Unit =
    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[R])(
        _: HeaderCarrier,
        _: ExecutionContext
      ))
      .expects(predicate, retrieval, *, *)
      .returning(result)

  def mockAuthWithEccRetrievals(enrolments: Enrolments, providerId: String, groupIdentifier: Option[String]): Unit =
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )

  def mockAuthWithEccAuthRetrievalsWithEmailCheck(
    enrolments: Enrolments,
    providerId: String,
    groupIdentifier: Option[String]
  ): Unit = {
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )
    mockHasCacheEmailVerification()
  }

  def mockAuthWithEccAuthRetrievalsNoEmailVerification(
    enrolments: Enrolments,
    providerId: String,
    groupIdentifier: Option[String]
  ): Unit =
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )

  def mockEmailVerificationPrioritisingCds(email: String = "foo@example.com"): Unit =
    mockEmailVerificationPrioritisingCds(VerifiedEmail(email, "", verified = true).some)

  def mockEmailVerificationPrioritisingCds(
    maybeVerifiedEmail: Option[VerifiedEmail]
  ): Unit =
    (mockEmailService
      .findVerifiedEmailPrioritisingCDS(_: EORI)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(maybeVerifiedEmail.map(_.email).toFuture)

  def mockHasCacheEmailVerification(hasVerified: Boolean = true): Unit =
    (mockEmailVerificationService.hasVerifiedEmail _)
      //should not really have all this wildcarded :(
      .expects(*)
      .returning(hasVerified.toFuture)

}
