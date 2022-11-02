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
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders.EscActionBuilder.{EccEnrolmentIdentifier, EccEnrolmentKey}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

import java.net.URLEncoder
import scala.concurrent.Future

trait AuthAndSessionDataBehaviour { this: ControllerSpec with AuthSupport with JourneyStoreSupport =>

  private val appName = "eu-subsidy-test"
  private val ggSignInUrl = "http://ggSignInUrl:123"
  private val ggSignOutUrl = "http://ggSignOutUrl:123"
  private val providerId = "1123"
  private val groupId = Some("groupIdentifier")

  private lazy val expectedSignInUrl: String =
    s"$ggSignInUrl?" + s"continue=${URLEncoder.encode("http://localhost/", "UTF-8")}&origin=$appName"

  private def identifiers(eori: EORI) = Seq(EnrolmentIdentifier(EccEnrolmentIdentifier, eori))
  private def eccEnrolments(eori: EORI) = Enrolment(key = EccEnrolmentKey, identifiers = identifiers(eori), state = "")
  private def enrolmentSets(eori: EORI) = Set(eccEnrolments(eori))

  override def additionalConfig: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         | appName = "$appName"
         | urls.ggSignInUrl = "$ggSignInUrl"
         | urls.ggSignOutUrl = "$ggSignOutUrl"
         |""".stripMargin
    )
  )

  def mockAuthWithoutEnrolment(): Unit =
    mockAuthWithEccRetrievals(Enrolments(Set.empty), providerId, groupId)

  def mockAuthWithEnrolment(eori: EORI = eori1): Unit =
    mockAuthWithEccRetrievals(Enrolments(Set(eccEnrolments(eori))), providerId, groupId)

  def mockAuthWithEnrolmentWithoutEori(): Unit = mockAuthWithEccRetrievals(
    Enrolments(Set(Enrolment(EccEnrolmentKey, Seq.empty, state = ""))), providerId, groupId
  )

  def mockAuthWithEnrolmentAndValidEmail(eori: EORI = eori1): Unit =
    mockAuthWithEccAuthRetrievalsWithEmailCheck(Enrolments(enrolmentSets(eori)), providerId, groupId)

  def mockAuthWithEnrolmentAndNoEmailVerification(eori: EORI = eori1): Unit =
    mockAuthWithEccAuthRetrievalsNoEmailVerification(Enrolments(enrolmentSets(eori)), providerId, groupId)

  def authBehaviour(performAction: () => Future[Result]): Unit =
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
