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

class RetailWholesaleControllerSpec
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

  private val controller = instanceOf[RetailWholesaleController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val wholesale = "G"
    val retail = "47"
    val nonSpecialisedRetail = "47.1"
    val foodRetail = "47.2"
    val automotiveFuelRetail4 = "47.30"
    val communicationEquipmentRetail4 = "47.40"
    val otherHouseholdEquipmentRetail = "47.5"
    val culturalRetail = "47.6"
    val otherGoodsRetail = "47.7"
    val motorVehiclesAndMotorcyclesRetail = "47.8"
    val retailIntermediationServices = "47.9"
    val booksRetail = "47.61"
    val gamesRetail = "47.64"
    val newspapersRetail = "47.62"
    val sportingEquipmentRetail = "47.63"
    val otherCulturalAndRecreationalGoods = "47.69"
    val beveragesRetail = "47.25"
    val breadRetail = "47.24"
    val fishRetail = "47.23"
    val fruitRetail = "47.21"
    val meatRetail = "47.22"
    val tobaccoRetail = "47.26"
    val otherFoodRetail = "47.27"
    val carpetsRetail = "47.53"
    val electricalRetail = "47.54"
    val hardwareRetail = "47.52"
    val textilesRetail = "47.51"
    val furnitureRetail = "47.55"
    val nonSpecialisedRetailIntermediation = "47.91"
    val specialisedRetailIntermediation = "47.92"
    val carsLightVehicles = "77.11"
    val trucks = "77.12"
    val nonSpecialisedFoodRetail = "47.11"
    val otherNonSpecialisedRetail = "47.12"
    val clothingRetail = "47.71"
    val cosmeticRetail = "47.75"
    val flowersRetail = "47.76"
    val footwearRetail = "47.72"
    val medicalRetail = "47.74"
    val pharmaceuticalRetail = "47.73"
    val watchesRetail = "47.77"
    val otherNewGoodsRetail = "47.78"
    val secondHandGoods = "47.79"
    val wholesaleContractBasis = "46.1"
    val agriculturalWholesale = "46.2"
    val foodWholesale = "46.3"
    val wholesaleHouseholdGoods = "46.4"
    val informationEquipmentWholesale4 = "46.50"
    val machineryWholesale = "46.6"
    val motorVehiclesWholesale = "46.7"
    val otherSpecialisedWholesale = "46.8"
    val nonSpecialisedWholesaleTrade = "46.90"
    val flowersAndPlants = "46.22"
    val grain = "46.21"
    val leatherWholesale = "46.24"
    val liveAnimals = "46.23"
    val agriculturalWholesaleContractBasis = "46.11"
    val foodWholesaleContractBasis = "46.17"
    val fuelsWholesaleContractBasis = "46.12"
    val furnitureWholesaleContractBasis = "46.15"
    val machineryWholesaleContractBasis = "46.14"
    val textilesWholesaleContractBasis = "46.16"
    val timberWholesaleContractBasis = "46.13"
    val otherWholesaleContractBasis = "46.18"
    val nonSpecialisedWholesaleContractBasis = "46.19"
    val beveragesWholesale = "46.34"
    val coffeeAndSpicesWholesale = "46.34"
    val dairyProductsWholesale = "46.33"
    val fruitWholesale = "46.31"
    val meatWholesale = "46.32"
    val sugarWholesale = "46.36"
    val tobaccoProductsWholesale = "46.35"
    val otherFoodWholesale = "46.38"
    val nonSpecialisedFoodWholesale = "46.39"
    val chinaWholesale = "46.44"
    val clothingWholesale = "46.42"
    val electricalHouseholdWholesale = "46.43"
    val householdWholesale = "46.47"
    val perfumeWholesale = "46.45"
    val pharmaceuticalWholesale = "46.46"
    val textilesWholesale = "46.41"
    val watchesWholesale = "46.48"
    val otherHouseholdGoodsWholesale = "46.49"
    val agriculturalMachineryWholesale = "46.61"
    val machineToolsWholesale = "46.62"
    val miningMachineryWholesale = "46.63"
    val otherMachineryWholesale = "46.64"
    val motorVehicleWholesale = "46.71"
    val motorVehiclePartsWholesale = "46.72"
    val motorcyclePartsWholesale = "46.73"
    val chemicalProductsWholesale = "46.85"
    val hardwareEquipment = "46.84"
    val metals = "46.82"
    val fuels = "46.81"
    val waste = "46.87"
    val wood = "46.83"
    val otherIntermediateProducts = "46.86"
    val otherSpecialisedWholesale4 = "46.89"
  }

  import SectorCodes._

  "RetailWholesaleController" should {
    "loadRetailWholesaleLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRetailWholesaleLvl2Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadRetailWholesaleLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-wholesale", "Wholesale"),
          ("sector-label-retail", "Retail")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitRetailWholesaleLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (wholesale, navigator.nextPage(wholesale, "").url),
          (retail, navigator.nextPage(retail, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRetailWholesaleLvl2Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitRetailWholesaleLvl2Page().url)
                .withFormUrlEncodedBody("retail2" -> value)
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
          controller.submitRetailWholesaleLvl2Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitRetailWholesaleLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in retail and wholesale"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadRetailLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRetailLvl3Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadRetailLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-non-specialised-retail", "Non-specialised retail"),
          ("sector-label-food-retail", "Food, beverages and tobacco"),
          ("sector-label-automotive-fuel", "Automotive fuel"),
          ("sector-label-information-communication", "Information and communication equipment"),
          ("sector-label-other-household-equipment", "Other household equipment"),
          ("sector-label-cultural-recreational", "Cultural and recreational goods"),
          ("sector-label-another-type-goods", "Another type of goods"),
          ("sector-label-motor-vehicles-retail", "Motor vehicles, motorcycles and related parts and accessories"),
          ("sector-label-intermediation-services", "My undertaking provides intermediation services for retail sale")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitRetailLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (nonSpecialisedRetail, navigator.nextPage(nonSpecialisedRetail, "").url),
          (foodRetail, navigator.nextPage(foodRetail, "").url),
          (automotiveFuelRetail4, navigator.nextPage(automotiveFuelRetail4, "").url),
          (communicationEquipmentRetail4, navigator.nextPage(communicationEquipmentRetail4, "").url),
          (otherHouseholdEquipmentRetail, navigator.nextPage(otherHouseholdEquipmentRetail, "").url),
          (culturalRetail, navigator.nextPage(culturalRetail, "").url),
          (otherGoodsRetail, navigator.nextPage(otherGoodsRetail, "").url),
          (motorVehiclesAndMotorcyclesRetail, navigator.nextPage(motorVehiclesAndMotorcyclesRetail, "").url),
          (retailIntermediationServices, navigator.nextPage(retailIntermediationServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRetailLvl3Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitRetailLvl3Page().url)
                .withFormUrlEncodedBody("retail3" -> value)
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
          controller.submitRetailLvl3Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitRetailLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of products your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCulturalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCulturalLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadCulturalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-books", "Books"),
          ("sector-label-games", "Games and toys"),
          ("sector-label-newspapers", "Newspapers, other periodical publications and stationery"),
          ("sector-label-sporting", "Sporting equipment"),
          ("sector-label-other-cultural", "Other cultural and recreational goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCulturalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (booksRetail, navigator.nextPage(booksRetail, "").url),
          (gamesRetail, navigator.nextPage(gamesRetail, "").url),
          (newspapersRetail, navigator.nextPage(newspapersRetail, "").url),
          (sportingEquipmentRetail, navigator.nextPage(sportingEquipmentRetail, "").url),
          (otherCulturalAndRecreationalGoods, navigator.nextPage(otherCulturalAndRecreationalGoods, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCulturalLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitCulturalLvl4Page().url)
                .withFormUrlEncodedBody("culturalRetail4" -> value)
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
          controller.submitCulturalLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitCulturalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of cultural or recreational goods your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFoodLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFoodLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadFoodLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-beverages", "Beverages"),
          ("sector-label-bread", "Bread, cake and confectionery"),
          ("sector-label-fish", "Fish, crustaceans and molluscs"),
          ("sector-label-fruit", "Fruit and vegetables"),
          ("sector-label-meat", "Meat and meat products"),
          ("sector-label-tobacco", "Tobacco products"),
          ("sector-label-other-food", "Other food")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFoodLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (beveragesRetail, navigator.nextPage(beveragesRetail, "").url),
          (breadRetail, navigator.nextPage(breadRetail, "").url),
          (fishRetail, navigator.nextPage(fishRetail, "").url),
          (fruitRetail, navigator.nextPage(fruitRetail, "").url),
          (meatRetail, navigator.nextPage(meatRetail, "").url),
          (tobaccoRetail, navigator.nextPage(tobaccoRetail, "").url),
          (otherFoodRetail, navigator.nextPage(otherFoodRetail, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFoodLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitFoodLvl4Page().url)
                .withFormUrlEncodedBody("foodRetail4" -> value)
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
          controller.submitFoodLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitFoodLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of food, beverages or tobacco your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHouseholdLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHouseholdLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadHouseholdLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-carpets", "Carpets, rugs, wall and floor coverings"),
          ("sector-label-electrical", "Electrical household appliances"),
          ("sector-label-hardware", "Hardware, building materials, paints and glass"),
          ("sector-label-textiles", "Textiles"),
          ("sector-label-furniture", "Furniture, lighting equipment, tableware and other household goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHouseholdLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (carpetsRetail, navigator.nextPage(carpetsRetail, "").url),
          (electricalRetail, navigator.nextPage(electricalRetail, "").url),
          (hardwareRetail, navigator.nextPage(hardwareRetail, "").url),
          (textilesRetail, navigator.nextPage(textilesRetail, "").url),
          (furnitureRetail, navigator.nextPage(furnitureRetail, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHouseholdLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitHouseholdLvl4Page().url)
                .withFormUrlEncodedBody("householdRetail4" -> value)
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
          controller.submitHouseholdLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitHouseholdLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other household equipment your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadIntermediationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadIntermediationLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadIntermediationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-non-specialised-intermediation", "Non-specialised retail"),
          ("sector-label-specialised-intermediation", "Specialised retail")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitIntermediationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (nonSpecialisedRetailIntermediation, navigator.nextPage(nonSpecialisedRetailIntermediation, "").url),
          (specialisedRetailIntermediation, navigator.nextPage(specialisedRetailIntermediation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitIntermediationLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitIntermediationLvl4Page().url)
                .withFormUrlEncodedBody("intermediationRetail4" -> value)
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
          controller.submitIntermediationLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitIntermediationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of retail your undertaking provides intermediation services for"
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
            FakeRequest(GET, routes.RetailWholesaleController.loadMotorVehiclesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motor-vehicles", "Motor vehicles"),
          ("sector-label-vehicle-parts", "Motor vehicle parts and accessories"),
          ("sector-label-motorcycles", "Motorcycles, motorcycle parts and accessories")
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
          (motorVehicleWholesale, navigator.nextPage(motorVehicleWholesale, "").url),
          (motorVehiclePartsWholesale, navigator.nextPage(motorVehiclePartsWholesale, "").url),
          (motorcyclePartsWholesale, navigator.nextPage(motorcyclePartsWholesale, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorVehiclesLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitMotorVehiclesLvl4Page().url)
                .withFormUrlEncodedBody("motorVehiclesRetail4" -> value)
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
            FakeRequest(POST, routes.RetailWholesaleController.submitMotorVehiclesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of motor vehicles, motorcycles or parts and accessories your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadNonSpecialisedLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadNonSpecialisedLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadNonSpecialisedLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-non-specialised-food",
            "Non-specialised retail sale of predominately food, beverages or tobacco"
          ),
          ("sector-label-other-non-specialised", "Other non-specialised retail sale")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitNonSpecialisedLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (nonSpecialisedFoodRetail, navigator.nextPage(nonSpecialisedFoodRetail, "").url),
          (otherNonSpecialisedRetail, navigator.nextPage(otherNonSpecialisedRetail, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitNonSpecialisedLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitNonSpecialisedLvl4Page().url)
                .withFormUrlEncodedBody("nonSpecialRetail4" -> value)
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
          controller.submitNonSpecialisedLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitNonSpecialisedLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of non-specialised retail your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherGoodsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherGoodsLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadOtherGoodsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-clothing", "Clothing"),
          ("sector-label-cosmetic", "Cosmetic and toilet articles"),
          ("sector-label-flowers", "Flowers, plants, fertilisers, pets and pet food"),
          ("sector-label-footwear", "Footwear and leather goods"),
          ("sector-label-medical", "Medical and orthopaedic goods"),
          ("sector-label-pharmaceutical", "Pharmaceutical products"),
          ("sector-label-watches", "Watches and jewellery"),
          ("sector-label-other-new-goods", "Other new goods"),
          ("sector-label-second-hand", "Second-hand goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherGoodsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (clothingRetail, navigator.nextPage(clothingRetail, "").url),
          (cosmeticRetail, navigator.nextPage(cosmeticRetail, "").url),
          (flowersRetail, navigator.nextPage(flowersRetail, "").url),
          (footwearRetail, navigator.nextPage(footwearRetail, "").url),
          (medicalRetail, navigator.nextPage(medicalRetail, "").url),
          (pharmaceuticalRetail, navigator.nextPage(pharmaceuticalRetail, "").url),
          (watchesRetail, navigator.nextPage(watchesRetail, "").url),
          (otherNewGoodsRetail, navigator.nextPage(otherNewGoodsRetail, "").url),
          (secondHandGoods, navigator.nextPage(secondHandGoods, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherGoodsLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitOtherGoodsLvl4Page().url)
                .withFormUrlEncodedBody("otherRetail4" -> value)
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
          controller.submitOtherGoodsLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitOtherGoodsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of goods your undertaking retails"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWholesaleLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWholesaleLvl3Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadWholesaleLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-contract-basis", "Wholesale on a fee or contract basis"),
          ("sector-label-agricultural", "Wholesale of agricultural raw materials and live animals"),
          ("sector-label-food", "Wholesale of food, beverages and tobacco"),
          ("sector-label-household", "Wholesale of household goods"),
          ("sector-label-information", "Wholesale of information and communication equipment"),
          ("sector-label-machinery", "Wholesale of other machinery, equipment and supplies"),
          ("sector-label-motor-vehicles", "Wholesale of motor vehicles, motorcycles and parts and accessories"),
          ("sector-label-other-specialised", "Other specialised wholesale"),
          ("sector-label-non-specialised", "Non-specialised wholesale")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWholesaleLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (wholesaleContractBasis, navigator.nextPage(wholesaleContractBasis, "").url),
          (agriculturalWholesale, navigator.nextPage(agriculturalWholesale, "").url),
          (foodWholesale, navigator.nextPage(foodWholesale, "").url),
          (wholesaleHouseholdGoods, navigator.nextPage(wholesaleHouseholdGoods, "").url),
          (informationEquipmentWholesale4, navigator.nextPage(informationEquipmentWholesale4, "").url),
          (machineryWholesale, navigator.nextPage(machineryWholesale, "").url),
          (motorVehiclesWholesale, navigator.nextPage(motorVehiclesWholesale, "").url),
          (otherSpecialisedWholesale, navigator.nextPage(otherSpecialisedWholesale, "").url),
          (nonSpecialisedWholesaleTrade, navigator.nextPage(nonSpecialisedWholesaleTrade, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWholesaleLvl3Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitWholesaleLvl3Page().url)
                .withFormUrlEncodedBody("Wholesale3" -> value)
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
          controller.submitWholesaleLvl3Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitWholesaleLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of wholesale your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAgriculturalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAgriculturalLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadAgriculturalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-flowers", "Flowers and plants"),
          ("sector-label-grain", "Grain, unmanufactured tobacco, seeds and animal feeds"),
          ("sector-label-hides", "Hides, skins and leather"),
          ("sector-label-live-animals", "Live animals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAgriculturalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (flowersAndPlants, navigator.nextPage(flowersAndPlants, "").url),
          (grain, navigator.nextPage(grain, "").url),
          (leatherWholesale, navigator.nextPage(leatherWholesale, "").url),
          (liveAnimals, navigator.nextPage(liveAnimals, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAgriculturalLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitAgriculturalLvl4Page().url)
                .withFormUrlEncodedBody("agriWholesale4" -> value)
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
          controller.submitAgriculturalLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitAgriculturalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of agricultural products your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadContractBasisLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadContractBasisLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadContractBasisLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-agricultural-contract",
            "Agricultural raw materials, live animals, textile raw materials and semi-finished goods"
          ),
          ("sector-label-food-contract", "Food, beverages and tobacco"),
          ("sector-label-fuels-contract", "Fuels, ores, metals and industrial chemicals"),
          ("sector-label-furniture-contract", "Furniture, household goods, hardware and ironmongery"),
          ("sector-label-machinery-contract", "Machinery, industrial equipment, ships and aircraft"),
          ("sector-label-textiles-contract", "Textiles, clothing, fur, footwear and leather goods"),
          ("sector-label-timber-contract", "Timber and building materials"),
          ("sector-label-other-contract", "Other particular products"),
          ("sector-label-non-specialised-contract", "My undertaking carries out non-specialised wholesale")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitContractBasisLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (agriculturalWholesaleContractBasis, navigator.nextPage(agriculturalWholesaleContractBasis, "").url),
          (foodWholesaleContractBasis, navigator.nextPage(foodWholesaleContractBasis, "").url),
          (fuelsWholesaleContractBasis, navigator.nextPage(fuelsWholesaleContractBasis, "").url),
          (furnitureWholesaleContractBasis, navigator.nextPage(furnitureWholesaleContractBasis, "").url),
          (machineryWholesaleContractBasis, navigator.nextPage(machineryWholesaleContractBasis, "").url),
          (textilesWholesaleContractBasis, navigator.nextPage(textilesWholesaleContractBasis, "").url),
          (timberWholesaleContractBasis, navigator.nextPage(timberWholesaleContractBasis, "").url),
          (otherWholesaleContractBasis, navigator.nextPage(otherWholesaleContractBasis, "").url),
          (nonSpecialisedWholesaleContractBasis, navigator.nextPage(nonSpecialisedWholesaleContractBasis, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitContractBasisLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitContractBasisLvl4Page().url)
                .withFormUrlEncodedBody("contractWholesale4" -> value)
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
          controller.submitContractBasisLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitContractBasisLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of products your undertaking wholesales on a fee or contract basis"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFoodWholesaleLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFoodWholesaleLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadFoodWholesaleLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-beverages", "Beverages"),
          ("sector-label-coffee-spices", "Coffee, tea, cocoa and spices"),
          ("sector-label-dairy", "Dairy products, eggs and edible oils and fats"),
          ("sector-label-fruit", "Fruit and vegetables"),
          ("sector-label-meat", "Meat, meat products, fish and fish products"),
          ("sector-label-sugar", "Sugar, chocolate and confectionery"),
          ("sector-label-tobacco", "Tobacco products"),
          ("sector-label-other-food", "Other food"),
          ("sector-label-non-specialised", "Non-specialised wholesale of food, beverages and tobacco")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFoodWholesaleLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (beveragesWholesale, navigator.nextPage(beveragesWholesale, "").url),
          (coffeeAndSpicesWholesale, navigator.nextPage(coffeeAndSpicesWholesale, "").url),
          (dairyProductsWholesale, navigator.nextPage(dairyProductsWholesale, "").url),
          (meatWholesale, navigator.nextPage(meatWholesale, "").url),
          (sugarWholesale, navigator.nextPage(sugarWholesale, "").url),
          (tobaccoProductsWholesale, navigator.nextPage(tobaccoProductsWholesale, "").url),
          (otherFoodWholesale, navigator.nextPage(otherFoodWholesale, "").url),
          (nonSpecialisedFoodWholesale, navigator.nextPage(nonSpecialisedFoodWholesale, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFoodWholesaleLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitFoodWholesaleLvl4Page().url)
                .withFormUrlEncodedBody("foodWholesale4" -> value)
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
          controller.submitFoodWholesaleLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitFoodWholesaleLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of food, beverages or tobacco your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHouseholdWholesaleLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHouseholdWholesaleLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadHouseholdWholesaleLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-china", "China, glassware and cleaning materials"),
          ("sector-label-clothing", "Clothing and footwear"),
          ("sector-label-electrical", "Electrical household appliances"),
          ("sector-label-household", "Household, office and shop furniture, carpets and lighting equipment"),
          ("sector-label-perfume", "Perfume and cosmetics"),
          ("sector-label-pharmaceutical", "Pharmaceutical and medical goods"),
          ("sector-label-textiles", "Textiles"),
          ("sector-label-watches", "Watches and jewellery"),
          ("sector-label-other-household", "Other household goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHouseholdWholesaleLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (chinaWholesale, navigator.nextPage(chinaWholesale, "").url),
          (clothingWholesale, navigator.nextPage(clothingWholesale, "").url),
          (electricalHouseholdWholesale, navigator.nextPage(electricalHouseholdWholesale, "").url),
          (householdWholesale, navigator.nextPage(householdWholesale, "").url),
          (perfumeWholesale, navigator.nextPage(perfumeWholesale, "").url),
          (pharmaceuticalWholesale, navigator.nextPage(pharmaceuticalWholesale, "").url),
          (textilesWholesale, navigator.nextPage(textilesWholesale, "").url),
          (watchesWholesale, navigator.nextPage(watchesWholesale, "").url),
          (otherHouseholdGoodsWholesale, navigator.nextPage(otherHouseholdGoodsWholesale, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHouseholdWholesaleLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitHouseholdWholesaleLvl4Page().url)
                .withFormUrlEncodedBody("householdWholesale4" -> value)
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
          controller.submitHouseholdWholesaleLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitHouseholdWholesaleLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of household goods your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMachineryLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMachineryLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadMachineryLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-agricultural-machinery", "Agricultural machinery, equipment and supplies"),
          ("sector-label-machine-tools", "Machine tools"),
          ("sector-label-mining-machinery", "Mining, construction and civil engineering machinery"),
          ("sector-label-other-machinery", "Other machinery and equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMachineryLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (agriculturalMachineryWholesale, navigator.nextPage(agriculturalMachineryWholesale, "").url),
          (machineToolsWholesale, navigator.nextPage(machineToolsWholesale, "").url),
          (miningMachineryWholesale, navigator.nextPage(miningMachineryWholesale, "").url),
          (otherMachineryWholesale, navigator.nextPage(otherMachineryWholesale, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMachineryLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitMachineryLvl4Page().url)
                .withFormUrlEncodedBody("machineryWholesale4" -> value)
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
          controller.submitMachineryLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitMachineryLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of machinery, equipment or supplies your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMotorVehiclesWholesaleLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMotorVehiclesWholesaleLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadMotorVehiclesWholesaleLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motor-vehicles", "Motor vehicles"),
          ("sector-label-vehicle-parts", "Motor vehicle parts and accessories"),
          ("sector-label-motorcycles", "Motorcycles, motorcycle parts and accessories")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMotorVehiclesWholesaleLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (motorVehicleWholesale, navigator.nextPage(motorVehicleWholesale, "").url),
          (motorVehiclePartsWholesale, navigator.nextPage(motorVehiclePartsWholesale, "").url),
          (motorcyclePartsWholesale, navigator.nextPage(motorcyclePartsWholesale, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorVehiclesWholesaleLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitMotorVehiclesWholesaleLvl4Page().url)
                .withFormUrlEncodedBody("motorVehiclesWholesale4" -> value)
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
          controller.submitMotorVehiclesWholesaleLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitMotorVehiclesWholesaleLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of motor vehicles, motorcycles or parts and accessories your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadSpecialisedLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSpecialisedLvl4Page()(
            FakeRequest(GET, routes.RetailWholesaleController.loadSpecialisedLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-chemical-products", "Chemical products"),
          ("sector-label-hardware-equipment", "Hardware, plumbing and heating equipment and supplies"),
          ("sector-label-metals", "Metals and metal ores"),
          ("sector-label-fuels", "Solid, liquid and gaseous fuels and related products"),
          ("sector-label-waste", "Waste and scrap"),
          ("sector-label-wood", "Wood, construction materials and sanitary equipment"),
          ("sector-label-other-intermediate", "Other intermediate products"),
          ("sector-label-other-specialised", "Other specialised wholesale")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSpecialisedLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (chemicalProductsWholesale, navigator.nextPage(chemicalProductsWholesale, "").url),
          (hardwareEquipment, navigator.nextPage(hardwareEquipment, "").url),
          (metals, navigator.nextPage(metals, "").url),
          (fuels, navigator.nextPage(fuels, "").url),
          (waste, navigator.nextPage(waste, "").url),
          (wood, navigator.nextPage(wood, "").url),
          (otherIntermediateProducts, navigator.nextPage(otherIntermediateProducts, "").url),
          (otherSpecialisedWholesale4, navigator.nextPage(otherSpecialisedWholesale4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSpecialisedLvl4Page()(
              FakeRequest(POST, routes.RetailWholesaleController.submitSpecialisedLvl4Page().url)
                .withFormUrlEncodedBody("specialWholesale4" -> value)
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
          controller.submitSpecialisedLvl4Page()(
            FakeRequest(POST, routes.RetailWholesaleController.submitSpecialisedLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other products your undertaking wholesales"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
