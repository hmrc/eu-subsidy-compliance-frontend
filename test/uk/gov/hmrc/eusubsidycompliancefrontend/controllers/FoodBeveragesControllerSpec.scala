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

class FoodBeveragesControllerSpec
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

  private val controller = instanceOf[FoodBeveragesController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val bakeryProducts = "10.7"
    val dairyProducts = "10.5"
    val fishProcessing = "10.20"
    val fruitAndVegetables = "10.3"
    val grainAndStarchProducts = "10.6"
    val meat = "10.1"
    val oils = "10.4"
    val preparedAnimalFeeds = "10.9"
    val otherFoodProducts = "10.8"
    val farmAnimalsFood = "10.91"
    val petFood = "10.92"
    val dairyProducts4 = "10.51"
    val iceCream = "10.52"
    val fruitAndVegetableJuiceManufacture = "10.32"
    val fruitAndVegetableProcessing = "10.31"
    val otherFruitAndVegetableProcessing = "10.39"
    val grainProducts4 = "10.61"
    val starchProducts = "10.62"
    val meatProcessing = "10.11"
    val poultryProcessing = "10.12"
    val meatProductsProduction = "10.13"
    val margarine = "10.42"
    val otherOils = "10.41"
    val confectionery = "10.82"
    val condiments = "10.84"
    val homogenisedFoodPreparations = "10.86"
    val preparedMeals = "10.85"
    val sugar = "10.81"
    val teaAndCoffee = "10.83"
    val otherFoodProduct = "10.89"
    val beer = "11.05"
    val ciders = "11.03"
    val malt = "11.06"
    val softDrinks = "11.07"
    val spirits = "11.01"
    val wine = "11.02"
    val otherFermentedBeverages = "11.04"
  }

  import SectorCodes._

  "FoodBeveragesController" should {
    "loadFoodLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFoodLvl3Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadFoodLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-bakery", "Bakery and farinaceous products"),
          ("sector-label-dairy", "Dairy products and ice"),
          ("sector-label-fish", "Fish, crustaceans and molluscs"),
          ("sector-label-fruit-veg", "Fruit and vegetables"),
          ("sector-label-grain", "Grain mill products and starch"),
          ("sector-label-meat", "Meat"),
          ("sector-label-oils", "Oils and fats"),
          ("sector-label-animalfeeds", "Prepared animal feeds"),
          ("sector-label-other", "Other food products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFoodLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (bakeryProducts, navigator.nextPage(bakeryProducts, "").url),
          (dairyProducts, navigator.nextPage(dairyProducts, "").url),
          (fishProcessing, navigator.nextPage(fishProcessing, "").url),
          (fruitAndVegetables, navigator.nextPage(fruitAndVegetables, "").url),
          (grainAndStarchProducts, navigator.nextPage(grainAndStarchProducts, "").url),
          (meat, navigator.nextPage(meat, "").url),
          (oils, navigator.nextPage(oils, "").url),
          (preparedAnimalFeeds, navigator.nextPage(preparedAnimalFeeds, "").url),
          (otherFoodProducts, navigator.nextPage(otherFoodProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFoodLvl3Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitFoodLvl3Page().url
              )
                .withFormUrlEncodedBody("food3" -> value)
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
          controller.submitFoodLvl3Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitFoodLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of food your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAnimalFeedsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAnimalFeedsLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadAnimalFeedsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-farm-feed", "Food for farm animals"),
          ("sector-label-pet-feed", "Pet food")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAnimalFeedsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (farmAnimalsFood, navigator.nextPage(farmAnimalsFood, "").url),
          (petFood, navigator.nextPage(petFood, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAnimalFeedsLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitAnimalFeedsLvl4Page().url
              )
                .withFormUrlEncodedBody("animalFood4" -> value)
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
          controller.submitAnimalFeedsLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitAnimalFeedsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of prepared animal feeds your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadBakeryAndFarinaceousLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBakeryAndFarinaceousLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadBakeryAndFarinaceousLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-rusks", "Biscuits, rusks and preserved pastries and cakes"),
          ("sector-label-bread-pastry", "Bread, cakes and fresh pastry goods"),
          ("sector-label-farinaceous", "Farinaceous products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBakeryAndFarinaceousLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (farmAnimalsFood, navigator.nextPage(farmAnimalsFood, "").url),
          (farmAnimalsFood, navigator.nextPage(farmAnimalsFood, "").url),
          (petFood, navigator.nextPage(petFood, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBakeryAndFarinaceousLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitBakeryAndFarinaceousLvl4Page().url
              )
                .withFormUrlEncodedBody("bakery4" -> value)
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
          controller.submitBakeryAndFarinaceousLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitBakeryAndFarinaceousLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of bakery and farinaceous products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadDairyProductsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadDairyProductsLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadDairyProductsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-dairy", "Dairy products"),
          ("sector-label-icecream", "Ice cream and other edible ice")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitDairyProductsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (dairyProducts4, navigator.nextPage(dairyProducts4, "").url),
          (iceCream, navigator.nextPage(iceCream, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitDairyProductsLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitDairyProductsLvl4Page().url
              )
                .withFormUrlEncodedBody("dairyFood4" -> value)
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
          controller.submitDairyProductsLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitDairyProductsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of dairy products or ice your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFruitAndVegLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFruitAndVegLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadFruitAndVegLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-fruitveg-juice", "Manufacture of fruit and vegetable juice"),
          ("sector-label-fruitveg-potatoes", "Processing and preserving of potatoes"),
          ("sector-label-fruitveg-other", "Other processing and preserving of fruit and vegetables")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFruitAndVegLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fruitAndVegetableJuiceManufacture, navigator.nextPage(fruitAndVegetableJuiceManufacture, "").url),
          (fruitAndVegetableProcessing, navigator.nextPage(fruitAndVegetableProcessing, "").url),
          (otherFruitAndVegetableProcessing, navigator.nextPage(otherFruitAndVegetableProcessing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFruitAndVegLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitFruitAndVegLvl4Page().url
              )
                .withFormUrlEncodedBody("fruit4" -> value)
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
          controller.submitFruitAndVegLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitFruitAndVegLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fruit and vegetable production or processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadGrainAndStarchLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGrainAndStarchLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadGrainAndStarchLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-grain", "Grain mill products"),
          ("sector-label-starch", "Starches and starch products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitGrainAndStarchLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (grainProducts4, navigator.nextPage(grainProducts4, "").url),
          (starchProducts, navigator.nextPage(starchProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGrainAndStarchLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitGrainAndStarchLvl4Page().url
              )
                .withFormUrlEncodedBody("grain4" -> value)
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
          controller.submitGrainAndStarchLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitGrainAndStarchLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of grain mill products or starch your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMeatLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMeatLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadMeatLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-meat-processing", "Processing and preserving of meat (except poultry)"),
          ("sector-label-poultry-processing", "Processing and preserving of poultry meat"),
          ("sector-label-meat-products", "Production of meat and poultry meat products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMeatLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (meatProcessing, navigator.nextPage(meatProcessing, "").url),
          (poultryProcessing, navigator.nextPage(poultryProcessing, "").url),
          (meatProductsProduction, navigator.nextPage(meatProductsProduction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMeatLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitMeatLvl4Page().url
              )
                .withFormUrlEncodedBody("meat4" -> value)
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
          controller.submitMeatLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitMeatLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of meat production or processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOilsAndFatsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOilsAndFatsLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadOilsAndFatsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-oilsfats-marg", "Margarine and similar edible fats"),
          ("sector-label-oilsfats-other", "Other oils and fats")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOilsAndFatsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (margarine, navigator.nextPage(margarine, "").url),
          (otherOils, navigator.nextPage(otherOils, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOilsAndFatsLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitOilsAndFatsLvl4Page().url
              )
                .withFormUrlEncodedBody("oils4" -> value)
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
          controller.submitOilsAndFatsLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitOilsAndFatsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of oils and fats your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherFoodProductsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherFoodProductsLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadOtherFoodProductsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-confectionary", "Cocoa, chocolate and confectionery"),
          ("sector-label-condiments", "Condiments and seasonings"),
          ("sector-label-homogenised", "Homogenised food preparations and dietetic food"),
          ("sector-label-prepared-meals", "Prepared meals"),
          ("sector-label-sugar", "Sugar"),
          ("sector-label-tea-coffee", "Tea and coffee"),
          ("sector-label-other", "Another type of food product")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherFoodProductsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (confectionery, navigator.nextPage(confectionery, "").url),
          (condiments, navigator.nextPage(condiments, "").url),
          (homogenisedFoodPreparations, navigator.nextPage(homogenisedFoodPreparations, "").url),
          (preparedMeals, navigator.nextPage(preparedMeals, "").url),
          (sugar, navigator.nextPage(sugar, "").url),
          (teaAndCoffee, navigator.nextPage(teaAndCoffee, "").url),
          (otherFoodProduct, navigator.nextPage(otherFoodProduct, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherFoodProductsLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitOtherFoodProductsLvl4Page().url
              )
                .withFormUrlEncodedBody("otherFood4" -> value)
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
          controller.submitOtherFoodProductsLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitOtherFoodProductsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other food products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadBeveragesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBeveragesLvl4Page()(
            FakeRequest(GET, routes.FoodBeveragesController.loadBeveragesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-beer", "Beer"),
          ("sector-label-cider", "Cider and other fruit fermented beverages"),
          ("sector-label-malt", "Malt"),
          ("sector-label-softdrinks", "Soft drinks and bottled waters"),
          ("sector-label-spirits", "Spirits (distilled, rectified or blended)"),
          ("sector-label-wine", "Wine from grapes"),
          ("sector-label-other-fermented", "Other non-distilled fermented beverages")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBeveragesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (beer, navigator.nextPage(beer, "").url),
          (ciders, navigator.nextPage(ciders, "").url),
          (malt, navigator.nextPage(malt, "").url),
          (softDrinks, navigator.nextPage(softDrinks, "").url),
          (spirits, navigator.nextPage(spirits, "").url),
          (wine, navigator.nextPage(wine, "").url),
          (otherFermentedBeverages, navigator.nextPage(otherFermentedBeverages, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBeveragesLvl4Page()(
              FakeRequest(
                POST,
                routes.FoodBeveragesController.submitBeveragesLvl4Page().url
              )
                .withFormUrlEncodedBody("beverages4" -> value)
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
          controller.submitBeveragesLvl4Page()(
            FakeRequest(POST, routes.FoodBeveragesController.submitBeveragesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of beverages your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
