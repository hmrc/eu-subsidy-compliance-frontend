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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions.builders

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContent
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.{AuthConnector, InvalidBearerToken}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

import scala.concurrent.Future

class NotEnrolledActionBuilderSpec
    extends BaseSpec
    with ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with MockFactory
    with ScalaFutures
    with DefaultAwaitTimeout {

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(authSupport.mockAuthConnector)
  )

  private def FakeSignInUrl = "/fake/gg/signin"

  override def additionalConfig: Configuration = Configuration.from(
    Map(
      "urls.ggSignInUrl" -> FakeSignInUrl
    )
  )

  private val underTest = instanceOf[NotEnrolledActionBuilder]

  private val block = (_: AuthenticatedRequest[AnyContent]) => Ok.toFuture

  "NotEnrolledActionBuilder" should {

    "redirect to the government gateway login page" when {
      "handling a request that is not authenticated" in {
        authSupport.mockAuth(EmptyPredicate, authSupport.authRetrievals)(Future.failed(InvalidBearerToken()))

        val request = FakeRequest().withHeaders(HOST -> "www.example.com")
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(s"$FakeSignInUrl?continue=%2F&origin=eu-subsidy-compliance-frontend")
      }
    }

    "redirect to the account page" when {
      "handling a request that is authenticated and enrolled" in {
        authAndSessionDataBehaviour.mockAuthWithEnrolment(eori1)

        val request = FakeRequest()
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
      }
    }

    "invoke the supplied block" when {
      "handling a request that is authenticated but not enrolled" in {
        authAndSessionDataBehaviour.mockAuthWithoutEnrolment()

        val request = FakeRequest()
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe OK
      }
    }

  }

}
