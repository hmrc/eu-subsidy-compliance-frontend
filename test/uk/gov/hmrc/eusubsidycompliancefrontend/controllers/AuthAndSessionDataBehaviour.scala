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
import uk.gov.hmrc.auth.core.{BearerTokenExpired, Enrolment, EnrolmentIdentifier, Enrolments, InvalidBearerToken, MissingBearerToken, NoActiveSession, SessionRecordNotFound}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import utils.CommonTestData.eori4

import java.net.URLEncoder
import scala.concurrent.Future

trait AuthAndSessionDataBehaviour { this: ControllerSpec with AuthSupport with JourneyStoreSupport =>

  val appName = "eu-subsidy-test"

  val eori = EORI("GB123456789012")

  val predicate    = Enrolment("HMRC-ESC-ORG")
  val ggSignInUrl  = "http://ggSignInUrl:123"
  val ggSignOutUrl = "http://ggSignOutUrl:123"

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

  def mockAuthWithNecessaryEnrolment(): Unit =
    mockAuthWithAuthRetrievals(
      Enrolments(
        Set(Enrolment(key = "HMRC-ESC-ORG", identifiers = Seq(EnrolmentIdentifier("EORINumber", eori)), state = ""))
      ),
      "1123",
      Some("groupIdentifier")
    )

  def mockAuthWithBEEnrolment(): Unit =
    mockAuthWithAuthRetrievals(
      Enrolments(
        Set(Enrolment(key = "HMRC-ESC-ORG", identifiers = Seq(EnrolmentIdentifier("EORINumber", eori4)), state = ""))
      ),
      "1123",
      Some("groupIdentifier")
    )

  def mockAuthWithEnrolment(eori: EORI): Unit =
    mockAuthWithAuthRetrievals(
      Enrolments(
        Set(Enrolment(key = "HMRC-ESC-ORG", identifiers = Seq(EnrolmentIdentifier("EORINumber", eori)), state = ""))
      ),
      "1123",
      Some("groupIdentifier")
    )

  def authBehaviour(performAction: () => Future[Result]): Unit =
    "redirect to the login page when the user is not logged in" in {
      List[NoActiveSession](
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { e =>
        withClue(s"For AuthorisationException $e: ") {
          mockAuth(predicate, authRetrievals)(Future.failed(e))

          checkIsRedirect(performAction(), expectedSignInUrl)
        }
      }

    }
}
