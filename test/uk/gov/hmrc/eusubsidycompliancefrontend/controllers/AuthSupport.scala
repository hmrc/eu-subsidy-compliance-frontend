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
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EmailVerificationService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthSupport { this: ControllerSpec =>

  protected val mockAuthConnector: AuthConnector = mock[AuthConnector]

  protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]

  protected val authRetrievals: Retrieval[Option[Credentials] ~ Option[String] ~ Enrolments ~ Option[AffinityGroup]] =
    Retrievals.credentials and Retrievals.groupIdentifier and Retrievals.allEnrolments and Retrievals.affinityGroup

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

  def mockAuthWithEccRetrievals(
    enrolments: Enrolments,
    providerId: String,
    groupIdentifier: Option[String],
    affinityGroup: Option[AffinityGroup] = Some(Individual)
  ): Unit =
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments and affinityGroup).toFuture
    )

  def mockAuthWithEccAuthRetrievalsNoEmailVerification(
    enrolments: Enrolments,
    providerId: String,
    groupIdentifier: Option[String],
    affinityGroup: Option[AffinityGroup] = Some(Individual)
  ): Unit =
    mockAuth(EmptyPredicate, authRetrievals)(
      (new ~(Credentials(providerId, "type").some, groupIdentifier) and enrolments and affinityGroup).toFuture
    )

}
