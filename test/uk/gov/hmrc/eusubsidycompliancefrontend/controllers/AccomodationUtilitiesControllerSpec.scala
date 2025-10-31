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

class AccomodationUtilitiesControllerSpec
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

  private val controller = instanceOf[AccomodationUtilitiesController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val accommodation = "55"
    val hotelsAccommodation = "55.1"
    val hotelsAccommodation4 = "55.10"
    val holidayShortStay = "55.2"
    val holidayShortStay4 = "55.20"
    val campingGrounds = "55.3"
    val campingGrounds4 = "55.30"
    val intermediationAccommodation = "55.4"
    val intermediationAccommodation4 = "55.40"
    val otherAccommodation = "55.9"
    val otherAccommodation4 = "55.90"

    val foodBeverageServiceActivities = "56"
    val restaurantsMobile = "56.1"
    val restaurantsMobile4 = "56.11"
    val mobileFoodServices = "56.12"
    val eventAndContractCatering = "56.2"
    val eventCatering = "56.21"
    val contractCatering = "56.22"
    val beverageServing = "56.3"
    val beverageServing4 = "56.30"
    val foodBeverageServiceIntermediationActivities = "56.4"
    val foodBeverageServiceIntermediationActivities4 = "56.40"

    val electricPowerGenerationAndDistribution = "35.1"
    val electricityDistribution = "35.14"
    val nonRenewableElectricityProduction = "35.11"
    val renewableElectricityProduction = "35.12"
    val electricityStorage = "35.16"
    val electricityTrade = "35.15"
    val electricityTransmission = "35.13"
    val gaseousFuelsManufacture = "35.2"
    val gaseousFuelDistribution = "35.22"
    val gaseousFuelManufacture = "35.21"
    val gaseousFuelStorage = "35.24"
    val gaseousFuelTrade = "35.23"
    val steamAndAirConditioningSupply = "35.3"
    val steamAndAirConditioningSupply4 = "35.30"
    val electricPowerAndGasBrokers = "35.4"
    val electricPowerAndGasBrokers4 = "35.40"

    val waterTreatment = "36"
    val waterTreatment3 = "36.0"
    val waterTreatment4 = "36.00"
    val sewerage = "37"
    val sewerage3 = "37.0"
    val sewerage4 = "37.00"
    val wasteManagement = "38"
    val wasteCollection = "38.1"
    val hazardousWaste = "38.12"
    val nonHazardousWaste = "38.11"
    val wasteRecovery = "38.2"
    val energyRecovery = "38.22"
    val materialsRecovery = "38.21"
    val otherWasteRecovery = "38.23"
    val wasteDisposal = "38.3"
    val incineration = "38.31"
    val landFilling = "38.32"
    val otherWasteDisposal = "38.33"
    val wasteManagementRemediation = "39"
    val wasteManagementRemediation3 = "39.0"
    val wasteManagementRemediation4 = "39.00"
  }

  import SectorCodes._

  "AccomodationUtilitiesController" should {
    /* ------------------------- Accomodation Views  -------------------------*/
    "loadAccommodationFoodLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAccommodationFoodLvl2Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadAccommodationFoodLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-accommodation", "Accommodation"),
          ("sector-label-foodService", "Food and beverage service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAccommodationFoodLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (accommodation, navigator.nextPage(accommodation, "").url),
          (foodBeverageServiceActivities, navigator.nextPage(foodBeverageServiceActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAccommodationFoodLvl2Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitAccommodationFoodLvl2Page().url
              )
                .withFormUrlEncodedBody("accommodation2" -> value)
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
          controller.submitAccommodationFoodLvl2Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitAccommodationFoodLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in accommodation and food services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadAccommodationLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAccommodationLvl3Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadAccommodationLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-campingGrounds", "Camping grounds and recreational vehicle parks"),
          ("sector-label-holidayShortStay", "Holiday and other short-stay accommodation"),
          ("sector-label-hotelsAccommodation", "Hotels and similar accommodation"),
          ("sector-label-intermediationAccommodation", "Intermediation service activities for accommodation"),
          ("sector-label-otherAccommodation", "Other accommodation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAccommodationLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (hotelsAccommodation4, navigator.nextPage(hotelsAccommodation4, "").url),
          (holidayShortStay4, navigator.nextPage(holidayShortStay4, "").url),
          (campingGrounds4, navigator.nextPage(campingGrounds4, "").url),
          (intermediationAccommodation4, navigator.nextPage(intermediationAccommodation4, "").url),
          (otherAccommodation4, navigator.nextPage(otherAccommodation4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAccommodationLvl3Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitAccommodationLvl3Page().url
              )
                .withFormUrlEncodedBody("accommodation3" -> value)
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
          controller.submitAccommodationLvl3Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitAccommodationLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in accommodation"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadFoodBeverageActivitiesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFoodBeverageActivitiesLvl3Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadFoodBeverageActivitiesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-restaurantsMobile", "Restaurants and mobile food services"),
          (
            "sector-label-eventAndContractCatering",
            "Event catering, contract catering and other food service activities"
          ),
          ("sector-label-beverageServing", "Beverage serving activities"),
          (
            "sector-label-foodBeverageServiceIntermediationActivities",
            "Intermediation services for food and beverage service activities"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitFoodBeverageActivitiesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (restaurantsMobile, navigator.nextPage(restaurantsMobile, "").url),
          (
            foodBeverageServiceIntermediationActivities4,
            navigator.nextPage(foodBeverageServiceIntermediationActivities4, "").url
          ),
          (beverageServing4, navigator.nextPage(beverageServing4, "").url),
          (eventAndContractCatering, navigator.nextPage(eventAndContractCatering, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFoodBeverageActivitiesLvl3Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitFoodBeverageActivitiesLvl3Page().url
              )
                .withFormUrlEncodedBody("foodActs3" -> value)
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
          controller.submitFoodBeverageActivitiesLvl3Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitFoodBeverageActivitiesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in food and beverage services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadEventCateringOtherFoodActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadEventCateringOtherFoodActivitiesLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadEventCateringOtherFoodActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-eventCatering", "Event catering"),
          ("sector-label-contractCatering", "Contract catering and other food service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitEventCateringOtherFoodActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (eventCatering, navigator.nextPage(eventCatering, "").url),
          (contractCatering, navigator.nextPage(contractCatering, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitEventCateringOtherFoodActivitiesLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitEventCateringOtherFoodActivitiesLvl4Page().url
              )
                .withFormUrlEncodedBody("catering4" -> value)
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
          controller.submitEventCateringOtherFoodActivitiesLvl4Page()(
            FakeRequest(
              POST,
              routes.AccomodationUtilitiesController.submitEventCateringOtherFoodActivitiesLvl4Page().url
            )
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in catering and other food service activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRestaurantFoodServicesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRestaurantFoodServicesLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadRestaurantFoodServicesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-mobileFoodServices", "Mobile food services"),
          ("sector-label-restaurantsMobile", "Restaurants")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitRestaurantFoodServicesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (restaurantsMobile4, navigator.nextPage(restaurantsMobile4, "").url),
          (mobileFoodServices, navigator.nextPage(mobileFoodServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRestaurantFoodServicesLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitRestaurantFoodServicesLvl4Page().url
              )
                .withFormUrlEncodedBody("restaurant4" -> value)
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
          controller.submitRestaurantFoodServicesLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitRestaurantFoodServicesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in restaurants and mobile food services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    /* ------------------------- Electricity Views  -------------------------*/
    "loadElectricityLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadElectricityLvl3Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadElectricityLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-electric-power", "Electric power generation, transmission and distribution"),
          ("sector-label-gas-distribution", "Manufacture of gas and distribution of gaseous fuels through mains"),
          ("sector-label-steam-air-conditioning", "Steam and air conditioning supply"),
          (
            "sector-label-brokers-agents-power-gas",
            "Activities of brokers and agents for electric power and natural gas"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitElectricityLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricPowerGenerationAndDistribution, navigator.nextPage(electricPowerGenerationAndDistribution, "").url),
          (gaseousFuelsManufacture, navigator.nextPage(gaseousFuelsManufacture, "").url),
          (steamAndAirConditioningSupply4, navigator.nextPage(steamAndAirConditioningSupply4, "").url),
          (electricPowerAndGasBrokers4, navigator.nextPage(electricPowerAndGasBrokers4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitElectricityLvl3Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitElectricityLvl3Page().url
              )
                .withFormUrlEncodedBody("electricity3" -> value)
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
          controller.submitElectricityLvl3Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitElectricityLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "What is your undertaking’s main business activity in electricity, gas, steam and air conditioning supply?"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadElectricityLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadElectricityLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadElectricityLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-distribution-electricity", "Distribution of electricity"),
          ("sector-label-electricity-nonrenewable", "Production of electricity from non-renewable sources"),
          ("sector-label-electricity-renewable", "Production of electricity from renewable sources"),
          ("sector-label-storage-electricity", "Storage of electricity"),
          ("sector-label-trade-electricity", "Trade of electricity"),
          ("sector-label-transmission-electricity", "Transmission of electricity")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitElectricityLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (electricityDistribution, navigator.nextPage(electricityDistribution, "").url),
          (nonRenewableElectricityProduction, navigator.nextPage(nonRenewableElectricityProduction, "").url),
          (renewableElectricityProduction, navigator.nextPage(renewableElectricityProduction, "").url),
          (electricityStorage, navigator.nextPage(electricityStorage, "").url),
          (electricityTrade, navigator.nextPage(electricityTrade, "").url),
          (electricityTransmission, navigator.nextPage(electricityTransmission, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitElectricityLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitElectricityLvl4Page().url
              )
                .withFormUrlEncodedBody("electricity4" -> value)
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
          controller.submitElectricityLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitElectricityLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in electricity"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadGasManufactureLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGasManufactureLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-distribution-gas", "Distribution of gaseous fuels through mains"),
          ("sector-label-manufacture-gas", "Manufacture of gas"),
          ("sector-label-storage-gas", "Storage of gas as part of network supply services"),
          ("sector-label-trade-gas", "Trade of gas through mains")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitGasManufactureLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (gaseousFuelDistribution, navigator.nextPage(gaseousFuelDistribution, "").url),
          (gaseousFuelManufacture, navigator.nextPage(gaseousFuelManufacture, "").url),
          (gaseousFuelStorage, navigator.nextPage(gaseousFuelStorage, "").url),
          (gaseousFuelTrade, navigator.nextPage(gaseousFuelTrade, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGasManufactureLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitGasManufactureLvl4Page().url
              )
                .withFormUrlEncodedBody("gas4" -> value)
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
          controller.submitGasManufactureLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitGasManufactureLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in manufacture and distribution of gas"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    /* ------------------------- Water Views  -------------------------*/

    "loadWaterLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWaterLvl2Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadWaterLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-sewerage", "Sewerage"),
          ("sector-label-waste-activities", "Waste collection, recovery and disposal"),
          ("sector-label-water-collection-supply", "Water collection, treatment and supply"),
          ("sector-label-remediation", "Remediation activities and other waste management services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWaterLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (sewerage4, navigator.nextPage(sewerage4, "").url),
          (wasteManagement, navigator.nextPage(wasteManagement, "").url),
          (waterTreatment4, navigator.nextPage(waterTreatment4, "").url),
          (wasteManagementRemediation4, navigator.nextPage(wasteManagementRemediation4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWaterLvl2Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitWaterLvl2Page().url
              )
                .withFormUrlEncodedBody("water2" -> value)
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
          controller.submitWaterLvl2Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitWaterLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in water supply, sewerage and waste management"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWasteCollectionRecoveryLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWasteCollectionRecoveryLvl3Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadWasteCollectionRecoveryLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-waste-collection", "Waste collection"),
          ("sector-label-waste-disposal-without-recovery", "Waste disposal without recovery"),
          ("sector-label-waste-recovery", "Waste recovery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWasteCollectionRecoveryLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (wasteCollection, navigator.nextPage(wasteCollection, "").url),
          (wasteDisposal, navigator.nextPage(wasteDisposal, "").url),
          (wasteRecovery, navigator.nextPage(wasteRecovery, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWasteCollectionRecoveryLvl3Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitWasteCollectionRecoveryLvl3Page().url
              )
                .withFormUrlEncodedBody("wasteCollection3" -> value)
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
          controller.submitWasteCollectionRecoveryLvl3Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitWasteCollectionRecoveryLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in waste collection, recovery and disposal"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWasteDisposalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWasteDisposalLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadWasteDisposalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-incineration-without-energy", "Incineration without energy recovery"),
          ("sector-label-landfilling", "Landfilling or permanent storage"),
          ("sector-label-other-waste-disposal", "Other waste disposal")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWasteDisposalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (otherWasteDisposal, navigator.nextPage(otherWasteDisposal, "").url),
          (landFilling, navigator.nextPage(landFilling, "").url),
          (incineration, navigator.nextPage(incineration, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWasteDisposalLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitWasteDisposalLvl4Page().url
              )
                .withFormUrlEncodedBody("wasteDisposal4" -> value)
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
          controller.submitWasteDisposalLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitWasteDisposalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of waste disposal your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWasteCollectionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWasteCollectionLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadWasteCollectionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-hazardous-waste", "Hazardous waste"),
          ("sector-label-non-hazardous-waste", "Non-hazardous waste")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWasteCollectionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (hazardousWaste, navigator.nextPage(hazardousWaste, "").url),
          (nonHazardousWaste, navigator.nextPage(nonHazardousWaste, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWasteCollectionLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitWasteCollectionLvl4Page().url
              )
                .withFormUrlEncodedBody("wasteCollection4" -> value)
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
          controller.submitWasteCollectionLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitWasteCollectionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of waste your undertaking collects"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWasteRecoveryLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWasteRecoveryLvl4Page()(
            FakeRequest(GET, routes.AccomodationUtilitiesController.loadWasteRecoveryLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-energy-recovery", "Energy recovery"),
          ("sector-label-materials-recovery", "Materials recovery"),
          ("sector-label-other-waste-recovery", "Other waste recovery")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWasteRecoveryLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (energyRecovery, navigator.nextPage(energyRecovery, "").url),
          (materialsRecovery, navigator.nextPage(materialsRecovery, "").url),
          (otherWasteRecovery, navigator.nextPage(otherWasteRecovery, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWasteRecoveryLvl4Page()(
              FakeRequest(
                POST,
                routes.AccomodationUtilitiesController.submitWasteRecoveryLvl4Page().url
              )
                .withFormUrlEncodedBody("wasteRecovery4" -> value)
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
          controller.submitWasteRecoveryLvl4Page()(
            FakeRequest(POST, routes.AccomodationUtilitiesController.submitWasteRecoveryLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of waste recovery your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
