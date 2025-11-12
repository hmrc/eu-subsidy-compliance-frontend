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

class ConstructionControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {
  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2025)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[ConstructionController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val residentialNonResidentialConstruction4 = "41.00"
    val civilEngineering = "42"
    val roadAndRailConstruction = "42.1"
    val bridgesTunnels = "42.13"
    val railwaysUnderground = "42.12"
    val roadsMotorways = "42.11"
    val utilityConstruction = "42.2"
    val electricityUtilityConstruction = "42.22"
    val fluidsUtilityConstruction = "42.21"
    val otherUtilityConstructionProjects = "42.9"
    val waterProjects = "42.91"
    val otherCivilEngineering = "42.99"
    val specialisedConstructionActivities = "43"
    val demolitionAndPreparation = "43.1"
    val demolition = "43.11"
    val sitePreparation = "43.12"
    val testDrillingBoring = "43.13"
    val electricalAndPlumbingInstallation = "43.2"
    val electricalInstallation = "43.21"
    val insulationInstallation = "43.23"
    val plumbingHeatingAC = "43.22"
    val otherConstructionInstallation = "43.24"
    val buildingFinishing = "43.3"
    val floorAndWallCovering = "43.33"
    val joineryInstallation = "43.32"
    val paintingAndGlazing = "43.34"
    val plastering = "43.31"
    val otherBuildingFinishing = "43.35"
    val specialisedBuildingActivities = "43.4"
    val roofingActivities = "43.41"
    val otherSpecialisedBuildingActivities = "43.42"
    val specialisedCivilEngineering = "43.5"
    val specialisedCivilEngineering4 = "43.50"
    val specialisedConstructionIntermediationServices = "43.6"
    val specialisedConstructionIntermediationServices4 = "43.60"
    val otherSpecialisedConstruction = "43.9"
    val masonryAndBricklaying = "43.91"
    val otherSpecialisedConstructionActivities = "43.99"
  }

  import SectorCodes._

  "ConstructionController" should {
    "loadConstructionLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadConstructionLvl2Page()(
            FakeRequest(GET, routes.ConstructionController.loadConstructionLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-civilEngineering", "Civil engineering"),
          (
            "sector-label-residentialNonResidentialConstruction",
            "Construction of residential and non-residential buildings"
          ),
          ("sector-label-specialisedConstructionActivities", "Specialised construction activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitConstructionLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (civilEngineering, navigator.nextPage(civilEngineering, "").url),
          (residentialNonResidentialConstruction4, navigator.nextPage(residentialNonResidentialConstruction4, "").url),
          (specialisedConstructionActivities, navigator.nextPage(specialisedConstructionActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitConstructionLvl2Page()(
              FakeRequest(POST, routes.ConstructionController.submitConstructionLvl2Page().url)
                .withFormUrlEncodedBody("construction2" -> value)
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
          controller.submitConstructionLvl2Page()(
            FakeRequest(POST, routes.ConstructionController.submitConstructionLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in construction"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadCivilEngineeringLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCivilEngineeringLvl3Page()(
            FakeRequest(GET, routes.ConstructionController.loadCivilEngineeringLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-roadAndRailConstruction", "Construction of roads and railways"),
          ("sector-label-utilityConstruction", "Construction of utility projects"),
          ("sector-label-otherUtilityConstructionProjects", "Construction of other civil engineering projects")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCivilEngineeringLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (roadAndRailConstruction, navigator.nextPage(roadAndRailConstruction, "").url),
          (utilityConstruction, navigator.nextPage(utilityConstruction, "").url),
          (otherUtilityConstructionProjects, navigator.nextPage(otherUtilityConstructionProjects, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCivilEngineeringLvl3Page()(
              FakeRequest(POST, routes.ConstructionController.submitCivilEngineeringLvl3Page().url)
                .withFormUrlEncodedBody("civilEngineering3" -> value)
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
          controller.submitCivilEngineeringLvl3Page()(
            FakeRequest(POST, routes.ConstructionController.submitCivilEngineeringLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in civil engineering"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadBuildingCompletionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBuildingCompletionLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadBuildingCompletionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-floorAndWallCovering", "Floor and wall covering"),
          ("sector-label-joineryInstallation", "Joinery installation"),
          ("sector-label-paintingAndGlazing", "Painting and glazing"),
          ("sector-label-plastering", "Plastering"),
          ("sector-label-otherBuildingFinishing", "Other building completion and finishing")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBuildingCompletionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (floorAndWallCovering, navigator.nextPage(floorAndWallCovering, "").url),
          (joineryInstallation, navigator.nextPage(joineryInstallation, "").url),
          (paintingAndGlazing, navigator.nextPage(paintingAndGlazing, "").url),
          (plastering, navigator.nextPage(plastering, "").url),
          (otherBuildingFinishing, navigator.nextPage(otherBuildingFinishing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBuildingCompletionLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitBuildingCompletionLvl4Page().url)
                .withFormUrlEncodedBody("building4" -> value)
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
          controller.submitBuildingCompletionLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitBuildingCompletionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of building completion and finishing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadConstructionRoadsRailwaysLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadConstructionRoadsRailwaysLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadConstructionRoadsRailwaysLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-roadsMotorways", "Roads and motorways"),
          (
            "sector-label-railwaysUnderground",
            "Railways and underground railways"
          ),
          ("sector-label-bridgesTunnels", "Bridges and tunnels")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitConstructionRoadsRailwaysLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (roadsMotorways, navigator.nextPage(roadsMotorways, "").url),
          (railwaysUnderground, navigator.nextPage(railwaysUnderground, "").url),
          (bridgesTunnels, navigator.nextPage(bridgesTunnels, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitConstructionRoadsRailwaysLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitConstructionRoadsRailwaysLvl4Page().url)
                .withFormUrlEncodedBody("roads4" -> value)
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
          controller.submitConstructionRoadsRailwaysLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitConstructionRoadsRailwaysLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of roads or railways your undertaking works on"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadConstructionUtilityProjectsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadConstructionUtilityProjectsLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadConstructionUtilityProjectsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-electricityUtilityConstruction", "Utility projects for electricity and telecommunications"),
          ("sector-label-fluidsUtilityConstruction", "Utility projects for fluids")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitConstructionUtilityProjectsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricityUtilityConstruction, navigator.nextPage(electricityUtilityConstruction, "").url),
          (fluidsUtilityConstruction, navigator.nextPage(fluidsUtilityConstruction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitConstructionUtilityProjectsLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitConstructionUtilityProjectsLvl4Page().url)
                .withFormUrlEncodedBody("utility4" -> value)
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
          controller.submitConstructionUtilityProjectsLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitConstructionUtilityProjectsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of utility projects your undertaking works on"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadDemolitionSitePreparationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadDemolitionSitePreparationLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadDemolitionSitePreparationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-demolition", "Demolition"),
          (
            "sector-label-sitePreparation",
            "Site preparation"
          ),
          ("sector-label-testDrillingBoring", "Test drilling and boring")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitDemolitionSitePreparationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (demolition, navigator.nextPage(demolition, "").url),
          (sitePreparation, navigator.nextPage(sitePreparation, "").url),
          (testDrillingBoring, navigator.nextPage(testDrillingBoring, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitDemolitionSitePreparationLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitDemolitionSitePreparationLvl4Page().url)
                .withFormUrlEncodedBody("demo4" -> value)
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
          controller.submitDemolitionSitePreparationLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitDemolitionSitePreparationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of demolition or site preparation your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadElectricalPlumbingConstructionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadElectricalPlumbingConstructionLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadElectricalPlumbingConstructionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-electricalInstallation", "Electrical installation"),
          ("sector-label-plumbingHeatingAC", "Plumbing, heat and air conditioning installation"),
          (
            "sector-label-insulationInstallation",
            "Installation of insulation"
          ),
          ("sector-label-otherConstructionInstallation", "Other construction installation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitElectricalPlumbingConstructionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricalInstallation, navigator.nextPage(electricalInstallation, "").url),
          (insulationInstallation, navigator.nextPage(insulationInstallation, "").url),
          (plumbingHeatingAC, navigator.nextPage(plumbingHeatingAC, "").url),
          (otherConstructionInstallation, navigator.nextPage(otherConstructionInstallation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitElectricalPlumbingConstructionLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitElectricalPlumbingConstructionLvl4Page().url)
                .withFormUrlEncodedBody("plumbing4" -> value)
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
          controller.submitElectricalPlumbingConstructionLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitElectricalPlumbingConstructionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of construction installation your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherCivilEngineeringProjectsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherCivilEngineeringProjectsLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadOtherCivilEngineeringProjectsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-waterProjects", "Water projects"),
          ("sector-label-otherCivilEngineering", "Other civil engineering projects")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherCivilEngineeringProjectsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (waterProjects, navigator.nextPage(waterProjects, "").url),
          (otherCivilEngineering, navigator.nextPage(otherCivilEngineering, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherCivilEngineeringProjectsLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitOtherCivilEngineeringProjectsLvl4Page().url)
                .withFormUrlEncodedBody("otherCivil4" -> value)
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
          controller.submitOtherCivilEngineeringProjectsLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitOtherCivilEngineeringProjectsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of civil engineering projects your undertaking works on"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherSpecialisedConstructionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherSpecialisedConstructionLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadOtherSpecialisedConstructionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-masonryAndBricklaying", "Masonry and bricklaying"),
          ("sector-label-otherSpecialisedConstructionActivities", "Other specialised construction activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherSpecialisedConstructionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (masonryAndBricklaying, navigator.nextPage(masonryAndBricklaying, "").url),
          (otherSpecialisedConstructionActivities, navigator.nextPage(otherSpecialisedConstructionActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherSpecialisedConstructionLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitOtherSpecialisedConstructionLvl4Page().url)
                .withFormUrlEncodedBody("otherSpecial4" -> value)
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
          controller.submitOtherSpecialisedConstructionLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitOtherSpecialisedConstructionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other specialised construction activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadSpecialisedConstructionLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSpecialisedConstructionLvl3Page()(
            FakeRequest(GET, routes.ConstructionController.loadSpecialisedConstructionLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-buildingFinishing", "Building completion and finishing"),
          ("sector-label-demolitionAndPreparation", "Demolition and site preparation"),
          (
            "sector-label-electricalAndPlumbingInstallation",
            "Electrical, plumbing and other construction installation"
          ),
          (
            "sector-label-specialisedConstructionIntermediationServices",
            "Intermediation service activities for specialised construction services"
          ),
          ("sector-label-specialisedCivilEngineering", "Specialised construction activities in civil engineering"),
          (
            "sector-label-specialisedBuildingActivities",
            "Specialised construction activities in construction of buildings"
          ),
          ("sector-label-otherSpecialisedConstruction", "Other specialised construction activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSpecialisedConstructionLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (buildingFinishing, navigator.nextPage(buildingFinishing, "").url),
          (demolitionAndPreparation, navigator.nextPage(demolitionAndPreparation, "").url),
          (electricalAndPlumbingInstallation, navigator.nextPage(electricalAndPlumbingInstallation, "").url),
          (
            specialisedConstructionIntermediationServices4,
            navigator.nextPage(specialisedConstructionIntermediationServices4, "").url
          ),
          (specialisedCivilEngineering4, navigator.nextPage(specialisedCivilEngineering4, "").url),
          (specialisedBuildingActivities, navigator.nextPage(specialisedBuildingActivities, "").url),
          (otherSpecialisedConstruction, navigator.nextPage(otherSpecialisedConstruction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSpecialisedConstructionLvl3Page()(
              FakeRequest(POST, routes.ConstructionController.submitSpecialisedConstructionLvl3Page().url)
                .withFormUrlEncodedBody("special3" -> value)
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
          controller.submitSpecialisedConstructionLvl3Page()(
            FakeRequest(POST, routes.ConstructionController.submitSpecialisedConstructionLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of specialised construction activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadSpecialisedConstructionActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSpecialisedConstructionActivitiesLvl4Page()(
            FakeRequest(GET, routes.ConstructionController.loadSpecialisedConstructionActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-roofingActivities", "Roofing activities"),
          (
            "sector-label-otherSpecialisedBuildingActivities",
            "Other specialised construction activities in construction of buildings"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSpecialisedConstructionActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (roofingActivities, navigator.nextPage(roofingActivities, "").url),
          (otherSpecialisedBuildingActivities, navigator.nextPage(otherSpecialisedBuildingActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSpecialisedConstructionActivitiesLvl4Page()(
              FakeRequest(POST, routes.ConstructionController.submitSpecialisedConstructionActivitiesLvl4Page().url)
                .withFormUrlEncodedBody("special4" -> value)
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
          controller.submitSpecialisedConstructionActivitiesLvl4Page()(
            FakeRequest(POST, routes.ConstructionController.submitSpecialisedConstructionActivitiesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of building construction activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
