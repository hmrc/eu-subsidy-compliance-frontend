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
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, POST, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class ComputersElectronicsControllerSpec
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

  private val controller = instanceOf[ComputersElectronicsController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val electronicComponents4 = "26.11"
    val loadedElectronicBoards = "26.12"
    val communicationEquipment = "26.3"
    val computersPeripheralEquipment = "26.2"
    val consumerElectronics = "26.4"
    val electronicComponents = "26.1"
    val irradiationElectromedicalAndElectrotherapeuticEquipment = "26.6"
    val measuringToolsAndClocks = "26.5"
    val opticalEquipment = "26.7"
    val electricDomesticAppliances = "27.51"
    val nonElectricDomesticAppliances = "27.52"
    val batteries = "27.2"
    val domesticAppliances = "27.5"
    val electricMotorsGenerators = "27.1"
    val lightingEquipment = "27.4"
    val wiring = "27.3"
    val otherElectricalEquipment = "27.9"
    val bearings = "28.15"
    val engines = "28.11"
    val fluidPowerEquipment = "28.12"
    val otherPumps = "28.13"
    val otherTaps = "28.14"
    val measuringInstruments = "26.51"
    val watchesAndClocks = "26.52"
    val metalFormingMachinery4 = "28.41"
    val otherMachineTools = "28.42"
    val electricMotors = "27.1"
    val electricityDistributionAndControl = "27.12"
    val liftingEquipment = "28.22"
    val nonDomesticAirConditioning = "28.25"
    val officeMachinery = "28.23"
    val ovens = "28.21"
    val powerDrivenHandTools = "28.24"
    val otherGeneralPurposeMachinery4 = "28.29"
    val generalPurposeMachinery = "28.1"
    val otherGeneralPurposeMachinery = "28.2"
    val agriculturalMachinery = "46.61"
    val metalFormingMachinery = "28.4"
    val otherSpecialPurposeMachinery = "28.9"
    val additiveManufacturingMachinery = "28.97"
    val foodMachinery = "28.93"
    val metallurgyMachinery = "28.91"
    val miningMachinery = "28.92"
    val paperMachinery = "28.95"
    val textileMachinery = "28.94"
    val plasticsMachinery = "28.96"
    val otherSpecialPurposeMachinery4 = "28.99"
    val civilianAirRepair = "33.16"
    val civilianShipsRepair = "33.15"
    val otherCivilianTransportEquipmentRepair = "33.17"
    val electricalEquipmentRepair = "33.14"
    val electronicEquipmentRepair = "33.13"
    val fabricatedMetalProducts4 = "33.11"
    val machineryRepair = "33.12"
    val militaryVehiclesRepair = "33.18"
    val otherEquipmentRepair = "33.19"
    val fabricatedMetalProductsRepair = "33.1"
    val industrialMachineryInstallation = "33.2"
    val fibreOpticCables = "27.31"
    val otherElectronicWires = "27.32"
    val wiringDevices = "27.33"
  }

  import SectorCodes._
  "ClothesTextilesHomewareController" should {
    "loadComponentsBoardsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadComponentsBoardsLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadComponentsBoardsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-components", "Electronic components"),
          ("sector-label-boards", "Loaded electronic boards")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitComponentsBoardsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electronicComponents4, navigator.nextPage(electronicComponents4, "").url),
          (loadedElectronicBoards, navigator.nextPage(loadedElectronicBoards, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitComponentsBoardsLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitComponentsBoardsLvl4Page().url)
                .withFormUrlEncodedBody("boards4" -> value)
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
          controller.submitComponentsBoardsLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitComponentsBoardsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of electronic components and boards your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadComputersElectronicsOpticalLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadComputersElectronicsOpticalLvl3Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadComputersElectronicsOpticalLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-communication", "Communication equipment"),
          ("sector-label-peripherals", "Computers and peripheral equipment"),
          ("sector-label-consumer", "Consumer electronics"),
          ("sector-label-components", "Electronic components and boards"),
          ("sector-label-irradiation", "Irradiation, electromedical and electrotherapeutic equipment"),
          ("sector-label-measuring", "Measuring testing instruments, clocks and watches"),
          ("sector-label-optical", "Optical instruments, magnetic and optical media, photographic equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitComputersElectronicsOpticalLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (communicationEquipment, navigator.nextPage(communicationEquipment, "").url),
          (computersPeripheralEquipment, navigator.nextPage(computersPeripheralEquipment, "").url),
          (consumerElectronics, navigator.nextPage(consumerElectronics, "").url),
          (electronicComponents, navigator.nextPage(electronicComponents, "").url),
          (
            irradiationElectromedicalAndElectrotherapeuticEquipment,
            navigator.nextPage(irradiationElectromedicalAndElectrotherapeuticEquipment, "").url
          ),
          (measuringToolsAndClocks, navigator.nextPage(measuringToolsAndClocks, "").url),
          (opticalEquipment, navigator.nextPage(opticalEquipment, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitComputersElectronicsOpticalLvl3Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitComputersElectronicsOpticalLvl3Page().url)
                .withFormUrlEncodedBody("optical3" -> value)
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
          controller.submitComputersElectronicsOpticalLvl3Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitComputersElectronicsOpticalLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of computers, electronics or optical products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadDomesticAppliancesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadDomesticAppliancesLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadDomesticAppliancesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-electric", "Electric domestic appliances"),
          ("sector-label-non-electric", "Non-electric domestic appliances")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitDomesticAppliancesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricDomesticAppliances, navigator.nextPage(electricDomesticAppliances, "").url),
          (nonElectricDomesticAppliances, navigator.nextPage(nonElectricDomesticAppliances, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitDomesticAppliancesLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitDomesticAppliancesLvl4Page().url)
                .withFormUrlEncodedBody("domestic4" -> value)
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
          controller.submitDomesticAppliancesLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitDomesticAppliancesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of domesic appliances your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadElectricalEquipmentLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadElectricalEquipmentLvl3Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadElectricalEquipmentLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-batteries", "Batteries and accumulators"),
          ("sector-label-domestic-appliances", "Domestic appliances"),
          (
            "sector-label-motors-generators",
            "Electric motors, generators, transformers; electricity distribution and control apparatus"
          ),
          ("sector-label-lighting", "Lighting equipment"),
          ("sector-label-wiring", "Wiring and wiring devices"),
          ("sector-label-other", "Other electrical equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitElectricalEquipmentLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (batteries, navigator.nextPage(batteries, "").url),
          (domesticAppliances, navigator.nextPage(domesticAppliances, "").url),
          (electricMotorsGenerators, navigator.nextPage(electricMotorsGenerators, "").url),
          (lightingEquipment, navigator.nextPage(lightingEquipment, "").url),
          (wiring, navigator.nextPage(wiring, "").url),
          (otherElectricalEquipment, navigator.nextPage(otherElectricalEquipment, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitElectricalEquipmentLvl3Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitElectricalEquipmentLvl3Page().url)
                .withFormUrlEncodedBody("electicEquip3" -> value)
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
          controller.submitDomesticAppliancesLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitDomesticAppliancesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of domesic appliances your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadGeneralPurposeLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGeneralPurposeLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadGeneralPurposeLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-bearings", "Bearings, gears, gearing and driving elements"),
          ("sector-label-engines", "Engines and turbines (except aircraft, vehicle and cycle engines)"),
          ("sector-label-fluid-power", "Fluid power equipment"),
          ("sector-label-others", "Other pumps and compressors"),
          ("sector-label-machining", "Other taps and valves")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitGeneralPurposeLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (bearings, navigator.nextPage(bearings, "").url),
          (engines, navigator.nextPage(engines, "").url),
          (fluidPowerEquipment, navigator.nextPage(fluidPowerEquipment, "").url),
          (otherPumps, navigator.nextPage(otherPumps, "").url),
          (otherTaps, navigator.nextPage(otherTaps, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGeneralPurposeLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitGeneralPurposeLvl4Page().url)
                .withFormUrlEncodedBody("generalMachines4" -> value)
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
          controller.submitGeneralPurposeLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitGeneralPurposeLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of general-purpose machinery your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadMeasuringTestingInstrumentsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMeasuringTestingInstrumentsLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadMeasuringTestingInstrumentsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-measuring", "Instruments and appliances for measuring, testing and navigation"),
          ("sector-label-watches-clocks", "Watches and clocks")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitMeasuringTestingInstrumentsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (measuringInstruments, navigator.nextPage(measuringInstruments, "").url),
          (watchesAndClocks, navigator.nextPage(watchesAndClocks, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMeasuringTestingInstrumentsLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitMeasuringTestingInstrumentsLvl4Page().url)
                .withFormUrlEncodedBody("testingInstruments4" -> value)
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
          controller.submitMeasuringTestingInstrumentsLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitMeasuringTestingInstrumentsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of instruments your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadMetalFormingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMetalFormingLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadMetalFormingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-metal-forming", "Metal-forming machinery and machine tools for metal work"),
          ("sector-label-other", "Other machine tools")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitMetalFormingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (metalFormingMachinery4, navigator.nextPage(metalFormingMachinery4, "").url),
          (otherMachineTools, navigator.nextPage(otherMachineTools, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMetalFormingLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitMetalFormingLvl4Page().url)
                .withFormUrlEncodedBody("metalForming4" -> value)
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
          controller.submitMetalFormingLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitMetalFormingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of metal-forming machinery and machine tools your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadMotorsGeneratorsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMotorsGeneratorsLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadMotorsGeneratorsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motors", "Electric motors, generators and transformers"),
          ("sector-label-electricity", "Electricity distribution and control apparatus")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitMotorsGeneratorsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricMotors, navigator.nextPage(electricMotors, "").url),
          (electricityDistributionAndControl, navigator.nextPage(electricityDistributionAndControl, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorsGeneratorsLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitMotorsGeneratorsLvl4Page().url)
                .withFormUrlEncodedBody("generators4" -> value)
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
          controller.submitMotorsGeneratorsLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitMotorsGeneratorsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of equipment for power generation, transmission or distribution your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherGeneralPurposeLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherGeneralPurposeLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadOtherGeneralPurposeLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-lifting", "Lifting and handling equipment"),
          ("sector-label-aircon", "Non-domestic air conditioning equipment"),
          (
            "sector-label-office-machinery",
            "Office machinery and equipment (except computers and peripheral equipment)"
          ),
          ("sector-label-ovens", "Ovens, furnaces and permanent household heating equipment"),
          ("sector-label-hand-tools", "Power-driven hand tools"),
          ("sector-label-other", "Another type of general-purpose machinery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitOtherGeneralPurposeLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (liftingEquipment, navigator.nextPage(liftingEquipment, "").url),
          (nonDomesticAirConditioning, navigator.nextPage(nonDomesticAirConditioning, "").url),
          (officeMachinery, navigator.nextPage(officeMachinery, "").url),
          (ovens, navigator.nextPage(ovens, "").url),
          (powerDrivenHandTools, navigator.nextPage(powerDrivenHandTools, "").url),
          (otherGeneralPurposeMachinery4, navigator.nextPage(otherGeneralPurposeMachinery4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherGeneralPurposeLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitOtherGeneralPurposeLvl4Page().url)
                .withFormUrlEncodedBody("otherGeneralPurpose4" -> value)
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
          controller.submitOtherGeneralPurposeLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitOtherGeneralPurposeLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other general-purpose machinery your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherMachineryLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherMachineryLvl3Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadOtherMachineryLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-general", "General-purpose machinery"),
          ("sector-label-other-general", "Other general-purpose machinery"),
          ("sector-label-agricultural", "Agricultural and forestry machinery"),
          ("sector-label-metal-forming", "Metal-forming machinery and machine tools"),
          ("sector-label-other-special", "Other special-purpose machinery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitOtherMachineryLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (generalPurposeMachinery, navigator.nextPage(generalPurposeMachinery, "").url),
          (otherGeneralPurposeMachinery, navigator.nextPage(otherGeneralPurposeMachinery, "").url),
          (agriculturalMachinery, navigator.nextPage(agriculturalMachinery, "").url),
          (metalFormingMachinery, navigator.nextPage(metalFormingMachinery, "").url),
          (otherSpecialPurposeMachinery, navigator.nextPage(otherSpecialPurposeMachinery, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherMachineryLvl3Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitOtherMachineryLvl3Page().url)
                .withFormUrlEncodedBody("otherMachines3" -> value)
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
          controller.submitOtherMachineryLvl3Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitOtherMachineryLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other machinery and equipment your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherSpecialPurposeLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherSpecialPurposeLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadOtherSpecialPurposeLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-additive", "Additive manufacturing machinery"),
          ("sector-label-food", "Machinery for food, beverage and tobacco processing"),
          ("sector-label-metallurgy", "Machinery for metallurgy"),
          ("sector-label-mining", "Machinery for mining, quarrying and construction"),
          ("sector-label-paper", "Machinery for paper and paperboard production"),
          ("sector-label-textile", "Machinery for textile, apparel and leather production"),
          ("sector-label-plastics", "Plastics and rubber machinery"),
          ("sector-label-other", "Other special-purpose machinery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitOtherSpecialPurposeLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (additiveManufacturingMachinery, navigator.nextPage(additiveManufacturingMachinery, "").url),
          (foodMachinery, navigator.nextPage(foodMachinery, "").url),
          (metallurgyMachinery, navigator.nextPage(metallurgyMachinery, "").url),
          (miningMachinery, navigator.nextPage(miningMachinery, "").url),
          (paperMachinery, navigator.nextPage(paperMachinery, "").url),
          (textileMachinery, navigator.nextPage(textileMachinery, "").url),
          (plasticsMachinery, navigator.nextPage(plasticsMachinery, "").url),
          (otherSpecialPurposeMachinery4, navigator.nextPage(otherSpecialPurposeMachinery4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherSpecialPurposeLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitOtherSpecialPurposeLvl4Page().url)
                .withFormUrlEncodedBody("specialMachines4" -> value)
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
          controller.submitOtherSpecialPurposeLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitOtherSpecialPurposeLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other special-purpose machinery your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRepairMaintenanceLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRepairMaintenanceLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadRepairMaintenanceLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-civ-aircraft", "Civilian air and spacecraft"),
          ("sector-label-civ-ships", "Civilian ships and boats"),
          ("sector-label-civ-other-transport", "Other civilian transport equipment"),
          ("sector-label-electrical", "Electrical equipment"),
          ("sector-label-electronic", "Electronic and optical equipment"),
          ("sector-label-fab-metals", "Fabricated metal products"),
          ("sector-label-repair-machinery", "Machinery"),
          ("sector-label-military-vehicles", "Military fighting vehicles, ships, boats, air and spacecraft"),
          ("sector-label-repair-other", "Another type of equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitRepairMaintenanceLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (civilianAirRepair, navigator.nextPage(civilianAirRepair, "").url),
          (civilianShipsRepair, navigator.nextPage(civilianShipsRepair, "").url),
          (otherCivilianTransportEquipmentRepair, navigator.nextPage(otherCivilianTransportEquipmentRepair, "").url),
          (electricalEquipmentRepair, navigator.nextPage(electricalEquipmentRepair, "").url),
          (electronicEquipmentRepair, navigator.nextPage(electronicEquipmentRepair, "").url),
          (fabricatedMetalProducts4, navigator.nextPage(fabricatedMetalProducts4, "").url),
          (machineryRepair, navigator.nextPage(machineryRepair, "").url),
          (militaryVehiclesRepair, navigator.nextPage(militaryVehiclesRepair, "").url),
          (otherEquipmentRepair, navigator.nextPage(otherEquipmentRepair, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRepairMaintenanceLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitRepairMaintenanceLvl4Page().url)
                .withFormUrlEncodedBody("repair4" -> value)
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
          controller.submitRepairMaintenanceLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitRepairMaintenanceLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of machinery or equipment your undertaking repairs or maintains"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRepairsMaintainInstallLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRepairsMaintainInstallLvl3Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadRepairsMaintainInstallLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-fabricated-metal",
            "Repair and maintenance of fabricated metal products, machinery and equipment"
          ),
          ("sector-label-industrial-install", "Installation of industrial machinery and equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitRepairsMaintainInstallLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fabricatedMetalProductsRepair, navigator.nextPage(fabricatedMetalProductsRepair, "").url),
          (industrialMachineryInstallation, navigator.nextPage(industrialMachineryInstallation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRepairsMaintainInstallLvl3Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitRepairsMaintainInstallLvl3Page().url)
                .withFormUrlEncodedBody("repair3" -> value)
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
          controller.submitRepairsMaintainInstallLvl3Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitRepairsMaintainInstallLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in repair, maintenance or installation of machinery and equipment"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWiringAndDevicesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWiringAndDevicesLvl4Page()(
            FakeRequest(GET, routes.ComputersElectronicsController.loadWiringAndDevicesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-fibre-optic", "Fibreoptic cables"),
          ("sector-label-other-wiring", "Other electronic and electric wires and cables"),
          ("sector-label-devices", "Wiring devices")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWiringAndDevicesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fibreOpticCables, navigator.nextPage(fibreOpticCables, "").url),
          (otherElectronicWires, navigator.nextPage(otherElectronicWires, "").url),
          (wiringDevices, navigator.nextPage(wiringDevices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWiringAndDevicesLvl4Page()(
              FakeRequest(POST, routes.ComputersElectronicsController.submitWiringAndDevicesLvl4Page().url)
                .withFormUrlEncodedBody("wiring4" -> value)
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
          controller.submitWiringAndDevicesLvl4Page()(
            FakeRequest(POST, routes.ComputersElectronicsController.submitWiringAndDevicesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of wiring or wiring device your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
