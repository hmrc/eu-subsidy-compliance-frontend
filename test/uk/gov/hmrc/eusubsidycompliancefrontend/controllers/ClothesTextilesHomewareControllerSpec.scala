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

class ClothesTextilesHomewareControllerSpec
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

  private val controller = instanceOf[ClothesTextilesHomewareController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val knittedClothingManufacture = "14.10"
    val otherClothing = "14.2"
    val footwearManufacture4 = "15.20"
    val leatherManufacture = "15.1"
    val textilesFinishing = "13.3"
    val textilesManufacture = "13.9"
    val textilesPreparation = "13.1"
    val textilesWeaving = "13.2"
    val woodProductsManufacture = "16.2"
    val woodProductsProcessing = "16.1"
    val carpets = "13.93"
    val cordage = "13.94"
    val householdTextiles = "13.92"
    val knittedFabrics = "13.91"
    val nonWoven = "13.95"
    val otherTechnicalTextiles = "13.96"
    val otherTextile = "13.99"
    val leatherAndFurClothing = "14.24"
    val outerwear = "14.21"
    val underwear = "14.22"
    val workwear = "14.23"
    val otherClothing4 = "14.29"
    val buildersWare = "22.24"
    val doorsAndWindows = "22.23"
    val packingGoods = "22.22"
    val plasticPlatesSheetsTubes = "22.21"
    val otherPlasticProduct = "22.26"
    val finishingPlasticProducts = "22.25"
    val rubberTubes = "22.11"
    val otherRubberProducts = "22.12"
    val processingAndFinishing = "16.12"
    val sawmillingAndPlaning = "16.11"
    val luggageManufacture = "15.12"
    val leatherTanning = "15.11"
    val assembledParquetFloors = "16.22"
    val solidFuelsVegetableBiomass = "16.26"
    val veneerSheetsPanels = "16.21"
    val woodenContainers = "16.24"
    val woodenWindowsDoors = "16.25"
    val otherBuildersCarpentryJoinery = "16.23"
    val otherWoodProducts = "16.28"
    val woodenProductsFinishing = "16.27"
  }

  import SectorCodes._

  "ClothesTextilesHomewareController" should {
    "loadClothingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadClothingLvl3Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadClothingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-knitted-crocheted", "Knitted and crocheted clothing"),
          ("sector-label-other", "Other clothing and accessories")
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
          (knittedClothingManufacture, navigator.nextPage(knittedClothingManufacture, "").url),
          (otherClothing, navigator.nextPage(otherClothing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitClothingLvl3Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitClothingLvl3Page().url)
                .withFormUrlEncodedBody("clothing3" -> value)
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
          controller.submitClothingLvl3Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitClothingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of clothing your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadLeatherLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadLeatherLvl3Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadLeatherLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-footwear", "Manufacture of footwear"),
          (
            "sector-label-tanning",
            "Tanning, dyeing, dressing of leather and fur; manufacture of luggage, handbags, saddlery and harnesses"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitLeatherLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (footwearManufacture4, navigator.nextPage(footwearManufacture4, "").url),
          (leatherManufacture, navigator.nextPage(leatherManufacture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitLeatherLvl3Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitLeatherLvl3Page().url)
                .withFormUrlEncodedBody("leather3" -> value)
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
          controller.submitLeatherLvl3Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitLeatherLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertakings main business activity in leather or related products"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRubberPlasticLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRubberPlasticLvl3Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadRubberPlasticLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-plastic", "Plastic products"),
          (
            "sector-label-rubber",
            "Rubber products"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitRubberPlasticLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (footwearManufacture4, navigator.nextPage(footwearManufacture4, "").url),
          (leatherManufacture, navigator.nextPage(leatherManufacture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRubberPlasticLvl3Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitRubberPlasticLvl3Page().url)
                .withFormUrlEncodedBody("rubber3" -> value)
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
          controller.submitRubberPlasticLvl3Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitRubberPlasticLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of rubber or plastic products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadTextilesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTextilesLvl3Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadTextilesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-finishing", "Finishing of textiles"),
          (
            "sector-label-other",
            "Manufacture of textiles (except clothes)"
          ),
          ("sector-label-fibre-spinning", "Preparation and spinning of textile fibres"),
          ("sector-label-weaving", "Weaving of textiles")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitTextilesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (textilesFinishing, navigator.nextPage(textilesFinishing, "").url),
          (textilesManufacture, navigator.nextPage(textilesManufacture, "").url),
          (textilesPreparation, navigator.nextPage(textilesPreparation, "").url),
          (textilesWeaving, navigator.nextPage(textilesWeaving, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTextilesLvl3Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitTextilesLvl3Page().url)
                .withFormUrlEncodedBody("textiles3" -> value)
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
          controller.submitTextilesLvl3Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitTextilesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of textile manufacturing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWoodCorkStrawLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWoodCorkStrawLvl3Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadWoodCorkStrawLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-wood-cork-straw", "Manufacture of products of wood, cork, straw and plaiting materials"),
          (
            "sector-label-dairy",
            "Sawmilling, planing, processing or finishing of wood"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWoodCorkStrawLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (woodProductsManufacture, navigator.nextPage(woodProductsManufacture, "").url),
          (woodProductsProcessing, navigator.nextPage(woodProductsProcessing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWoodCorkStrawLvl3Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitWoodCorkStrawLvl3Page().url)
                .withFormUrlEncodedBody("straw3" -> value)
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
          controller.submitWoodCorkStrawLvl3Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitWoodCorkStrawLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertakings main business activity in wood, cork or straw products"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadManufactureOfTextilesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadManufactureOfTextilesLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadManufactureOfTextilesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-carpets-rugs", "Carpets and rugs"),
          (
            "sector-label-corgage",
            "Cordage, rope, twine and netting"
          ),
          ("sector-label-household", "Household textiles and made-up furnishing articles"),
          ("sector-label-knitted", "Knitted and crocheted fabrics"),
          ("sector-label-nonwoven", "Non-wovens and non-woven articles"),
          ("sector-label-industrial", "Other technical and industrial textiles"),
          ("sector-label-other", "Another type of textile")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitManufactureOfTextilesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (carpets, navigator.nextPage(carpets, "").url),
          (cordage, navigator.nextPage(cordage, "").url),
          (householdTextiles, navigator.nextPage(householdTextiles, "").url),
          (knittedFabrics, navigator.nextPage(knittedFabrics, "").url),
          (nonWoven, navigator.nextPage(nonWoven, "").url),
          (otherTechnicalTextiles, navigator.nextPage(otherTechnicalTextiles, "").url),
          (otherTextile, navigator.nextPage(otherTextile, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitManufactureOfTextilesLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitManufactureOfTextilesLvl4Page().url)
                .withFormUrlEncodedBody("textiles4" -> value)
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
          controller.submitManufactureOfTextilesLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitManufactureOfTextilesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of textiles your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadOtherClothingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherClothingLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadOtherClothingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-leather-fur", "Leather and fur clothing"),
          ("sector-label-outerwear", "Outerwear"),
          ("sector-label-underwear", "Underwear"),
          ("sector-label-workwear", "Workwear"),
          ("sector-label-other", "Other clothing and accessories")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitOtherClothingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (leatherAndFurClothing, navigator.nextPage(leatherAndFurClothing, "").url),
          (outerwear, navigator.nextPage(outerwear, "").url),
          (underwear, navigator.nextPage(underwear, "").url),
          (workwear, navigator.nextPage(workwear, "").url),
          (otherClothing4, navigator.nextPage(otherClothing4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherClothingLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitOtherClothingLvl4Page().url)
                .withFormUrlEncodedBody("otherClothing4" -> value)
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
          controller.submitOtherClothingLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitOtherClothingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of clothing or accessories your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadPlasticLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPlasticLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadPlasticLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-builders-ware", "Builders’ ware"),
          ("sector-label-doors-windows", "Doors and windows"),
          ("sector-label-packing", "Packing goods"),
          ("sector-label-plates-sheets", "Plates, sheets, tubes and profiles"),
          ("sector-label-other-plastic", "Another type of plastic product"),
          ("sector-label-finishing", "My undertaking carries out processing and finishing of plastic products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitPlasticLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (buildersWare, navigator.nextPage(buildersWare, "").url),
          (doorsAndWindows, navigator.nextPage(doorsAndWindows, "").url),
          (packingGoods, navigator.nextPage(packingGoods, "").url),
          (plasticPlatesSheetsTubes, navigator.nextPage(plasticPlatesSheetsTubes, "").url),
          (otherPlasticProduct, navigator.nextPage(otherPlasticProduct, "").url),
          (finishingPlasticProducts, navigator.nextPage(finishingPlasticProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPlasticLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitPlasticLvl4Page().url)
                .withFormUrlEncodedBody("plastic4" -> value)
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
          controller.submitPlasticLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitPlasticLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of plastic products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadRubberLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRubberLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadRubberLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-tyres-tubes", "Rubber tyres and tubes"),
          ("sector-label-other-rubber", "Another type of rubber products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitRubberLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (rubberTubes, navigator.nextPage(rubberTubes, "").url),
          (otherRubberProducts, navigator.nextPage(otherRubberProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRubberLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitRubberLvl4Page().url)
                .withFormUrlEncodedBody("rubber4" -> value)
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
          controller.submitRubberLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitRubberLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of rubber products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadSawmillingWoodworkLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSawmillingWoodworkLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadSawmillingWoodworkLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-finishing", "Processing and finishing"),
          ("sector-label-sawmilling", "Sawmilling and planing")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitSawmillingWoodworkLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (processingAndFinishing, navigator.nextPage(processingAndFinishing, "").url),
          (sawmillingAndPlaning, navigator.nextPage(sawmillingAndPlaning, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSawmillingWoodworkLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitSawmillingWoodworkLvl4Page().url)
                .withFormUrlEncodedBody("sawmilling4" -> value)
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
          controller.submitSawmillingWoodworkLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitSawmillingWoodworkLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select how your undertaking manufactures or processes wood"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadTanningDressingDyeingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTanningDressingDyeingLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadTanningDressingDyeingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-luggage", "Manufacture of luggage, handbags, saddlery and harnesses of any material"),
          ("sector-label-leather-fur", "Tanning, dressing, dyeing of leather and fur")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitTanningDressingDyeingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (luggageManufacture, navigator.nextPage(luggageManufacture, "").url),
          (leatherTanning, navigator.nextPage(leatherTanning, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTanningDressingDyeingLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitTanningDressingDyeingLvl4Page().url)
                .withFormUrlEncodedBody("tanning4" -> value)
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
          controller.submitTanningDressingDyeingLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitTanningDressingDyeingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of manufacturing or related activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

    "loadWoodCorkStrawPlaitingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWoodCorkStrawPlaitingLvl4Page()(
            FakeRequest(GET, routes.ClothesTextilesHomewareController.loadWoodCorkStrawPlaitingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-parquet", "Assembled parquet floors"),
          ("sector-label-solid-fuels", "Solid fuels from vegetable biomass"),
          ("sector-label-veneer-sheets", "Veneer sheets and wood-based panels"),
          ("sector-label-wooden-containers", "Wooden containers"),
          ("sector-label-doors-windows", "Wooden windows and doors"),
          ("sector-label-builders", "Other builders carpentry and joinery"),
          ("sector-label-other-wood", "Other products of wood, cork, straw and plaiting materials"),
          ("sector-label-finishing", "My undertaking carries out finishing of wooden products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitWoodCorkStrawPlaitingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (assembledParquetFloors, navigator.nextPage(assembledParquetFloors, "").url),
          (solidFuelsVegetableBiomass, navigator.nextPage(solidFuelsVegetableBiomass, "").url),
          (veneerSheetsPanels, navigator.nextPage(veneerSheetsPanels, "").url),
          (woodenContainers, navigator.nextPage(woodenContainers, "").url),
          (woodenWindowsDoors, navigator.nextPage(woodenWindowsDoors, "").url),
          (otherBuildersCarpentryJoinery, navigator.nextPage(otherBuildersCarpentryJoinery, "").url),
          (otherWoodProducts, navigator.nextPage(otherWoodProducts, "").url),
          (woodenProductsFinishing, navigator.nextPage(woodenProductsFinishing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWoodCorkStrawPlaitingLvl4Page()(
              FakeRequest(POST, routes.ClothesTextilesHomewareController.submitWoodCorkStrawPlaitingLvl4Page().url)
                .withFormUrlEncodedBody("strawPlaiting4" -> value)
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
          controller.submitWoodCorkStrawPlaitingLvl4Page()(
            FakeRequest(POST, routes.ClothesTextilesHomewareController.submitWoodCorkStrawPlaitingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of wood, cork, straw or plaited product your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
