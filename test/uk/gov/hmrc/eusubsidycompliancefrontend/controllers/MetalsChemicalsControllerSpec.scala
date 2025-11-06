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

class MetalsChemicalsControllerSpec
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

  private val controller = instanceOf[MetalsChemicalsController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val pharmaceuticalsManufacture = "21.10"
    val pharmaceuticalPreparationsManufacture = "21.20"
    val nuclearFuelProcessing = "24.46"
    val aluminiumProduction = "24.42"
    val copperProduction = "24.44"
    val leadZincTinProduction = "24.43"
    val preciousMetalsProduction = "24.41"
    val nonFerrousMetalsProduction = "24.45"
    val metalDoors = "25.12"
    val metalStructures = "25.11"
    val centralHeating = "25.21"
    val otherMetalTanks = "25.22"
    val coating = "25.51"
    val heatTreatment = "25.52"
    val machining = "25.53"
    val perfumesToiletPreparations = "20.42"
    val soap = "20.41"
    val dyes = "20.12"
    val fertilisers = "20.15"
    val industrialGases = "20.11"
    val primaryFormsPlastics = "20.16"
    val syntheticRubber = "20.17"
    val otherInorganicChemicals = "20.13"
    val otherOrganicChemicals = "20.14"
    val basicIronSteelManufacture = "24.10"
    val steelTubesFittingsManufacture = "24.20"
    val otherSteelProductsProcessing = "24.3"
    val basicPreciousAndNonFerrousMetals = "24.4"
    val metalsCasting = "24.5"
    val iron = "24.51"
    val lightMetals = "24.53"
    val steel = "24.52"
    val otherMetals = "24.54"
    val basicChemicalProducts = "20.1"
    val manMadeFibreManufacture = "20.60"
    val paintsVarnishesCoatingsManufacture = "20.30"
    val pesticidesDisinfectantsManufacture = "20.20"
    val washingCleaning = "20.4"
    val otherChemicalProducts = "20.5"
    val cokeProductsManufacture = "19.10"
    val fossilFuelProductsManufacture = "19.20"
    val cutlery = "25.61"
    val locks = "25.62"
    val tools = "25.63"
    val metalForging4 = "25.40"
    val cutleryToolsManufacture = "25.6"
    val fabricatedStructuralMetalProducts = "25.1"
    val metalTanksManufacture = "25.2"
    val weaponsManufacture4 = "25.30"
    val otherFabricatedMetalProductsManufacture = "25.9"
    val metalTreatmentCoating = "25.5"
    val barsColdDrawing = "24.31"
    val wireColdDrawing = "24.34"
    val foldingColdDrawing = "24.33"
    val narrowStripColdDrawing = "24.32"
    val fastenerProducts = "25.94"
    val lightMetalPackaging = "25.92"
    val steelDrums = "25.91"
    val wireProducts = "25.93"
    val otherFabricatedMetalProducts = "25.99"
    val liquidBiofuels = "20.51"
    val otherChemicalProducts4 = "20.59"
  }

  import SectorCodes._

  "MetalsChemicalsController" should {
    "loadPharmaceuticalsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPharmaceuticalsLvl3Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadPharmaceuticalsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-basic", "Basic pharmaceutical products"),
          ("sector-label-preparations", "Pharmaceutical preparations")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMiningLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (pharmaceuticalsManufacture, navigator.nextPage(pharmaceuticalsManufacture, "").url),
          (pharmaceuticalPreparationsManufacture, navigator.nextPage(pharmaceuticalPreparationsManufacture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPharmaceuticalsLvl3Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitPharmaceuticalsLvl3Page().url
              )
                .withFormUrlEncodedBody("pharm3" -> value)
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
          controller.submitPharmaceuticalsLvl3Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitPharmaceuticalsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of pharmaceuticals your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPreciousNonFerrousLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPreciousNonFerrousLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadPreciousNonFerrousLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-nuclear-fuel", "Processing of nuclear fuel"),
          ("sector-label-aluminium", "Production of aluminium"),
          ("sector-label-copper", "Production of copper"),
          ("sector-label-lead-zinc-tin", "Production of lead, zinc and tin"),
          ("sector-label-precious", "Production of precious metals"),
          ("sector-label-non-ferrous", "Production of other non-ferrous metals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPreciousNonFerrousLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (nuclearFuelProcessing, navigator.nextPage(nuclearFuelProcessing, "").url),
          (aluminiumProduction, navigator.nextPage(aluminiumProduction, "").url),
          (copperProduction, navigator.nextPage(copperProduction, "").url),
          (leadZincTinProduction, navigator.nextPage(leadZincTinProduction, "").url),
          (preciousMetalsProduction, navigator.nextPage(preciousMetalsProduction, "").url),
          (nonFerrousMetalsProduction, navigator.nextPage(nonFerrousMetalsProduction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPreciousNonFerrousLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitPreciousNonFerrousLvl4Page().url
              )
                .withFormUrlEncodedBody("preciousNonIron4" -> value)
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
          controller.submitPreciousNonFerrousLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitPreciousNonFerrousLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metal production or processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadStructuralMetalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadStructuralMetalLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadStructuralMetalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-doors-windows", "Doors and windows"),
          ("sector-label-cutlery", "Metal structures and parts of structures")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitStructuralMetalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (metalDoors, navigator.nextPage(metalDoors, "").url),
          (metalStructures, navigator.nextPage(metalStructures, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitStructuralMetalLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitStructuralMetalLvl4Page().url
              )
                .withFormUrlEncodedBody("structuralMetal4" -> value)
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
          controller.submitStructuralMetalLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitStructuralMetalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of structural metal products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTanksReservoirsContainersLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTanksReservoirsContainersLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadTanksReservoirsContainersLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-doors-windows", "Central heating radiators, steam generators and boilers"),
          ("sector-label-other", "Other tanks, reservoirs and containers of metal")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTanksReservoirsContainersLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (centralHeating, navigator.nextPage(centralHeating, "").url),
          (otherMetalTanks, navigator.nextPage(otherMetalTanks, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTanksReservoirsContainersLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitTanksReservoirsContainersLvl4Page().url
              )
                .withFormUrlEncodedBody("tanks4" -> value)
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
          controller.submitTanksReservoirsContainersLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitTanksReservoirsContainersLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metal products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTreatmentCoatingMachiningLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTreatmentCoatingMachiningLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadTreatmentCoatingMachiningLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-coating", "Coating"),
          ("sector-label-heat-treatment", "Heat treatment"),
          ("sector-label-machining", "Machining")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTreatmentCoatingMachiningLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (coating, navigator.nextPage(coating, "").url),
          (heatTreatment, navigator.nextPage(heatTreatment, "").url),
          (machining, navigator.nextPage(machining, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTreatmentCoatingMachiningLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitTreatmentCoatingMachiningLvl4Page().url
              )
                .withFormUrlEncodedBody("treatment4" -> value)
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
          controller.submitTreatmentCoatingMachiningLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitTreatmentCoatingMachiningLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metal finishing or processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWashingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWashingLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadWashingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-perfume", "Perfumes and toilet preparations"),
          ("sector-label-detergents", "Soap and detergents, cleaning and polishing preparations")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWashingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (perfumesToiletPreparations, navigator.nextPage(perfumesToiletPreparations, "").url),
          (soap, navigator.nextPage(soap, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWashingLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitWashingLvl4Page().url
              )
                .withFormUrlEncodedBody("washing4" -> value)
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
          controller.submitWashingLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitWashingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of washing, cleaning and polishing preparations your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadBasicLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBasicLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadBasicLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-dyes", "Dyes and pigments"),
          ("sector-label-fertilisers", "Fertilisers and nitrogen compounds"),
          ("sector-label-industrial-gases", "Industrial gases"),
          ("sector-label-plastics", "Plastics in primary forms"),
          ("sector-label-rubber", "Synthetic rubber in primary forms"),
          ("sector-label-inorganic", "Other inorganic basic chemicals"),
          ("sector-label-organic", "Other organic basic chemicals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBasicLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (dyes, navigator.nextPage(dyes, "").url),
          (fertilisers, navigator.nextPage(fertilisers, "").url),
          (industrialGases, navigator.nextPage(industrialGases, "").url),
          (primaryFormsPlastics, navigator.nextPage(primaryFormsPlastics, "").url),
          (syntheticRubber, navigator.nextPage(syntheticRubber, "").url),
          (otherInorganicChemicals, navigator.nextPage(otherInorganicChemicals, "").url),
          (otherOrganicChemicals, navigator.nextPage(otherOrganicChemicals, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBasicLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitBasicLvl4Page().url
              )
                .withFormUrlEncodedBody("basicChem4" -> value)
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
          controller.submitBasicLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitBasicLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of basic chemicals or polymer-based materials your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadBasicMetalsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBasicMetalsLvl3Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadBasicMetalsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-basic-iron", "Basic iron, steel and ferro-alloys"),
          ("sector-label-tubes", "Steel tubes, pipes, hollow profiles and related fittings"),
          ("sector-label-other", "Other products of first processing of steel"),
          ("sector-label-precious", "Basic precious and other non-ferrous metals"),
          ("sector-label-casting", "My undertaking carries out casting of metals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitBasicMetalsLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (basicIronSteelManufacture, navigator.nextPage(basicIronSteelManufacture, "").url),
          (steelTubesFittingsManufacture, navigator.nextPage(steelTubesFittingsManufacture, "").url),
          (otherSteelProductsProcessing, navigator.nextPage(otherSteelProductsProcessing, "").url),
          (basicPreciousAndNonFerrousMetals, navigator.nextPage(basicPreciousAndNonFerrousMetals, "").url),
          (metalsCasting, navigator.nextPage(metalsCasting, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBasicMetalsLvl3Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitBasicMetalsLvl3Page().url
              )
                .withFormUrlEncodedBody("basicMetals3" -> value)
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
          controller.submitBasicMetalsLvl3Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitBasicMetalsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of basic metals your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCastingMetalsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCastingMetalsLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadCastingMetalsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-iron", "Iron"),
          ("sector-label-light-metals", "Light metals"),
          ("sector-label-steel", "Steel"),
          ("sector-label-non-ferrous", "Other non-ferrous metals")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCastingMetalsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (iron, navigator.nextPage(iron, "").url),
          (lightMetals, navigator.nextPage(lightMetals, "").url),
          (steel, navigator.nextPage(steel, "").url),
          (otherMetals, navigator.nextPage(otherMetals, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCastingMetalsLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitCastingMetalsLvl4Page().url
              )
                .withFormUrlEncodedBody("castingMetals4" -> value)
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
          controller.submitCastingMetalsLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitCastingMetalsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metals your undertaking casts"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadChemicalsProductsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadChemicalsProductsLvl3Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadChemicalsProductsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-basic", "Basic chemicals, fertilisers and nitrogen compounds, plastics and synthetic rubber in primary forms"),
          ("sector-label-manmade-fibre", "Manmade fibres"),
          ("sector-label-paints", "Paints, varnishes and similar coatings, printing ink and mastics"),
          ("sector-label-pesticides", "Pesticides, disinfectants and other agrochemical products"),
          ("sector-label-washing", "Washing, cleaning and polishing preparations"),
          ("sector-label-petroleum", "Other chemical products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitChemicalsProductsLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (basicChemicalProducts, navigator.nextPage(basicChemicalProducts, "").url),
          (manMadeFibreManufacture, navigator.nextPage(manMadeFibreManufacture, "").url),
          (paintsVarnishesCoatingsManufacture, navigator.nextPage(paintsVarnishesCoatingsManufacture, "").url),
          (pesticidesDisinfectantsManufacture, navigator.nextPage(pesticidesDisinfectantsManufacture, "").url),
          (washingCleaning, navigator.nextPage(washingCleaning, "").url),
          (otherChemicalProducts, navigator.nextPage(otherChemicalProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitChemicalsProductsLvl3Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitChemicalsProductsLvl3Page().url
              )
                .withFormUrlEncodedBody("chemProds3" -> value)
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
          controller.submitChemicalsProductsLvl3Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitChemicalsProductsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of chemical or chemical product your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCokePetroleumLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCokePetroleumLvl3Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadCokePetroleumLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-coke", "Coke oven products"),
          ("sector-label-petroleum", "Refined petroleum and fossil fuel products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCokePetroleumLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cokeProductsManufacture, navigator.nextPage(cokeProductsManufacture, "").url),
          (fossilFuelProductsManufacture, navigator.nextPage(fossilFuelProductsManufacture, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCokePetroleumLvl3Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitCokePetroleumLvl3Page().url
              )
                .withFormUrlEncodedBody("coke3" -> value)
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
          controller.submitCokePetroleumLvl3Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitCokePetroleumLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fuel products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadCutleryToolsHardwareLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadCutleryToolsHardwareLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadCutleryToolsHardwareLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-cutlery", "Cutlery"),
          ("sector-label-locks", "Locks and hinges"),
          ("sector-label-Tools", "Tools")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitCutleryToolsHardwareLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cutlery, navigator.nextPage(cutlery, "").url),
          (locks, navigator.nextPage(locks, "").url),
          (tools, navigator.nextPage(tools, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitCutleryToolsHardwareLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitCutleryToolsHardwareLvl4Page().url
              )
                .withFormUrlEncodedBody("cutlery4" -> value)
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
          controller.submitCutleryToolsHardwareLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitCutleryToolsHardwareLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of cutlery or hardware your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFabricatedMetalsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFabricatedMetalsLvl3Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadFabricatedMetalsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-forging", "Forging and shaping metal and powder metallurgy"),
          ("sector-label-cutlery", "Manufacture of cutlery, tools and general hardware"),
          ("sector-label-structural", "Manufacture of structural metal products"),
          ("sector-label-tanks", "Manufacture of tanks, reservoirs and containers of metal"),
          ("sector-label-weapons", "Manufacture of weapons and ammunition"),
          ("sector-label-other", "Manufacture of other fabricated metal products"),
          ("sector-label-coating", "Treatment and coating of metals; machining")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFabricatedMetalsLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (metalForging4, navigator.nextPage(metalForging4, "").url),
          (cutleryToolsManufacture, navigator.nextPage(cutleryToolsManufacture, "").url),
          (fabricatedStructuralMetalProducts, navigator.nextPage(fabricatedStructuralMetalProducts, "").url),
          (metalTanksManufacture, navigator.nextPage(metalTanksManufacture, "").url),
          (weaponsManufacture4, navigator.nextPage(weaponsManufacture4, "").url),
          (otherFabricatedMetalProductsManufacture, navigator.nextPage(otherFabricatedMetalProductsManufacture, "").url),
          (metalTreatmentCoating, navigator.nextPage(metalTreatmentCoating, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFabricatedMetalsLvl3Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitFabricatedMetalsLvl3Page().url
              )
                .withFormUrlEncodedBody("fabMetal3" -> value)
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
          controller.submitFabricatedMetalsLvl3Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitFabricatedMetalsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fabricated metal manufacturing or processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFirstProcessingSteelLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFirstProcessingSteelLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadFirstProcessingSteelLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-bars", "Cold drawing of bars"),
          ("sector-label-wire", "Cold drawing of wire"),
          ("sector-label-forming", "Cold forming or folding"),
          ("sector-label-narrow-strip", "Cold rolling of narrow strip")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFirstProcessingSteelLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (barsColdDrawing, navigator.nextPage(barsColdDrawing, "").url),
          (wireColdDrawing, navigator.nextPage(wireColdDrawing, "").url),
          (foldingColdDrawing, navigator.nextPage(foldingColdDrawing, "").url),
          (narrowStripColdDrawing, navigator.nextPage(narrowStripColdDrawing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFirstProcessingSteelLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitFirstProcessingSteelLvl4Page().url
              )
                .withFormUrlEncodedBody("steel4" -> value)
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
          controller.submitFirstProcessingSteelLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitFirstProcessingSteelLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of steel processing your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherFabricatedProductsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherFabricatedProductsLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadOtherFabricatedProductsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-fasteners", "Fasteners and screw machine products"),
          ("sector-label-light-packaging", "Light metal packaging"),
          ("sector-label-steel-drums", "Steel drums and similar containers"),
          ("sector-label-wire-springs", "Wire products, chain and springs"),
          ("sector-label-other", "Another type of fabricated metal product")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherFabricatedProductsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fastenerProducts, navigator.nextPage(fastenerProducts, "").url),
          (lightMetalPackaging, navigator.nextPage(lightMetalPackaging, "").url),
          (steelDrums, navigator.nextPage(steelDrums, "").url),
          (wireProducts, navigator.nextPage(wireProducts, "").url),
          (otherFabricatedMetalProducts, navigator.nextPage(otherFabricatedMetalProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherFabricatedProductsLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitOtherFabricatedProductsLvl4Page().url
              )
                .withFormUrlEncodedBody("otherFab4" -> value)
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
          controller.submitOtherFabricatedProductsLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitOtherFabricatedProductsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of fabricated metal products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherProductsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherProductsLvl4Page()(
            FakeRequest(GET, routes.MetalsChemicalsController.loadOtherProductsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-biofuels", "Liquid biofuels"),
          ("sector-label-detergents", "Another type of chemical product")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherProductsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (liquidBiofuels, navigator.nextPage(liquidBiofuels, "").url),
          (otherChemicalProducts4, navigator.nextPage(otherChemicalProducts4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherProductsLvl4Page()(
              FakeRequest(
                POST,
                routes.MetalsChemicalsController.submitOtherProductsLvl4Page().url
              )
                .withFormUrlEncodedBody("otherProducts4" -> value)
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
          controller.submitOtherProductsLvl4Page()(
            FakeRequest(POST, routes.MetalsChemicalsController.submitOtherProductsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other chemical products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
