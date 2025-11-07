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
    val uraniumThoriumOres = "07.21"
    val otherNonFerrousOres = "07.29"
    val stoneQuarrying = "08.1"
    val miningAndQuarryingNEC = "08.9"
    val peatExtraction = "08.92"
    val saltExtraction = "08.93"
    val chemicalMineralsMining = "08.91"
    val otherNEC = "08.99"
    val gravelPitsOperation = "08.12"
    val ornamentalQuarrying = "08.11"
    val hardcoalMining = "05.10"
    val ligniteMining = "05.20"
    val crudePetroleumExtraction = "06.10"
    val naturalGasExtraction = "06.20"
    val ironOresMining = "07.10"
    val nonFerrousOres = "07.2"
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
    "loadNonFeMetalMiningLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadNonFeMetalMiningLvl4Page()(
            FakeRequest(GET, routes.MiningController.loadNonFeMetalMiningLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-uraniumThoriumOres", "Uranium and thorium ores"),
          ("sector-label-otherNonFerrousOres", "Other non-ferrous metal ores")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitNonFeMetalMiningLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (uraniumThoriumOres, navigator.nextPage(uraniumThoriumOres, "").url),
          (otherNonFerrousOres, navigator.nextPage(otherNonFerrousOres, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitNonFeMetalMiningLvl4Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitNonFeMetalMiningLvl4Page().url
              )
                .withFormUrlEncodedBody("nonIron4" -> value)
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
          controller.submitNonFeMetalMiningLvl4Page()(
            FakeRequest(POST, routes.MiningController.submitNonFeMetalMiningLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of non-ferrous metal ores your undertaking mines"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherMiningLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherMiningLvl3Page()(
            FakeRequest(GET, routes.MiningController.loadOtherMiningLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-stoneQuarrying", "Quarrying of stone, sand and clay"),
          ("sector-label-miningAndQuarryingNEC", "Other mining and quarrying")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherMiningLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (stoneQuarrying, navigator.nextPage(stoneQuarrying, "").url),
          (miningAndQuarryingNEC, navigator.nextPage(miningAndQuarryingNEC, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherMiningLvl3Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitOtherMiningLvl3Page().url
              )
                .withFormUrlEncodedBody("otherMining3" -> value)
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
          controller.submitOtherMiningLvl3Page()(
            FakeRequest(POST, routes.MiningController.submitOtherMiningLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of mining or quarrying your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherMiningLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherMiningLvl4Page()(
            FakeRequest(GET, routes.MiningController.loadOtherMiningLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-peatExtraction", "Extraction of peat"),
          ("sector-label-saltExtraction", "Extraction of salt"),
          ("sector-label-chemicalMineralsMining", "Mining of chemical and fertiliser minerals"),
          ("sector-label-otherNEC", "Other mining and quarrying")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherMiningLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (peatExtraction, navigator.nextPage(peatExtraction, "").url),
          (saltExtraction, navigator.nextPage(saltExtraction, "").url),
          (chemicalMineralsMining, navigator.nextPage(chemicalMineralsMining, "").url),
          (otherNEC, navigator.nextPage(otherNEC, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherMiningLvl4Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitOtherMiningLvl4Page().url
              )
                .withFormUrlEncodedBody("otherMining4" -> value)
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
          controller.submitOtherMiningLvl4Page()(
            FakeRequest(POST, routes.MiningController.submitOtherMiningLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of mining or quarrying your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadQuarryingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadQuarryingLvl4Page()(
            FakeRequest(GET, routes.MiningController.loadQuarryingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-gravelPitsOperation", "Operation of gravel and sand pits and mining of clay and kaolin"),
          (
            "sector-label-ornamentalQuarrying",
            "Quarrying of ornamental stone, limestone, gypsum, slate and other stone"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitQuarryingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (gravelPitsOperation, navigator.nextPage(gravelPitsOperation, "").url),
          (ornamentalQuarrying, navigator.nextPage(ornamentalQuarrying, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitQuarryingLvl4Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitQuarryingLvl4Page().url
              )
                .withFormUrlEncodedBody("quarrying4" -> value)
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
          controller.submitQuarryingLvl4Page()(
            FakeRequest(POST, routes.MiningController.submitQuarryingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of sand, stone and clay quarrying your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCoalMiningLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCoalMiningLvl3Page()(
            FakeRequest(GET, routes.MiningController.loadCoalMiningLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-hardcoalMining", "Hard coal"),
          ("sector-label-ligniteMining", "Lignite")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCoalMiningLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (hardcoalMining, navigator.nextPage(hardcoalMining, "").url),
          (ligniteMining, navigator.nextPage(ligniteMining, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCoalMiningLvl3Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitCoalMiningLvl3Page().url
              )
                .withFormUrlEncodedBody("coal3" -> value)
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
          controller.submitCoalMiningLvl3Page()(
            FakeRequest(POST, routes.MiningController.submitCoalMiningLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of coal your undertaking mines"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadGasMiningLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGasMiningLvl3Page()(
            FakeRequest(GET, routes.MiningController.loadGasMiningLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-crudePetroleumExtraction", "Crude petroleum"),
          ("sector-label-naturalGasExtraction", "Natural gas")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitGasMiningLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (crudePetroleumExtraction, navigator.nextPage(crudePetroleumExtraction, "").url),
          (naturalGasExtraction, navigator.nextPage(naturalGasExtraction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGasMiningLvl3Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitGasMiningLvl3Page().url
              )
                .withFormUrlEncodedBody("gas3" -> value)
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
          controller.submitGasMiningLvl3Page()(
            FakeRequest(POST, routes.MiningController.submitGasMiningLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fuel your undertaking extracts"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMetalMiningLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMetalMiningLvl3Page()(
            FakeRequest(GET, routes.MiningController.loadMetalMiningLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-ironOre", "Iron ores"),
          ("sector-label-nonFerrousOres", "Non-ferrous metal ores")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMetalMiningLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (ironOresMining, navigator.nextPage(ironOresMining, "").url),
          (nonFerrousOres, navigator.nextPage(nonFerrousOres, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMetalMiningLvl3Page()(
              FakeRequest(
                POST,
                routes.MiningController.submitMetalMiningLvl3Page().url
              )
                .withFormUrlEncodedBody("metal3" -> value)
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
          controller.submitMetalMiningLvl3Page()(
            FakeRequest(POST, routes.MiningController.submitMetalMiningLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metal ores your undertaking mines"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
