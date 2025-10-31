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

class AgricultureControllerSpec
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

  private val controller = instanceOf[AgricultureController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val cropAnimalProduction = "01"
    val growingNonPerennialCrops = "01.1"
    val cerealsLeguminousCrops = "01.11"
    val fibreCrops = "01.16"
    val rice = "01.12"
    val sugarCane = "01.14"
    val tobacco = "01.15"
    val vegetables = "01.13"
    val otherNonPerennialCrops = "01.19"
    val growingPerennialCrops = "01.2"
    val beverageCrops = "01.27"
    val citrusFruits = "01.23"
    val grapes = "01.21"
    val oleaginousFruits = "01.26"
    val stoneFruits = "01.24"
    val spicesPharmaceuticalCrops = "01.28"
    val tropicalFruits = "01.22"
    val otherTree = "01.25"
    val otherPerennialCrops = "01.29"
    val plantPropagation = "01.3"
    val plantPropagation4 = "01.30"
    val animalProduction = "01.4"
    val dairyCattle = "01.41"
    val otherCattle = "01.42"
    val camels = "01.44"
    val horses = "01.43"
    val poultry = "01.47"
    val sheep = "01.45"
    val swine = "01.46"
    val otherAnimals = "01.48"
    val mixedFarming = "01.5"
    val mixedFarming4 = "01.50"
    val supportActivities = "01.6"
    val postHarvestActivities = "01.63"
    val supportActivitiesAnimal = "01.62"
    val supportActivitiesCrop = "01.61"
    val huntingTrapping = "01.7"
    val huntingTrapping4 = "01.70"

    val fishing = "03.1"
    val freshwaterFishing = "03.12"
    val marineFishing = "03.11"
    val aquaculture3 = "03.2"
    val freshwaterAquaculture = "03.22"
    val marineAquaculture = "03.21"
    val aquacultureSupportActivities = "03.3"
    val aquacultureSupportActivities4 = "03.30"

    val silviculture = "02.1"
    val silviculture4 = "02.10"
    val logging = "02.2"
    val logging4 = "02.20"
    val gatheringOfWildGrowth = "02.3"
    val gatheringOfWildGrowth4 = "02.30"
    val forestrySupportServices = "02.4"
    val forestrySupportServices4 = "02.40"
  }

  import SectorCodes._

  "AgricultureController" should {
    /* ------------------------- Agriculture Views  -------------------------*/
    "loadAgricultureLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAgricultureLvl3Page()(
            FakeRequest(GET, routes.AgricultureController.loadAgricultureLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-animalProduction", "Animal production"),
          ("sector-label-growingNonPerennialCrops", "Growing non-perennial crops"),
          ("sector-label-growingPerennialCrops", "Growing perennial crops"),
          ("sector-label-huntingTrapping", "Hunting, trapping and related service activities"),
          ("sector-label-mixedFarming", "Mixed farming"),
          ("sector-label-plantPropagation", "Plant propagation"),
          ("sector-label-supportActivities", "Support activities to agriculture and post-harvest crop activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAgricultureLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (growingNonPerennialCrops, navigator.nextPage(growingNonPerennialCrops, "").url),
          (growingPerennialCrops, navigator.nextPage(growingPerennialCrops, "").url),
          (plantPropagation, navigator.nextPage(plantPropagation, "").url),
          (animalProduction, navigator.nextPage(animalProduction, "").url),
          (mixedFarming, navigator.nextPage(mixedFarming, "").url),
          (supportActivities, navigator.nextPage(supportActivities, "").url),
          (huntingTrapping, navigator.nextPage(huntingTrapping, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAgricultureLvl3Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitAgricultureLvl3Page().url
              )
                .withFormUrlEncodedBody("agriculture3" -> value)
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
          controller.submitAgricultureLvl3Page()(
            FakeRequest(POST, routes.AgricultureController.submitAgricultureLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in agriculture"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadNonPerennialCropLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadNonPerennialCropLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadNonPerennialCropLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cerealsLeguminousCrops", "Cereals (except rice), leguminous crops and oil seeds"),
          ("sector-label-fibreCrops", "Fibre crops"),
          ("sector-label-rice", "Rice"),
          ("sector-label-sugarCane", "Sugar cane"),
          ("sector-label-tobacco", "Tobacco"),
          ("sector-label-vegetables", "Vegetables and melons, roots and tubers"),
          ("sector-label-otherNonPerennialCrops", "Other non-perennial crops")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitNonPerennialCropLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cerealsLeguminousCrops, navigator.nextPage(cerealsLeguminousCrops, "").url),
          (fibreCrops, navigator.nextPage(fibreCrops, "").url),
          (rice, navigator.nextPage(rice, "").url),
          (sugarCane, navigator.nextPage(sugarCane, "").url),
          (tobacco, navigator.nextPage(tobacco, "").url),
          (vegetables, navigator.nextPage(vegetables, "").url),
          (otherNonPerennialCrops, navigator.nextPage(otherNonPerennialCrops, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitNonPerennialCropLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitNonPerennialCropLvl4Page().url
              )
                .withFormUrlEncodedBody("nonPCrops4" -> value)
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
          controller.submitNonPerennialCropLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitNonPerennialCropLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in agriculture"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadPerennialCropLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPerennialCropLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadPerennialCropLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-beverageCrops", "Beverage crops"),
          ("sector-label-citrusFruits", "Citrus fruits"),
          ("sector-label-grapes", "Grapes"),
          ("sector-label-oleaginousFruits", "Oleaginous fruits"),
          ("sector-label-stoneFruits", "Pome fruits and stone fruits"),
          ("sector-label-spicesPharmaceuticalCrops", "Spices, aromatic, drug and pharmaceutical crops"),
          ("sector-label-tropicalFruits", "Tropical and subtropical fruits"),
          ("sector-label-otherTree", "Other tree and bush fruits and nuts"),
          ("sector-label-otherPerennialCrops", "Other perennial crops")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitPerennialCropLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (beverageCrops, navigator.nextPage(beverageCrops, "").url),
          (citrusFruits, navigator.nextPage(citrusFruits, "").url),
          (grapes, navigator.nextPage(grapes, "").url),
          (oleaginousFruits, navigator.nextPage(oleaginousFruits, "").url),
          (stoneFruits, navigator.nextPage(stoneFruits, "").url),
          (spicesPharmaceuticalCrops, navigator.nextPage(spicesPharmaceuticalCrops, "").url),
          (tropicalFruits, navigator.nextPage(tropicalFruits, "").url),
          (otherTree, navigator.nextPage(otherTree, "").url),
          (otherPerennialCrops, navigator.nextPage(otherPerennialCrops, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPerennialCropLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitPerennialCropLvl4Page().url
              )
                .withFormUrlEncodedBody("pCrops4" -> value)
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
          controller.submitPerennialCropLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitPerennialCropLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of crop your undertaking grows"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadAnimalProductionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAnimalProductionLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadAnimalProductionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-dairyCattle", "Dairy cattle"),
          ("sector-label-otherCattle", "Other cattle and buffaloes"),
          ("sector-label-camels", "Camels and camelids"),
          ("sector-label-horses", "Horses and other equines"),
          ("sector-label-poultry", "Poultry"),
          ("sector-label-sheep", "Sheep and goats"),
          ("sector-label-swine", "Swine and pigs"),
          ("sector-label-otherAnimals", "Other animals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAnimalProductionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (dairyCattle, navigator.nextPage(dairyCattle, "").url),
          (otherCattle, navigator.nextPage(otherCattle, "").url),
          (camels, navigator.nextPage(camels, "").url),
          (horses, navigator.nextPage(horses, "").url),
          (poultry, navigator.nextPage(poultry, "").url),
          (sheep, navigator.nextPage(sheep, "").url),
          (swine, navigator.nextPage(swine, "").url),
          (otherAnimals, navigator.nextPage(otherAnimals, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAnimalProductionLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitAnimalProductionLvl4Page().url
              )
                .withFormUrlEncodedBody("animal4" -> value)
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
          controller.submitAnimalProductionLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitAnimalProductionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of animal your undertaking raises"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadSupportActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSupportActivitiesLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadSupportActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-postHarvestActivities", "Post-harvest crop activities and seed processing for propagation"),
          ("sector-label-supportActivitiesAnimal", "Support activities for animal production"),
          ("sector-label-supportActivitiesCrop", "Support activities for crop production")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitSupportActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (postHarvestActivities, navigator.nextPage(postHarvestActivities, "").url),
          (supportActivitiesAnimal, navigator.nextPage(supportActivitiesAnimal, "").url),
          (supportActivitiesCrop, navigator.nextPage(supportActivitiesCrop, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSupportActivitiesLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitSupportActivitiesLvl4Page().url
              )
                .withFormUrlEncodedBody("agriSupport4" -> value)
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
          controller.submitSupportActivitiesLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitSupportActivitiesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of support activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    /* ------------------------- Fishing Views  -------------------------*/
    "loadFishingAndAquacultureLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFishingAndAquacultureLvl3Page()(
            FakeRequest(GET, routes.AgricultureController.loadFishingAndAquacultureLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-aquaculture3", "Fishing"),
          ("sector-label-fishing", "Aquaculture"),
          ("sector-label-aquacultureSupportActivities", "Support activities for fishing and aquaculture")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitFishingAndAquacultureLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fishing, navigator.nextPage(fishing, "").url),
          (aquaculture3, navigator.nextPage(aquaculture3, "").url),
          (aquacultureSupportActivities, navigator.nextPage(aquacultureSupportActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFishingAndAquacultureLvl3Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitFishingAndAquacultureLvl3Page().url
              )
                .withFormUrlEncodedBody("fishing3" -> value)
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
          controller.submitFishingAndAquacultureLvl3Page()(
            FakeRequest(POST, routes.AgricultureController.submitFishingAndAquacultureLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in fishery and aquaculture"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadAquacultureLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAquacultureLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadAquacultureLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-freshwaterAquaculture", "Freshwater aquaculture"),
          ("sector-label-marineAquaculture", "Marine aquaculture")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAquacultureLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (freshwaterAquaculture, navigator.nextPage(freshwaterAquaculture, "").url),
          (marineAquaculture, navigator.nextPage(marineAquaculture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAquacultureLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitAquacultureLvl4Page().url
              )
                .withFormUrlEncodedBody("aquaculture4" -> value)
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
          controller.submitAquacultureLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitAquacultureLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in aquaculture"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadFishingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFishingLvl4Page()(
            FakeRequest(GET, routes.AgricultureController.loadFishingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-freshwaterFishing", "Freshwater"),
          ("sector-label-marineFishing", "Marine")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitFishingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (freshwaterFishing, navigator.nextPage(freshwaterFishing, "").url),
          (marineFishing, navigator.nextPage(marineFishing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFishingLvl4Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitFishingLvl4Page().url
              )
                .withFormUrlEncodedBody("fishing4" -> value)
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
          controller.submitFishingLvl4Page()(
            FakeRequest(POST, routes.AgricultureController.submitFishingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fishing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    /* ------------------------- Forestry Views  -------------------------*/
    "loadForestryLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadForestryLvl3Page()(
            FakeRequest(GET, routes.AgricultureController.loadForestryLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-silviculture", "Silviculture and other forestry activities"),
          ("sector-label-logging", "Logging"),
          ("sector-label-gatheringOfWildGrowth", "Gathering of wild growing non-wood products"),
          ("sector-label-forestrySupportServices", "Support services to forestry")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitForestryLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (silviculture4, navigator.nextPage(silviculture4, "").url),
          (logging4, navigator.nextPage(logging4, "").url),
          (gatheringOfWildGrowth4, navigator.nextPage(gatheringOfWildGrowth4, "").url),
          (forestrySupportServices4, navigator.nextPage(forestrySupportServices4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitForestryLvl3Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitForestryLvl3Page().url
              )
                .withFormUrlEncodedBody("forestry3" -> value)
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
          controller.submitForestryLvl3Page()(
            FakeRequest(POST, routes.AgricultureController.submitForestryLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in forestry"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
