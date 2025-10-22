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
  }
}
