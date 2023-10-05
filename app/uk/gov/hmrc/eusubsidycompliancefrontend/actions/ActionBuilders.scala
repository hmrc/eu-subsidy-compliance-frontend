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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions

import play.api.mvc.{ActionBuilder, AnyContent}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders.{EnrolledActionBuilder, NotEnrolledActionBuilder, VerifiedEoriActionBuilder}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.{AuthenticatedEnrolledRequest, AuthenticatedRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class ActionBuilders @Inject() (
  enrolledActionBuilder: EnrolledActionBuilder,
  notEnrolledActionBuilder: NotEnrolledActionBuilder,
  verifiedEmailActionBuilder: VerifiedEoriActionBuilder
) {

  // GG Auth with ECC Enrolment - redirect to ECC if not enrolled
  val enrolled: ActionBuilder[AuthenticatedEnrolledRequest, AnyContent] = enrolledActionBuilder

  // GG Auth without ECC enrolment - redirect to account home if enrolled
  val notEnrolled: ActionBuilder[AuthenticatedRequest, AnyContent] = notEnrolledActionBuilder

  // GG Auth with ECC Enrolment and Verified Email Address
  val verifiedEori: ActionBuilder[AuthenticatedEnrolledRequest, AnyContent] = verifiedEmailActionBuilder

}
