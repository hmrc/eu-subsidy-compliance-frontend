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

class NonMetallicOtherControllerSpec
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

  private val controller = instanceOf[NonMetallicOtherController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val cementPlasterArticles = "23.6"
    val cementLimePlaster = "23.5"
    val clayBuildingMaterials = "23.3"
    val stoneCuttingFinishing4 = "23.70"
    val glassProducts = "23.1"
    val refractoryProductsManufacture = "23.20"
    val otherCeramicProducts = "23.4"
    val otherAbrasiveProducts = "23.9"
    val gamesAndToysManufacture = "32.40"
    val jewelleryAndCoins = "32.1"
    val medicalInstrumentsManufacture = "32.50"
    val musicalInstrumentsManufacture = "32.20"
    val sportsGoodsManufacture = "32.30"
    val otherProducts = "32.9"
    val abrasiveProducts = "23.91"
    val otherNonMetallicMineral = "23.99"
    val cement = "23.51"
    val plasterLime = "23.52"
    val bricks = "23.32"
    val ceramicTiles = "23.31"
    val concreteProducts = "23.61"
    val fibreCement = "23.65"
    val mortars = "23.64"
    val plasterProducts = "23.62"
    val readyMixedConcrete = "23.63"
    val otherConcreteProducts = "23.66"


  }

  import SectorCodes._

  "NonMetallicOtherController" should {
    "loadNonMetallicMineralLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadNonMetallicMineralLvl3Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadNonMetallicMineralLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cement-plaster", "Articles of concrete, cement and plaster"),
          ("sector-label-cement-lime", "Cement, lime and plaster"),
          ("sector-label-clay", "Clay building materials"),
          ("sector-label-stone", "Cutting, shaping and finishing of stone"),
          ("sector-label-glass", "Glass and glass products"),
          ("sector-label-refractory", "Refractory products"),
          ("sector-label-porcelain", "Other porcelain and ceramic products"),
          ("sector-label-abrasive", "Another type of abrasive or non-metallic mineral product.")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitNonMetallicMineralLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cementPlasterArticles, navigator.nextPage(cementPlasterArticles, "").url),
          (cementLimePlaster, navigator.nextPage(cementLimePlaster, "").url),
          (clayBuildingMaterials, navigator.nextPage(clayBuildingMaterials, "").url),
          (stoneCuttingFinishing4, navigator.nextPage(stoneCuttingFinishing4, "").url),
          (glassProducts, navigator.nextPage(glassProducts, "").url),
          (refractoryProductsManufacture, navigator.nextPage(refractoryProductsManufacture, "").url),
          (otherCeramicProducts, navigator.nextPage(otherCeramicProducts, "").url),
          (otherAbrasiveProducts, navigator.nextPage(otherAbrasiveProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitNonMetallicMineralLvl3Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitNonMetallicMineralLvl3Page().url
              )
                .withFormUrlEncodedBody("mineral3" -> value)
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
          controller.submitNonMetallicMineralLvl3Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitNonMetallicMineralLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of non-metallic mineral products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherManufacturingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherManufacturingLvl3Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadOtherManufacturingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-other-toys", "Games and toys"),
          ("sector-label-jewellery", "Jewellery, coins and related articles"),
          ("sector-label-medical", "Medical and dental instruments and supplies"),
          ("sector-label-music", "Musical instruments"),
          ("sector-label-sports", "Sports goods"),
          ("sector-label-other", "Other products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherManufacturingLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (gamesAndToysManufacture, navigator.nextPage(gamesAndToysManufacture, "").url),
          (jewelleryAndCoins, navigator.nextPage(jewelleryAndCoins, "").url),
          (medicalInstrumentsManufacture, navigator.nextPage(medicalInstrumentsManufacture, "").url),
          (musicalInstrumentsManufacture, navigator.nextPage(musicalInstrumentsManufacture, "").url),
          (sportsGoodsManufacture, navigator.nextPage(sportsGoodsManufacture, "").url),
          (otherProducts, navigator.nextPage(otherProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherManufacturingLvl3Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitOtherManufacturingLvl3Page().url
              )
                .withFormUrlEncodedBody("otherManufacturing" -> value)
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
          controller.submitOtherManufacturingLvl3Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitOtherManufacturingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAnotherTypeLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAnotherTypeLvl4Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadAnotherTypeLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-abrasive", "Abrasive products"),
          ("sector-label-other", "Other non-metallic mineral products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAnotherTypeLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (abrasiveProducts, navigator.nextPage(abrasiveProducts, "").url),
          (otherNonMetallicMineral, navigator.nextPage(otherNonMetallicMineral, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAnotherTypeLvl4Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitAnotherTypeLvl4Page().url
              )
                .withFormUrlEncodedBody("otherMineral4" -> value)
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
          controller.submitAnotherTypeLvl4Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitAnotherTypeLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of abrasive or non-metallic mineral products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCementLimePlasterLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCementLimePlasterLvl4Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadCementLimePlasterLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cement", "Cement"),
          ("sector-label-limeplaster", "Lime and plaster")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCementLimePlasterLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cement, navigator.nextPage(cement, "").url),
          (plasterLime, navigator.nextPage(plasterLime, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCementLimePlasterLvl4Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitCementLimePlasterLvl4Page().url
              )
                .withFormUrlEncodedBody("cement4" -> value)
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
          controller.submitCementLimePlasterLvl4Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitCementLimePlasterLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of materials your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadClayBuildingMaterialsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadClayBuildingMaterialsLvl4Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadClayBuildingMaterialsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-bricks-clay", "Bricks, tiles and construction products in baked clay"),
          ("sector-label-ceramic-tiles", "Ceramic tiles and flags")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitClayBuildingMaterialsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (bricks, navigator.nextPage(bricks, "").url),
          (ceramicTiles, navigator.nextPage(ceramicTiles, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitClayBuildingMaterialsLvl4Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitClayBuildingMaterialsLvl4Page().url
              )
                .withFormUrlEncodedBody("clay4" -> value)
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
          controller.submitClayBuildingMaterialsLvl4Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitClayBuildingMaterialsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of clay building materials your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadConcreteCementPlasterLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadConcreteCementPlasterLvl4Page()(
            FakeRequest(GET, routes.NonMetallicOtherController.loadConcreteCementPlasterLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-concrete", "Concrete products for construction purposes"),
          ("sector-label-fibre-cement", "Fibre cement"),
          ("sector-label-mortars", "Mortars"),
          ("sector-label-plaster-products", "Plaster products for construction purposes"),
          ("sector-label-readymix", "Ready-mixed concrete"),
          ("sector-label-other", "Other articles of concrete, cement and plaster")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitConcreteCementPlasterLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (concreteProducts, navigator.nextPage(concreteProducts, "").url),
          (fibreCement, navigator.nextPage(fibreCement, "").url),
          (mortars, navigator.nextPage(mortars, "").url),
          (plasterProducts, navigator.nextPage(plasterProducts, "").url),
          (readyMixedConcrete, navigator.nextPage(readyMixedConcrete, "").url),
          (otherConcreteProducts, navigator.nextPage(otherConcreteProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitConcreteCementPlasterLvl4Page()(
              FakeRequest(
                POST,
                routes.NonMetallicOtherController.submitConcreteCementPlasterLvl4Page().url
              )
                .withFormUrlEncodedBody("concrete4" -> value)
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
          controller.submitConcreteCementPlasterLvl4Page()(
            FakeRequest(POST, routes.NonMetallicOtherController.submitConcreteCementPlasterLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of concrete, cement or plaster articles your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }



  }
}
