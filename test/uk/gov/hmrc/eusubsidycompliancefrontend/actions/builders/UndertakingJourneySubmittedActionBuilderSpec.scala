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
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContent
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers._
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.ConnectorError
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.test.BaseSpec
import uk.gov.hmrc.eusubsidycompliancefrontend.test.CommonTestData.eori1

import scala.concurrent.Future

class UndertakingJourneySubmittedActionBuilderSpec
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
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[Store].toInstance(mockJourneyStore)
  )

  private val underTest = instanceOf[UndertakingJourneySubmittedActionBuilder]

  private val block = (_: AuthenticatedEnrolledRequest[AnyContent]) => Ok.toFuture

  "UndertakingJourneySubmittedActionBuilder" when {

    "journey is not submitted" should {
      "allow the request to proceed" in {
        val journey = UndertakingJourney()

        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[UndertakingJourney](eori1)(Right(Some(journey)))
        }

        val request = FakeRequest()
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe OK
      }
    }

    "journey is already submitted" should {
      "redirect to registration already submitted page" in {
        val submittedJourney = UndertakingJourney().setSubmitted(true)

        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[UndertakingJourney](eori1)(Right(Some(submittedJourney)))
        }

        val request = FakeRequest()
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) should contain(
          routes.RegistrationSubmittedController.registrationAlreadySubmitted.url
        )
      }
    }

    "no journey exists in store" should {
      "return internal server error" in {
        inSequence {
          mockAuthWithEnrolment(eori1)
          mockGet[UndertakingJourney](eori1)(Right(None))
        }

        val request = FakeRequest()
        val result = underTest.invokeBlock(request, block)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
