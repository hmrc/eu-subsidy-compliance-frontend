/*
 * Copyright 2025 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.TableDrivenPropertyChecks._
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig

class MiningControllerSpec
  extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[MiningController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val petroleumNaturalGasExtraction = "06"
    val coalAndLigniteMining = "05"
    val metalOresMining = "07"
    val otherMiningQuarrying = "08"
    val miningSupportServices = "09"
    val miningSupportPetroleumExtraction4 = "09.10"
    val otherMiningSupport4 = "09.90"

  }

  import SectorCodes._

  "MiningController" should {
    "loadMiningLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMiningLvl2Page()(
            FakeRequest(GET, routes.MiningController.loadMiningLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-petroleumNaturalGasExtraction", "Extraction of crude petroleum and natural gas"),
          ("sector-label-coalAndLigniteMining", "Mining of coal and lignite"),
          ("sector-label-metalOresMining", "Mining of metal ores"),
          ("sector-label-otherMiningQuarrying", "Other mining and quarrying"),
          ("sector-label-miningSupportServices", "Mining support services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMiningLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (petroleumNaturalGasExtraction, navigator.nextPage(petroleumNaturalGasExtraction, "").url),
          (coalAndLigniteMining, navigator.nextPage(coalAndLigniteMining, "").url),
          (metalOresMining, navigator.nextPage(metalOresMining, "").url),
          (otherMiningQuarrying, navigator.nextPage(otherMiningQuarrying, "").url),
          (miningSupportServices, navigator.nextPage(miningSupportServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMiningLvl2Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitMiningLvl2Page().url
              )
                .withFormUrlEncodedBody("mining2" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitMiningLvl2Page()(
            FakeRequest(POST, routes.MiningController.submitMiningLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in mining and quarrying"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMiningSupportLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMiningSupportLvl3Page()(
            FakeRequest(GET, routes.MiningController.loadMiningSupportLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-miningSupportPetroleumExtraction", "Extraction of petroleum and natural gas"),
          ("sector-label-otherMiningSupport", "Other mining and quarrying")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMiningSupportLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (miningSupportPetroleumExtraction4, navigator.nextPage(miningSupportPetroleumExtraction4, "").url),
          (otherMiningSupport4, navigator.nextPage(otherMiningSupport4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMiningSupportLvl3Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitMiningSupportLvl3Page().url
              )
                .withFormUrlEncodedBody("miningSupport3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitMiningSupportLvl3Page()(
            FakeRequest(POST, routes.MiningController.submitMiningSupportLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "mining or quarrying your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }





  }
}
