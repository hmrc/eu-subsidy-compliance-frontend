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

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

import java.net.URLEncoder
import scala.concurrent.Future

trait AuthAndSessionDataBehaviour { this: ControllerSpec with AuthSupport with JourneyStoreSupport =>

  val appName = "eu-subsidy-test"

  val eccEnrolmentKey = "HMRC-ESC-ORG"
  val eccPredicate = Enrolment(eccEnrolmentKey)

  val ggSignInUrl = "http://ggSignInUrl:123"
  val ggSignOutUrl = "http://ggSignOutUrl:123"

  def identifiers(eori: EORI) = Seq(EnrolmentIdentifier("EORINumber", eori))

  def eccEnrolments(eori: EORI) = Enrolment(key = eccEnrolmentKey, identifiers = identifiers(eori), state = "")

  def enrolmentSets(eori: EORI) = Set(eccEnrolments(eori))

  override def additionalConfig = Configuration(
    ConfigFactory.parseString(
      s"""
         | appName = "$appName"
         | urls.ggSignInUrl = "$ggSignInUrl"
         | urls.ggSignOutUrl = "$ggSignOutUrl"
         |""".stripMargin
    )
  )

  lazy val expectedSignInUrl = {
    s"$ggSignInUrl?" + s"continue=${URLEncoder.encode("/", "UTF-8")}&" +
      s"origin=$appName"
  }

  def mockAuthWithNoEnrolment() =
    mockAuthNoEnrolmentsRetrievals("1123", Some("groupIdentifier"))

  def mockAuthWithNoEnrolmentNoCheck() =
    mockAuthWithAuthRetrievalsNoPredicate(Enrolments(Set.empty), "1123", Some("groupIdentifier"))

  def mockAuthWithEccEnrolmentOnly(eori: EORI) = {
    mockAuthWithAuthRetrievalsNoPredicate(Enrolments(Set(eccEnrolments(eori))), "1123", Some("groupIdentifier"))
  }

  def mockAuthWithEccEnrolmentWithoutEori() = mockAuthWithAuthRetrievalsNoPredicate(
    Enrolments(Set(Enrolment(eccEnrolmentKey, Seq.empty, state = ""))), "1123", Some("groupIdentifier")
  )

  def mockNoPredicateAuthWithNecessaryEnrolment(eori: EORI = eori1): Unit =
    mockAuthWithAuthRetrievalsNoPredicate(Enrolments(enrolmentSets(eori)), "1123", Some("groupIdentifier"))

  // TODO - review naming here
  def mockAuthWithNecessaryEnrolmentWithValidEmail(eori: EORI = eori1): Unit =
    mockAuthWithEccAuthRetrievalsWithEmailCheck(Enrolments(enrolmentSets(eori)), "1123", Some("groupIdentifier"))

  // TODO - do we need these necessary enrolment methods?
  def mockAuthWithNecessaryEnrolment(eori: EORI = eori1): Unit =
    mockAuthWithEccAuthRetrievals(Enrolments(enrolmentSets(eori)), "1123", Some("groupIdentifier"))

  def mockAuthWithNecessaryEnrolmentNoEmailVerification(eori: EORI = eori1): Unit =
    mockAuthWithEccAuthRetrievalsNoEmailVerification(Enrolments(enrolmentSets(eori)), "1123", Some("groupIdentifier"))

  def mockAuthWithEORIEnrolment(eori: EORI): Unit =
    mockAuthWithAuthRetrievalsNoPredicate(Enrolments(enrolmentSets(eori)), "1123", Some("groupIdentifier"))

  def authBehaviourWithPredicate(performAction: () => Future[Result]): Unit =
    "redirect to the login page when the user is not logged in" in {
      List[NoActiveSession](
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { e =>
        withClue(s"For AuthorisationException $e: ") {
          mockAuth(EmptyPredicate, authRetrievals)(Future.failed(e))

          checkIsRedirect(performAction(), expectedSignInUrl)
        }
      }

    }

}
