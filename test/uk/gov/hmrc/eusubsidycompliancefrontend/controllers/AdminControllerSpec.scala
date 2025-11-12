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
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class AdminControllerSpec
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

  private val controller = instanceOf[AdminController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val RentalAndLeasing = "77"
    val MotorVehiclesRental = "77.1"
    val CarsLightVehicles = "77.11"
    val Trucks = "77.12"
    val PersonalRental = "77.2"
    val RecreationalSportsGoods = "77.21"
    val OtherPersonalHouseholdGoods = "77.22"
    val OtherMachineryRental = "77.3"
    val AgriculturalMachineryRental = "77.31"
    val AirTransportRental = "77.35"
    val ConstructionMachineryRental = "77.32"
    val OfficeMachineryRental = "77.33"
    val WaterTransportRental = "77.34"
    val OtherMachineryRental4 = "77.39"
    val LeasingIntellectualProperty = "77.40"
    val TangibleGoodsRentalIntermediation = "77.5"
    val CarsMotorhomesTrailers = "77.51"
    val otherTangibleGoodsRental = "77.52"
    val Employment = "78"
    val employmentPlacementActivities4 = "78.10"
    val tempEmploymentPlacementActivities4 = "78.20"
    val TravelAgencyAndReservation = "79"
    val TravelAgency = "79.1"
    val TourOperator = "79.12"
    val TravelAgencyActivities = "79.11"
    val OtherReservationServices4 = "79.90"
    val InvestigationAndSecurityActivities = "80.0"
    val PrivateInvestigationAndSecurityActivities = "80.01"
    val OtherInvestigationAndSecurityActivities = "80.09"
    val BuildingsAndLandscapingServices = "81"
    val combinedFacilitiesSupport4 = "81.10"
    val Cleaning = "81.2"
    val GeneralCleaning = "81.21"
    val IndustrialCleaning = "81.22"
    val OtherCleaning = "81.23"
    val landscapeServiceActivities = "81.30"
    val OfficeAdministrativeSupport = "82"
    val officeAdministrativeActivities4 = "82.10"
    val callCentresActivities = "82.20"
    val conventionsOrganisation4 = "82.30"
    val businessSupportIntermediation4 = "82.40"
    val otherBusinessSupportIntermediationService = "82.9"
    val CollectionAgencies = "82.91"
    val PackagingActivities = "82.92"
    val OtherBusinessSupport = "82.99"
  }

  import SectorCodes._

  "AdminController" should {
    /* ------------------------- Admin Views  -------------------------*/
    "loadAdministrativeLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAdministrativeLvl2Page()(
            FakeRequest(GET, routes.AdminController.loadAdministrativeLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-employment", "Employment"),
          ("sector-label-investigation", "Investigation and security"),
          ("sector-label-office", "Office administrative, office support and other business support"),
          ("sector-label-rental", "Rental and leasing"),
          ("sector-label-buildings", "Services to buildings and landscaping"),
          ("sector-label-travel", "Travel agency, tour operator and other reservation services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAdministrativeLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (RentalAndLeasing, navigator.nextPage(RentalAndLeasing, "").url),
          (Employment, navigator.nextPage(Employment, "").url),
          (TravelAgencyAndReservation, navigator.nextPage(TravelAgencyAndReservation, "").url),
          (InvestigationAndSecurityActivities, navigator.nextPage(InvestigationAndSecurityActivities, "").url),
          (BuildingsAndLandscapingServices, navigator.nextPage(BuildingsAndLandscapingServices, "").url),
          (OfficeAdministrativeSupport, navigator.nextPage(OfficeAdministrativeSupport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAdministrativeLvl2Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitAdministrativeLvl2Page().url
              )
                .withFormUrlEncodedBody("admin2" -> value)
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
          controller.submitAdministrativeLvl2Page()(
            FakeRequest(POST, routes.AdminController.submitAdministrativeLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of administrative and support services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadTravelLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTravelLvl3Page()(
            FakeRequest(GET, routes.AdminController.loadTravelLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-travel-agency", "Travel agency and tour operator activities"),
          ("sector-label-other-reservation", "Other reservation services and related activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTravelLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (TravelAgency, navigator.nextPage(TravelAgency, "").url),
          (OtherReservationServices4, navigator.nextPage(OtherReservationServices4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTravelLvl3Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitTravelLvl3Page().url
              )
                .withFormUrlEncodedBody("travel3" -> value)
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
          controller.submitTravelLvl3Page()(
            FakeRequest(POST, routes.AdminController.submitTravelLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in travel and other reservation services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadTravelAgencyLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTravelAgencyLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadTravelAgencyLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-tour-operator", "Tour operator activities"),
          ("sector-label-travel-agency", "Travel agency activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTravelAgencyLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (TourOperator, navigator.nextPage(TourOperator, "").url),
          (TravelAgencyActivities, navigator.nextPage(TravelAgencyActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTravelAgencyLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitTravelAgencyLvl4Page().url
              )
                .withFormUrlEncodedBody("travelAgency4" -> value)
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
          controller.submitTravelAgencyLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitTravelAgencyLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in travel agency and tour operator activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRentalLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRentalLvl3Page()(
            FakeRequest(GET, routes.AdminController.loadRentalLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motor-vehicles", "Motor vehicles"),
          ("sector-label-personal-household", "Personal and household goods"),
          ("sector-label-machinery-equipment", "Other machinery, equipment and tangible goods"),
          (
            "sector-label-intellectual-property",
            "Intellectual property and similar products (except copyrighted works)"
          ),
          (
            "sector-label-intermediation",
            "My undertaking provides intermediation services for rental and leasing of tangible goods and non-financial intangible assets"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitRentalLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (MotorVehiclesRental, navigator.nextPage(MotorVehiclesRental, "").url),
          (PersonalRental, navigator.nextPage(PersonalRental, "").url),
          (OtherMachineryRental, navigator.nextPage(OtherMachineryRental, "").url),
          (LeasingIntellectualProperty, navigator.nextPage(LeasingIntellectualProperty, "").url),
          (TangibleGoodsRentalIntermediation, navigator.nextPage(TangibleGoodsRentalIntermediation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRentalLvl3Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitRentalLvl3Page().url
              )
                .withFormUrlEncodedBody("rental3" -> value)
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
          controller.submitRentalLvl3Page()(
            FakeRequest(POST, routes.AdminController.submitRentalLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of goods your undertaking rents or leases"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadPersonalHouseholdLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPersonalHouseholdLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadPersonalHouseholdLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-recreational", "Recreational and sports goods"),
          ("sector-label-other-personal", "Other personal and household goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPersonalHouseholdLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (RecreationalSportsGoods, navigator.nextPage(RecreationalSportsGoods, "").url),
          (OtherPersonalHouseholdGoods, navigator.nextPage(OtherPersonalHouseholdGoods, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPersonalHouseholdLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitPersonalHouseholdLvl4Page().url
              )
                .withFormUrlEncodedBody("personalHouse4" -> value)
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
          controller.submitPersonalHouseholdLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitPersonalHouseholdLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of personal or household goods your undertaking rents or leases"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherBusinessSupportLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherBusinessSupportLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadOtherBusinessSupportLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-collection-agencies", "Collection agencies and credit bureaus"),
          ("sector-label-packaging", "Packaging activities"),
          ("sector-label-other-business-activities", "Other business support service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherBusinessSupportLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (CollectionAgencies, navigator.nextPage(CollectionAgencies, "").url),
          (PackagingActivities, navigator.nextPage(PackagingActivities, "").url),
          (OtherBusinessSupport, navigator.nextPage(OtherBusinessSupport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherBusinessSupportLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitOtherBusinessSupportLvl4Page().url
              )
                .withFormUrlEncodedBody("otherBusSupport4" -> value)
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
          controller.submitOtherBusinessSupportLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitOtherBusinessSupportLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other business support services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOfficeLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOfficeLvl3Page()(
            FakeRequest(GET, routes.AdminController.loadOfficeLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-call-centres", "Call centres"),
          ("sector-label-intermediation", "Intermediation services for business support"),
          ("sector-label-office-admin", "Office administrative and support activities"),
          ("sector-label-conventions", "Organisation of conventions and trade shows"),
          ("sector-label-other-business", "Other business support service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOfficeLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (callCentresActivities, navigator.nextPage(callCentresActivities, "").url),
          (businessSupportIntermediation4, navigator.nextPage(businessSupportIntermediation4, "").url),
          (officeAdministrativeActivities4, navigator.nextPage(officeAdministrativeActivities4, "").url),
          (conventionsOrganisation4, navigator.nextPage(conventionsOrganisation4, "").url),
          (
            otherBusinessSupportIntermediationService,
            navigator.nextPage(otherBusinessSupportIntermediationService, "").url
          )
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOfficeLvl3Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitOfficeLvl3Page().url
              )
                .withFormUrlEncodedBody("office3" -> value)
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
          controller.submitOfficeLvl3Page()(
            FakeRequest(POST, routes.AdminController.submitOfficeLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of office or business support your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadMotorVehiclesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMotorVehiclesLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadMotorVehiclesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cars", "Cars and light motor vehicles"),
          ("sector-label-trucks", "Trucks")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMotorVehiclesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (Trucks, navigator.nextPage(Trucks, "").url),
          (CarsLightVehicles, navigator.nextPage(CarsLightVehicles, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorVehiclesLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitMotorVehiclesLvl4Page().url
              )
                .withFormUrlEncodedBody("vehicles4" -> value)
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
          controller.submitMotorVehiclesLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitMotorVehiclesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of motor vehicles your undertaking rents or leases"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadMachineryEquipmentLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMachineryEquipmentLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadMachineryEquipmentLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-agricultural-machinery", "Agricultural machinery and equipment"),
          ("sector-label-air-transport-equipment", "Air transport equipment"),
          ("sector-label-construction-machinery", "Construction and civil engineering machinery and equipment"),
          ("sector-label-office-machinery", "Office machinery, equipment and computers"),
          ("sector-label-water-transport-equipment", "Water transport equipment"),
          ("sector-label-other-machinery", "Other machinery, equipment and tangible goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMachineryEquipmentLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (AgriculturalMachineryRental, navigator.nextPage(AgriculturalMachineryRental, "").url),
          (AirTransportRental, navigator.nextPage(AirTransportRental, "").url),
          (ConstructionMachineryRental, navigator.nextPage(ConstructionMachineryRental, "").url),
          (OfficeMachineryRental, navigator.nextPage(OfficeMachineryRental, "").url),
          (WaterTransportRental, navigator.nextPage(WaterTransportRental, "").url),
          (OtherMachineryRental4, navigator.nextPage(OtherMachineryRental4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMachineryEquipmentLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitMachineryEquipmentLvl4Page().url
              )
                .withFormUrlEncodedBody("equipment4" -> value)
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
          controller.submitMachineryEquipmentLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitMachineryEquipmentLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of other machinery, equipment and tangible goods your undertaking rents or leases"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadInvestigationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadInvestigationLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadInvestigationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-investigation", "Investigation and private security activities"),
          ("sector-label-other-security", "Another type of security activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitInvestigationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (
            PrivateInvestigationAndSecurityActivities,
            navigator.nextPage(PrivateInvestigationAndSecurityActivities, "").url
          ),
          (OtherInvestigationAndSecurityActivities, navigator.nextPage(OtherInvestigationAndSecurityActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitInvestigationLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitInvestigationLvl4Page().url
              )
                .withFormUrlEncodedBody("investigation4" -> value)
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
          controller.submitInvestigationLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitInvestigationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in investigation and security"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadIntermediationServicesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadIntermediationServicesLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadIntermediationServicesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cars-motorhomes", "Cars, motorhomes and trailers"),
          ("sector-label-other-tangible", "Other tangible goods and non-financial intangible assets")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitIntermediationServicesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (CarsMotorhomesTrailers, navigator.nextPage(CarsMotorhomesTrailers, "").url),
          (otherTangibleGoodsRental, navigator.nextPage(otherTangibleGoodsRental, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitIntermediationServicesLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitIntermediationServicesLvl4Page().url
              )
                .withFormUrlEncodedBody("intermediation4" -> value)
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
          controller.submitIntermediationServicesLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitIntermediationServicesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of goods for which your undertaking provides intermediation services for rental and leasing"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadEmploymentLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadEmploymentLvl3Page()(
            FakeRequest(GET, routes.AdminController.loadEmploymentLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-temporary-employment",
            "Temporary employment agency activities and other human resource provisions"
          ),
          ("sector-label-employment-placement", "Activities of employment placement agencies")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitEmploymentLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (employmentPlacementActivities4, navigator.nextPage(employmentPlacementActivities4, "").url),
          (tempEmploymentPlacementActivities4, navigator.nextPage(tempEmploymentPlacementActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitEmploymentLvl3Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitEmploymentLvl3Page().url
              )
                .withFormUrlEncodedBody("employment3" -> value)
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
          controller.submitEmploymentLvl3Page()(
            FakeRequest(POST, routes.AdminController.submitEmploymentLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in employment services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadCleaningLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCleaningLvl4Page()(
            FakeRequest(GET, routes.AdminController.loadCleaningLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-general-cleaning", "General cleaning of buildings"),
          ("sector-label-industrial-cleaning", "Other building and industrial cleaning activities"),
          ("sector-label-other-cleaning", "Other cleaning activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCleaningLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (GeneralCleaning, navigator.nextPage(GeneralCleaning, "").url),
          (IndustrialCleaning, navigator.nextPage(IndustrialCleaning, "").url),
          (OtherCleaning, navigator.nextPage(OtherCleaning, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCleaningLvl4Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitCleaningLvl4Page().url
              )
                .withFormUrlEncodedBody("cleaning4" -> value)
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
          controller.submitCleaningLvl4Page()(
            FakeRequest(POST, routes.AdminController.submitCleaningLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of cleaning your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadBuildingsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBuildingsLvl3Page()(
            FakeRequest(GET, routes.AdminController.loadBuildingsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cleaning", "Cleaning"),
          ("sector-label-combined-facilities", "Combined facilities support"),
          ("sector-label-landscaping", "Landscaping")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBuildingsLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (Cleaning, navigator.nextPage(Cleaning, "").url),
          (combinedFacilitiesSupport4, navigator.nextPage(combinedFacilitiesSupport4, "").url),
          (landscapeServiceActivities, navigator.nextPage(landscapeServiceActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBuildingsLvl3Page()(
              FakeRequest(
                POST,
                routes.AdminController.submitBuildingsLvl3Page().url
              )
                .withFormUrlEncodedBody("building3" -> value)
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
          controller.submitBuildingsLvl3Page()(
            FakeRequest(POST, routes.AdminController.submitBuildingsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of building or landscape service your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
