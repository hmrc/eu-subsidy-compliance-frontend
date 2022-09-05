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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions

import play.api.mvc.{ActionBuilder, AnyContent}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders.{AuthenticatedActionBuilder, EnrolledActionBuilder, EnrolledActionBuilderToFirstEligibilityPage, VerifiedEmailActionBuilder}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.{AuthenticatedEnrolledRequest, AuthenticatedRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class ActionBuilders @Inject() (
  authenticatedActionBuilder: AuthenticatedActionBuilder,
  enrolledActionBuilder: EnrolledActionBuilder,
  enrolledActionBuilderToFirstEligibilityPage: EnrolledActionBuilderToFirstEligibilityPage,
  verifiedEmailActionBuilder: VerifiedEmailActionBuilder,
) {

  // GG Auth only (enrolment not checked)
  val authenticated: ActionBuilder[AuthenticatedRequest, AnyContent] = authenticatedActionBuilder

  // GG Auth with ECC Enrolment - redir to ECC if not enrolled
  val enrolled: ActionBuilder[AuthenticatedEnrolledRequest, AnyContent] = enrolledActionBuilder

  // GG Auth without ECC enrolment - run bock if not enrolled otherwise redirect to /
  // TODO - better name for this
  val enrolledToFirstLogin: ActionBuilder[AuthenticatedRequest, AnyContent] = enrolledActionBuilderToFirstEligibilityPage

  // TODO - do we need a not enrolled handler?

  // GG Auth with ECC Enrolment and Verified Email Address
  val verifiedEmail: ActionBuilder[AuthenticatedEnrolledRequest, AnyContent] = verifiedEmailActionBuilder

}