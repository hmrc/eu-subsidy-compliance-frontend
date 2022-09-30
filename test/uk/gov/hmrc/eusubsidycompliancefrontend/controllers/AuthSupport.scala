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

import cats.implicits.catsSyntaxOptionId
import org.scalamock.handlers.CallHandler1
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, EmptyRetrieval, Retrieval, ~}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.VerifiedEmail
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthSupport { this: ControllerSpec =>

  import AuthSupport._

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

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

  def mockAuthWithNoRetrievals(): Unit =
    mockAuth(EmptyPredicate, EmptyRetrieval)(().toFuture)

  def mockAuthWithAuthRetrievalsNoPredicate(
    enrolments: Enrolments,
    providerId: String,
    groupIdentifier: Option[String]
  ) = {
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )
  }


  def mockAuthNoEnrolmentsRetrievals(
    providerId: String,
    groupIdentifier: Option[String]
  ) =
    mockAuth(EmptyPredicate, authRetrievalsNoEnrolment)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier)).toFuture
    )

  def mockAuthWithEccAuthRetrievalsWithEmailCheck(enrolments: Enrolments, providerId: String, groupIdentifier: Option[String]) = {
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )
    mockGetEmailVerification()
  }

  def mockAuthWithEccAuthRetrievals(enrolments: Enrolments, providerId: String, groupIdentifier: Option[String]) = {
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )
  }

  def mockAuthWithEccAuthRetrievalsNoEmailVerification(enrolments: Enrolments, providerId: String, groupIdentifier: Option[String]) =
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments).toFuture
    )

  val authRetrievals: Retrieval[Option[Credentials] ~ Option[String] ~ Enrolments] =
    Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments

  val authRetrievalsNoEnrolment: Retrieval[Option[Credentials] ~ Option[String]] =
    Retrievals.credentials and Retrievals.groupIdentifier

  def mockGetEmailVerification(result: Option[VerifiedEmail]): CallHandler1[EORI, Future[Option[VerifiedEmail]]] =
    (mockEmailVerificationService
      .getEmailVerification(_: EORI))
      .expects(*)
      .returning(result.toFuture)

  def mockGetEmailVerification(email: String = "foo@example.com"): CallHandler1[EORI, Future[Option[VerifiedEmail]]] =
    mockGetEmailVerification(VerifiedEmail(email, "", verified = true).some)

}

object AuthSupport {

  implicit class RetrievalOps[A, B](val r: ~[A, B]) {
    def and[C](c: C): ~[~[A, B], C] = new ~(r, c)
  }

}
