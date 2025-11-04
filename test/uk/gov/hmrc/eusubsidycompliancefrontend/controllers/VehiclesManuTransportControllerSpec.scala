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

class VehiclesManuTransportControllerSpec
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

  private val controller = instanceOf[VehiclesManuTransportController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val civilianAircraftMachinery = "30.31"
    val militaryAircraftMachinery = "30.32"
    val motorVehiclesManufacture = "29.10"
    val motorVehiclesBodies4 = "29.20"
    val motorVehiclesParts = "29.3"
    val aircraft = "30.3"
    val militaryFightingVehicles = "30.4"
    val railway = "30.2"
    val ships = "30.1"
    val otherTransportEquipment3 = "30.9"
    val bicycles = "30.92"
    val motorcyclesEquipment = "30.91"
    val otherTransportEquipment4 = "30.99"
    val motorVehiclesElectricalManufacture = "29.31"
    val motorVehiclesOtherManufacture = "29.32"
    val civilianShips = "30.11"
    val militaryShips = "30.13"
    val pleasureShips = "30.12"
  }

  import SectorCodes._

  "VehiclesManuTransportController" should {
    "loadAircraftSpacecraftLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAircraftSpacecraftLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadAirTransportFreightAirLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-civilians", "Civilian aircraft, spacecraft and related machinery"),
          ("sector-label-military", "Military aircraft, spacecraft and related machinery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAircraftSpacecraftLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (civilianAircraftMachinery, navigator.nextPage(civilianAircraftMachinery, "").url),
          (militaryAircraftMachinery, navigator.nextPage(militaryAircraftMachinery, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAircraftSpacecraftLvl4Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitAircraftSpacecraftLvl4Page().url)
                .withFormUrlEncodedBody("aircraft4" -> value)
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
          controller.submitAircraftSpacecraftLvl4Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitAircraftSpacecraftLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of aircraft, spacecraft and related machinery your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMotorVehiclesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMotorVehiclesLvl3Page()(
            FakeRequest(GET, routes.VehiclesManuTransportController.loadMotorVehiclesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motor", "Motor vehicles"),
          ("sector-label-other-bodies-trailers", "Bodies and coachwork for motor vehicles; trailers and semi-trailers"),
          ("sector-label-parts-accessories", "Motor vehicle parts and accessories")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMotorVehiclesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (motorVehiclesManufacture, navigator.nextPage(motorVehiclesManufacture, "").url),
          (motorVehiclesBodies4, navigator.nextPage(motorVehiclesBodies4, "").url),
          (motorVehiclesParts, navigator.nextPage(motorVehiclesParts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorVehiclesLvl3Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitMotorVehiclesLvl3Page().url)
                .withFormUrlEncodedBody("vehiclesMan3" -> value)
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
          controller.submitMotorVehiclesLvl3Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitMotorVehiclesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of motor vehicles, trailers or parts your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherTransportEquipmentLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherTransportEquipmentLvl3Page()(
            FakeRequest(GET, routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-other-aircraft", "Aircraft, spacecraft and related machinery"),
          ("sector-label-military", "Military fighting vehicles"),
          ("sector-label-railway", "Railway locomotives and rolling stock"),
          ("sector-label-ships", "Ships and boats"),
          ("sector-label-parts-other", "Other transport equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherTransportEquipmentLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (aircraft, navigator.nextPage(aircraft, "").url),
          (militaryFightingVehicles, navigator.nextPage(militaryFightingVehicles, "").url),
          (railway, navigator.nextPage(railway, "").url),
          (ships, navigator.nextPage(ships, "").url),
          (otherTransportEquipment3, navigator.nextPage(otherTransportEquipment3, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherTransportEquipmentLvl3Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitOtherTransportEquipmentLvl3Page().url)
                .withFormUrlEncodedBody("otherTransport3" -> value)
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
          controller.submitOtherTransportEquipmentLvl3Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitOtherTransportEquipmentLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of transport equipment your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherTransportEquipmentLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherTransportEquipmentLvl4Page()(
            FakeRequest(GET, routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-bicycles", "Bicycles and invalid carriages"),
          ("sector-label-motorcycles", "Motorcycles"),
          ("sector-label-other", "Another type of transport equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherTransportEquipmentLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (bicycles, navigator.nextPage(bicycles, "").url),
          (motorcyclesEquipment, navigator.nextPage(motorcyclesEquipment, "").url),
          (otherTransportEquipment4, navigator.nextPage(otherTransportEquipment4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherTransportEquipmentLvl4Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitOtherTransportEquipmentLvl4Page().url)
                .withFormUrlEncodedBody("otherTransport4" -> value)
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
          controller.submitOtherTransportEquipmentLvl4Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitOtherTransportEquipmentLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other transport equipment your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPartsAccessoriesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPartsAccessoriesLvl4Page()(
            FakeRequest(GET, routes.VehiclesManuTransportController.loadPartsAccessoriesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-electrical-equipment", "Electrical and electronic equipment for motor vehicles"),
          ("sector-label-parts-accessories", "Other parts and accessories for motor vehicles")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPartsAccessoriesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (motorVehiclesElectricalManufacture, navigator.nextPage(motorVehiclesElectricalManufacture, "").url),
          (motorVehiclesOtherManufacture, navigator.nextPage(motorVehiclesOtherManufacture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPartsAccessoriesLvl4Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitPartsAccessoriesLvl4Page().url)
                .withFormUrlEncodedBody("parts4" -> value)
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
          controller.submitPartsAccessoriesLvl4Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitPartsAccessoriesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of motor vehicle parts and accessories your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadShipsBoatsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadShipsBoatsLvl4Page()(
            FakeRequest(GET, routes.VehiclesManuTransportController.loadShipsBoatsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-civilians", "Civilian ships and floating structures"),
          ("sector-label-military", "Military ships and vessels"),
          ("sector-label-pleasure", "Pleasure and sporting boats")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitShipsBoatsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (civilianShips, navigator.nextPage(civilianShips, "").url),
          (militaryShips, navigator.nextPage(militaryShips, "").url),
          (pleasureShips, navigator.nextPage(pleasureShips, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitShipsBoatsLvl4Page()(
              FakeRequest(POST, routes.VehiclesManuTransportController.submitShipsBoatsLvl4Page().url)
                .withFormUrlEncodedBody("ships4" -> value)
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
          controller.submitShipsBoatsLvl4Page()(
            FakeRequest(POST, routes.VehiclesManuTransportController.submitShipsBoatsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of ships and boats your undertaking builds"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
