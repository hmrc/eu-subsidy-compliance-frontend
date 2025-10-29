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
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table

class TransportControllerSpec
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

  private val controller = instanceOf[TransportController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val freightAirTransport = "51.21"
    val spaceTransport = "51.22"
    val freightAndSpaceAirTransport = "51.2"
    val passengerAirTransport4 = "51.10"
    val freightRoadTransport4 = "49.41"
    val removalServices = "49.42"
    val passengerRailTransport = "49.1"
    val otherPassengerLandTransport = "49.3"
    val freightRailTransport4 = "49.20"
    val freightRoadTransport = "49.4"
    val pipelineTransport4 = "49.50"
    val nonScheduledPassengerRoadTransport = "49.32"
    val onDemandPassengerRoadTransport = "49.32"
    val cablewaysPassengerTransport = "49.34"
    val scheduledPassengerRoadTransport = "49.31"
    val otherLandTransport = "49.39"
    val passengerHeavyRailTransport = "49.11"
    val otherPassengerRailTransport = "49.12"
    val postalActivities4 = "53.10"
    val otherPostalActivities4 = "53.20"
    val postalIntermediationActivities4 = "53.30"
    val airTransport = "77.35"
    val landTransport = "49"
    val postalAndCourierActivities = "53"
    val warehousingStorageTransportSupportActivities = "52"
    val waterTransport = "50"
    val cargoHandling = "52.24"
    val logisticsServiceActivities = "52.25"
    val airTransportServiceActivities = "52.23"
    val landTransportServiceActivities = "52.21"
    val waterTransportServiceActivities = "52.22"
    val otherTransportSupportActivities = "52.26"
    val freightTransportIntermediationServiceActivities = "52.31"
    val passengerTransportIntermediationServiceActivities = "52.32"
    val inlandFreightWaterTransport4 = "50.40"
    val inlandPassengerWaterTransport4 = "50.30"
    val coastalFreightWaterTransport4 = "50.20"
    val coastalPassengerWaterTransport4 = "50.10"
    val transportIntermediationServiceActivities = "52.3"
    val transportSupportActivities = "52.2"
    val warehousingStorage4 = "52.10"

  }

  import SectorCodes._

  "TransportControllerSpec" should {
    "loadAirTransportFreightAirLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAirTransportFreightAirLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadAirTransportFreightAirLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-freight-air", "Freight air transport"),
          ("sector-label-space-transport", "Space transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAirTransportFreightAirLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (freightAirTransport, navigator.nextPage(freightAirTransport, "").url),
          (spaceTransport, navigator.nextPage(spaceTransport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitAirTransportFreightAirLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitAirTransportFreightAirLvl4Page().url)
                .withFormUrlEncodedBody("airTransp4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitAirTransportFreightAirLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitAirTransportFreightAirLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of freight transport or space transport your undertaking provide"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAirTransportLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAirTransportLvl3Page()(
            FakeRequest(GET, routes.TransportController.loadAirTransportLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val html = contentAsString(result)
        html should include(routes.TransportController.loadAirTransportLvl3Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-freight-air-space", "Freight air transport and space transport"),
          ("sector-label-passenger-air", "Passenger air transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAirTransportLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (passengerAirTransport4, navigator.nextPage(passengerAirTransport4, "").url),
          (freightAndSpaceAirTransport, navigator.nextPage(freightAndSpaceAirTransport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitAirTransportLvl3Page()(
              FakeRequest(POST, routes.TransportController.submitAirTransportLvl3Page().url)
                .withFormUrlEncodedBody("airTransp3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitAirTransportLvl3Page()(
            FakeRequest(POST, routes.TransportController.submitAirTransportLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of air transport your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLandTransportFreightTransportLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadLandTransportFreightTransportLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadLandTransportFreightTransportLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val html = contentAsString(result)
        html should include(routes.TransportController.loadLandTransportFreightTransportLvl4Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-freight-road", "Freight transport by road"),
          ("sector-label-removal-services", "Removal services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLandTransportFreightTransportLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (freightRoadTransport4, navigator.nextPage(freightRoadTransport4, "").url),
          (removalServices, navigator.nextPage(removalServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitLandTransportFreightTransportLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitLandTransportFreightTransportLvl4Page().url)
                .withFormUrlEncodedBody("freight4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitLandTransportFreightTransportLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitLandTransportFreightTransportLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of freight transport or service your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLandTransportLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadLandTransportLvl3Page()(
            FakeRequest(GET, routes.TransportController.loadLandTransportLvl3Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadLandTransportLvl3Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-passenger-rail", "Passenger rail transport"),
          ("sector-label-other-passenger-land", "Other passenger land transport"),
          ("sector-label-freight-rail", "Freight rail transport"),
          ("sector-label-freight-road-removal", "Freight transport by road and removal services"),
          ("sector-label-pipeline-transport", "Transport via pipeline")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLandTransportLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (passengerRailTransport, navigator.nextPage(passengerRailTransport, "").url),
          (otherPassengerLandTransport, navigator.nextPage(otherPassengerLandTransport, "").url),
          (freightRailTransport4, navigator.nextPage(freightRailTransport4, "").url),
          (freightRoadTransport, navigator.nextPage(freightRoadTransport, "").url),
          (pipelineTransport4, navigator.nextPage(pipelineTransport4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitLandTransportLvl3Page()(
              FakeRequest(POST, routes.TransportController.submitLandTransportLvl3Page().url)
                .withFormUrlEncodedBody("landTransp3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitLandTransportLvl3Page()(
            FakeRequest(POST, routes.TransportController.submitLandTransportLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of land transport or transport via pipeline your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLandTransportOtherPassengerLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadLandTransportOtherPassengerLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadLandTransportOtherPassengerLvl4Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadLandTransportOtherPassengerLvl4Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-non-scheduled-passenger", "Non-scheduled passenger transport by road"),
          (
            "sector-label-on-demand-passenger",
            "On-demand passenger transport service activities by vehicle with driver"
          ),
          ("sector-label-cableways-ski-lifts", "Passenger transport by cableways and ski lifts"),
          ("sector-label-scheduled-passenger-road", "Scheduled passenger transport by road"),
          ("sector-label-other-land-transport", "Another type of passenger land transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLandTransportOtherPassengerLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (nonScheduledPassengerRoadTransport, navigator.nextPage(nonScheduledPassengerRoadTransport, "").url),
          (onDemandPassengerRoadTransport, navigator.nextPage(onDemandPassengerRoadTransport, "").url),
          (cablewaysPassengerTransport, navigator.nextPage(cablewaysPassengerTransport, "").url),
          (scheduledPassengerRoadTransport, navigator.nextPage(scheduledPassengerRoadTransport, "").url),
          (otherLandTransport, navigator.nextPage(otherLandTransport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitLandTransportOtherPassengerLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitLandTransportOtherPassengerLvl4Page().url)
                .withFormUrlEncodedBody("landOther4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitLandTransportOtherPassengerLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitLandTransportOtherPassengerLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of passenger land transport your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLandTransportPassengerRailLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadLandTransportPassengerRailLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadLandTransportPassengerRailLvl4Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadLandTransportPassengerRailLvl4Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-passenger-heavy-rail", "Passenger heavy rail transport"),
          ("sector-label-other-passenger-rail", "Other passenger rail transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLandTransportPassengerRailLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (passengerHeavyRailTransport, navigator.nextPage(passengerHeavyRailTransport, "").url),
          (otherPassengerRailTransport, navigator.nextPage(otherPassengerRailTransport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitLandTransportPassengerRailLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitLandTransportPassengerRailLvl4Page().url)
                .withFormUrlEncodedBody("landPassenger4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitLandTransportPassengerRailLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitLandTransportPassengerRailLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of passenger rail transport your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPostalAndCourierLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadPostalAndCourierLvl3Page()(
            FakeRequest(GET, routes.TransportController.loadPostalAndCourierLvl3Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadPostalAndCourierLvl3Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-postal-universal-service", "Postal activities under universal service obligation"),
          ("sector-label-other-postal-courier", "Other postal and courier activities"),
          (
            "sector-label-intermediation-postal-courier",
            "Intermediation service activities for postal and courier activities"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPostalAndCourierLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (postalActivities4, navigator.nextPage(postalActivities4, "").url),
          (otherPostalActivities4, navigator.nextPage(otherPostalActivities4, "").url),
          (postalIntermediationActivities4, navigator.nextPage(postalIntermediationActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitPostalAndCourierLvl3Page()(
              FakeRequest(POST, routes.TransportController.submitPostalAndCourierLvl3Page().url)
                .withFormUrlEncodedBody("postal3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitPostalAndCourierLvl3Page()(
            FakeRequest(POST, routes.TransportController.submitPostalAndCourierLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of postal or courier service does your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTransportLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadTransportLvl2Page()(
            FakeRequest(GET, routes.TransportController.loadTransportLvl2Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadTransportLvl2Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-airTransport", "Air transport"),
          ("sector-label-landTransport", "Land transport and transport via pipelines"),
          ("sector-label-postalAndCourierActivities", "Postal and courier activities"),
          (
            "sector-label-warehousingStorageTransportSupportActivities",
            "Warehousing, storage and support activities for transport"
          ),
          ("sector-label-waterTransport", "Water transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTransportLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (airTransport, navigator.nextPage(airTransport, "").url),
          (landTransport, navigator.nextPage(landTransport, "").url),
          (postalAndCourierActivities, navigator.nextPage(postalAndCourierActivities, "").url),
          (
            warehousingStorageTransportSupportActivities,
            navigator.nextPage(warehousingStorageTransportSupportActivities, "").url
          ),
          (waterTransport, navigator.nextPage(waterTransport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitTransportLvl2Page()(
              FakeRequest(POST, routes.TransportController.submitTransportLvl2Page().url)
                .withFormUrlEncodedBody("transport2" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitTransportLvl2Page()(
            FakeRequest(POST, routes.TransportController.submitTransportLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of transport and storage your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWarehousingSupportActivitiesTransportLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadWarehousingSupportActivitiesTransportLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadWarehousingSupportActivitiesTransportLvl4Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadWarehousingSupportActivitiesTransportLvl4Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-cargohandling", "Cargo handling"),
          ("sector-label-logistics-services", "Logistics service activities"),
          ("sector-label-air-transport-support", "Service activities incidental to air transport"),
          ("sector-label-land-transport-support", "Service activities incidental to land transport"),
          ("sector-label-water-transport-support", "Service activities incidental to water transport"),
          ("sector-label-other-transport-support", "Other support activities for transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWarehousingSupportActivitiesTransportLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cargoHandling, navigator.nextPage(cargoHandling, "").url),
          (logisticsServiceActivities, navigator.nextPage(logisticsServiceActivities, "").url),
          (airTransportServiceActivities, navigator.nextPage(airTransportServiceActivities, "").url),
          (landTransportServiceActivities, navigator.nextPage(landTransportServiceActivities, "").url),
          (waterTransportServiceActivities, navigator.nextPage(waterTransportServiceActivities, "").url),
          (otherTransportSupportActivities, navigator.nextPage(otherTransportSupportActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitWarehousingSupportActivitiesTransportLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitWarehousingSupportActivitiesTransportLvl4Page().url)
                .withFormUrlEncodedBody("wHouseSupport4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitWarehousingSupportActivitiesTransportLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitWarehousingSupportActivitiesTransportLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of transport support activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWarehousingIntermediationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadWarehousingIntermediationLvl4Page()(
            FakeRequest(GET, routes.TransportController.loadWarehousingIntermediationLvl4Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadWarehousingIntermediationLvl4Page().url)
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-freightTransportIntermediationServiceActivities",
            "Intermediation service activities for freight transport"
          ),
          (
            "sector-label-passengerTransportIntermediationServiceActivities",
            "Intermediation service activities for passenger transport"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWarehousingIntermediationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (
            freightTransportIntermediationServiceActivities,
            navigator.nextPage(freightTransportIntermediationServiceActivities, "").url
          ),
          (
            passengerTransportIntermediationServiceActivities,
            navigator.nextPage(passengerTransportIntermediationServiceActivities, "").url
          )
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitWarehousingIntermediationLvl4Page()(
              FakeRequest(POST, routes.TransportController.submitWarehousingIntermediationLvl4Page().url)
                .withFormUrlEncodedBody("wHouseInt4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitWarehousingIntermediationLvl4Page()(
            FakeRequest(POST, routes.TransportController.submitWarehousingIntermediationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of transport intermediation service your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWaterTransportLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadWaterTransportLvl3Page()(
            FakeRequest(GET, routes.TransportController.loadWaterTransportLvl3Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadWaterTransportLvl3Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-inland-freight", "Inland freight water transport"),
          ("sector-label-inland-passenger", "Inland passenger water transport"),
          ("sector-label-sea-coastal-freight", "Sea and coastal freight water transport"),
          ("sector-label-sea-coastal-passenger", "Sea and coastal passenger water transport")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWaterTransportLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (inlandFreightWaterTransport4, navigator.nextPage(inlandFreightWaterTransport4, "").url),
          (inlandPassengerWaterTransport4, navigator.nextPage(inlandPassengerWaterTransport4, "").url),
          (coastalFreightWaterTransport4, navigator.nextPage(coastalFreightWaterTransport4, "").url),
          (coastalPassengerWaterTransport4, navigator.nextPage(coastalPassengerWaterTransport4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitWaterTransportLvl3Page()(
              FakeRequest(POST, routes.TransportController.submitWaterTransportLvl3Page().url)
                .withFormUrlEncodedBody("waterTransp3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitWaterTransportLvl3Page()(
            FakeRequest(POST, routes.TransportController.submitWaterTransportLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of water transport your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWarehousingSupportLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.loadWarehousingSupportLvl3Page()(
            FakeRequest(GET, routes.TransportController.loadWarehousingSupportLvl3Page().url)
          )
        status(result) shouldBe OK
        val html = contentAsString(result)
        val document = Jsoup.parse(contentAsString(result))
        html should include(routes.TransportController.loadWarehousingSupportLvl3Page().url)
        val radios = Table(
          ("id", "text"),
          ("sector-label-transportIntermediationServiceActivities", "Intermediation service activities for transport"),
          ("sector-label-transportSupportActivities", "Support activities for transport"),
          ("sector-label-warehousingStorage4", "Warehousing and storage")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWarehousingSupportLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (
            transportIntermediationServiceActivities,
            navigator.nextPage(transportIntermediationServiceActivities, "").url
          ),
          (transportSupportActivities, navigator.nextPage(transportSupportActivities, "").url),
          (warehousingStorage4, navigator.nextPage(warehousingStorage4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence { mockAuthWithEnrolment() }
          val result =
            controller.submitWarehousingSupportLvl3Page()(
              FakeRequest(POST, routes.TransportController.submitWarehousingSupportLvl3Page().url)
                .withFormUrlEncodedBody("wHouse3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence { mockAuthWithEnrolment() }
        val result =
          controller.submitWarehousingSupportLvl3Page()(
            FakeRequest(POST, routes.TransportController.submitWarehousingSupportLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of warehousing, storage or support activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
