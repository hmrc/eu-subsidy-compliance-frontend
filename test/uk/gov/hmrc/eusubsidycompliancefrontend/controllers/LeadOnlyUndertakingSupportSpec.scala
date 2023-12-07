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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.NewInstanceInjector.instanceOf
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.EscService
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.{eori1, eori3, undertaking}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig

import scala.concurrent.{ExecutionContext, Future}

class LeadOnlyUndertakingSupportSpec
    extends AnyWordSpecLike
    with MockFactory
    with ScalaFutures
    with Matchers
    with JourneyStoreSupport {

  private val mockEscService = mock[EscService]

  private val underTest = new FrontendController(mock[MessagesControllerComponents]) with LeadOnlyUndertakingSupport {
    override protected val escService: EscService = mockEscService
    override protected implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
  }

  "LeadOnlyUnderTakingSupport" should {

    "invoke the function" when {

      def runTest() = {
        val fakeRequest = authorisedRequestForEori(eori1)
        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest, instanceOf[AppConfig])
        status(result) shouldBe OK
      }

      "called with a request from a lead undertaking user" in {
        inSequence {
          mockRetrieveUndertaking(eori1)(undertaking.some.toFuture)
        }

        runTest()
      }

    }

    "redirect to the account home page" when {

      "called with a request from a non-lead undertaking user" in {
        inSequence {
          mockRetrieveUndertaking(eori3)(undertaking.some.toFuture)
        }

        val fakeRequest = authorisedRequestForEori(eori3)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest, instanceOf[AppConfig])

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
      }

      "no undertaking could be found for the eori associated with the request" in {
        inSequence {
          mockRetrieveUndertaking(eori1)(Option.empty.toFuture)
        }

        val fakeRequest = authorisedRequestForEori(eori1)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest, instanceOf[AppConfig])

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(routes.AccountController.getAccountPage.url)
      }
    }

    "throw an error" when {

      def runTest() = {
        val fakeRequest = authorisedRequestForEori(eori1)

        val result = underTest.withLeadUndertaking(_ => Ok("Foo").toFuture)(fakeRequest, instanceOf[AppConfig])

        a[RuntimeException] shouldBe thrownBy(result.futureValue)
      }

      "an error occurred retrieving the undertaking from the backend" in {
        inSequence {
          mockRetrieveUndertaking(eori1)(Future.failed(new RuntimeException("Some error")))
        }

        runTest()
      }
    }

  }

  private def mockRetrieveUndertaking(eori: EORI)(result: Future[Option[Undertaking]]) =
    (mockEscService
      .retrieveUndertaking(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result)

  private def authorisedRequestForEori(e: EORI) = AuthenticatedEnrolledRequest(
    "Foo",
    "Bar",
    FakeRequest(),
    e
  )

}
