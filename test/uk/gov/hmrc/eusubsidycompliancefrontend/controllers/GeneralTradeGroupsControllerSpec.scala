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

class GeneralTradeGroupsControllerSpec
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

  private val controller = instanceOf[GeneralTradeGroupsController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val wholesaleAndRetailTrade = "G"
    val manufacturing = "C"
    val professionalScientificAndTechnicalActivities = "N"
    val construction = "F"
    val telecommunications = "K"
    val publishingBroadcasting = "J"
    val administration = "O"
    val transportStorage = "H"
    val forestry = "02"
    val realEstate = "M"
    val otherGeneralTrade = "INT00"
    val financialInsuranceActivities = "L"
    val waterSupply = "E"
    val electricityGas = "D"
    val miningQuarrying = "B"
    val accommodationAndFoodService = "I"
    val artsSportsRecreation = "S"
    val education2 = "85"
    val humanHealthSocialWork = "R"
    val publicAdministrationSocialSecurity = "84"
    val otherService = "T"
    val households = "U"
    val extraterritorialOrganisationsActivities4 = "99.00"
    val manuGroup5 = "INT05"
    val manuGroup4 = "INT04"
    val manuGroup3 = "INT03"
    val manuGroup2 = "INT02"
    val manuGroup6 = "INT06"
    val manuGroup7 = "INT07"
    val otherManufacturing = "32"
    val metalProductsRepairMaintenance = "33"
    val furnitureManufacture4 = "31.00"
    val clothing = "14"
    val textiles = "13"
    val leatherProducts = "15"
    val computersProducts = "26"
    val electricalEquipment = "27"
    val otherMachineryAndEquipment = "28"
    val food = "10"
    val beverages = "11"
    val tobaccoProductsManufacture4 = "12.00"
    val fabricatedMetalProducts = "25"
    val woodProducts = "16"
    val rubberPlasticProducts = "22"
    val chemicalProducts = "20"
    val basicMetals = "24"
    val cokeProducts = "19"
    val otherNonMetallicProducts = "23"
    val paperRelated = "17"
    val printedProducts = "18"
    val motorVehicles = "29"
    val otherTransportEquipment = "30"
  }

  import SectorCodes._

  "GeneralTradeGroupsController" should {
    "loadGeneralTradeUndertakingPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGeneralTradeUndertakingPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-wholesaleRetail", "Wholesale and retail"),
          ("sector-label-manufacturing", "Manufacturing"),
          ("sector-label-professionalScientificTechnicalActivities", "Professional, scientific and technical services"),
          ("sector-label-construction", "Construction"),
          ("sector-label-telecommunications", "Telecommunications and computing"),
          ("sector-label-publishingBroadcasting", "Publishing and broadcasting"),
          ("sector-label-administration", "Administration and support services"),
          ("sector-label-transportStorage", "Transport and storage"),
          ("sector-label-forestry", "Forestry"),
          ("sector-label-realEstate", "Real estate"),
          ("sector-label-other", "Other")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitGeneralTradeUndertakingPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (wholesaleAndRetailTrade, navigator.nextPage(wholesaleAndRetailTrade, "").url),
          (manufacturing, navigator.nextPage(manufacturing, "").url),
          (
            professionalScientificAndTechnicalActivities,
            navigator.nextPage(professionalScientificAndTechnicalActivities, "").url
          ),
          (construction, navigator.nextPage(construction, "").url),
          (telecommunications, navigator.nextPage(telecommunications, "").url),
          (publishingBroadcasting, navigator.nextPage(publishingBroadcasting, "").url),
          (administration, navigator.nextPage(administration, "").url),
          (transportStorage, navigator.nextPage(transportStorage, "").url),
          (forestry, navigator.nextPage(forestry, "").url),
          (realEstate, navigator.nextPage(realEstate, "").url),
          (otherGeneralTrade, navigator.nextPage(otherGeneralTrade, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGeneralTradeUndertakingPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitGeneralTradeUndertakingPage().url
              )
                .withFormUrlEncodedBody("gt" -> value)
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
          controller.submitGeneralTradeUndertakingPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitGeneralTradeUndertakingPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadGeneralTradeUndertakingOtherPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadGeneralTradeUndertakingOtherPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-financialInsuranceActivities", "Financial services and insurance"),
          ("sector-label-waterSupply", "Water supply, sewerage and waste management"),
          ("sector-label-electricityGas", "Electricity, gas, steam and air conditioning supply"),
          ("sector-label-mining", "Mining and quarrying"),
          ("sector-label-accommodationAndFoodService", "Accommodation and food services"),
          ("sector-label-artsSportsRecreation", "Arts, sports and recreation"),
          ("sector-label-education", "Education"),
          ("sector-label-humanHealthSocialWork", "Human health and social work"),
          ("sector-label-publicAdministration", "Public administration, defence and compulsory social security"),
          ("sector-label-otherService", "Other service activities"),
          ("sector-label-households", "Households employing workers or producing goods and services for their own use"),
          ("sector-label-activitiesExtraterritorial", "Activities of extraterritorial organisations and bodies")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitGeneralTradeUndertakingOtherPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (financialInsuranceActivities, navigator.nextPage(financialInsuranceActivities, "").url),
          (waterSupply, navigator.nextPage(waterSupply, "").url),
          (electricityGas, navigator.nextPage(electricityGas, "").url),
          (miningQuarrying, navigator.nextPage(miningQuarrying, "").url),
          (accommodationAndFoodService, navigator.nextPage(accommodationAndFoodService, "").url),
          (artsSportsRecreation, navigator.nextPage(artsSportsRecreation, "").url),
          (education2, navigator.nextPage(education2, "").url),
          (humanHealthSocialWork, navigator.nextPage(humanHealthSocialWork, "").url),
          (publicAdministrationSocialSecurity, navigator.nextPage(publicAdministrationSocialSecurity, "").url),
          (otherService, navigator.nextPage(otherService, "").url),
          (households, navigator.nextPage(households, "").url),
          (
            extraterritorialOrganisationsActivities4,
            navigator.nextPage(extraterritorialOrganisationsActivities4, "").url
          )
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitGeneralTradeUndertakingOtherPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitGeneralTradeUndertakingOtherPage().url
              )
                .withFormUrlEncodedBody("gt-other" -> value)
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
          controller.submitGeneralTradeUndertakingOtherPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitGeneralTradeUndertakingOtherPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the option that best describes your undertaking’s main business activity"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLvl2_1GroupsPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadLvl2_1GroupsPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-metals", "Metals, chemicals and materials"),
          ("sector-label-food", "Food, beverages and tobacco"),
          ("sector-label-computers", "Computers, electronics and machinery"),
          ("sector-label-clothes", "Clothes, textiles and homeware"),
          ("sector-label-paper", "Paper and printed products"),
          ("sector-label-vehicles", "Vehicles and transport"),
          ("sector-label-pharmaceuticals", "Pharmaceuticals"),
          ("sector-label-other", "Other manufacturing"),
          ("sector-label-machinery", "My undertaking repairs, maintains or installs machinery and equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLvl2_1GroupsPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (manuGroup5, navigator.nextPage(manuGroup5, "").url),
          (manuGroup4, navigator.nextPage(manuGroup4, "").url),
          (manuGroup3, navigator.nextPage(manuGroup3, "").url),
          (manuGroup2, navigator.nextPage(manuGroup2, "").url),
          (manuGroup6, navigator.nextPage(manuGroup6, "").url),
          (manuGroup7, navigator.nextPage(manuGroup7, "").url),
          (otherManufacturing, navigator.nextPage(otherManufacturing, "").url),
          (metalProductsRepairMaintenance, navigator.nextPage(metalProductsRepairMaintenance, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitLvl2_1GroupsPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitLvl2_1GroupsPage().url
              )
                .withFormUrlEncodedBody("manu-g1" -> value)
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
          controller.submitLvl2_1GroupsPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitLvl2_1GroupsPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of product your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadClothesTextilesHomewarePage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadClothesTextilesHomewarePage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-furniture", "Furniture"),
          ("sector-label-clothing", "Clothing"),
          ("sector-label-textiles", "Textiles"),
          ("sector-label-leather", "Leather and related products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitClothesTextilesHomewarePage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (furnitureManufacture4, navigator.nextPage(furnitureManufacture4, "").url),
          (clothing, navigator.nextPage(clothing, "").url),
          (textiles, navigator.nextPage(textiles, "").url),
          (leatherProducts, navigator.nextPage(leatherProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitClothesTextilesHomewarePage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitClothesTextilesHomewarePage().url
              )
                .withFormUrlEncodedBody("manu-g2" -> value)
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
          controller.submitClothesTextilesHomewarePage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitClothesTextilesHomewarePage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of clothes, textiles or homeware your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadComputersElectronicsMachineryPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadComputersElectronicsMachineryPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-computers", "Computers, electronics and optical products"),
          ("sector-label-electrical", "Electrical equipment"),
          ("sector-label-other", "Other machinery and equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitComputersElectronicsMachineryPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (computersProducts, navigator.nextPage(computersProducts, "").url),
          (electricalEquipment, navigator.nextPage(electricalEquipment, "").url),
          (otherMachineryAndEquipment, navigator.nextPage(otherMachineryAndEquipment, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitComputersElectronicsMachineryPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitComputersElectronicsMachineryPage().url
              )
                .withFormUrlEncodedBody("manu-g3" -> value)
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
          controller.submitComputersElectronicsMachineryPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitComputersElectronicsMachineryPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of computers, electronics or machinery your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFoodBeveragesTobaccoPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFoodBeveragesTobaccoPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-food", "Food"),
          ("sector-label-beverages", "Beverages"),
          ("sector-label-tobacco", "Tobacco products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFoodBeveragesTobaccoPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (food, navigator.nextPage(food, "").url),
          (beverages, navigator.nextPage(beverages, "").url),
          (tobaccoProductsManufacture4, navigator.nextPage(tobaccoProductsManufacture4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFoodBeveragesTobaccoPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitFoodBeveragesTobaccoPage().url
              )
                .withFormUrlEncodedBody("manu-g4" -> value)
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
          controller.submitFoodBeveragesTobaccoPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitFoodBeveragesTobaccoPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of food, beverages or tobacco your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMetalsChemicalsMaterialsPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMetalsChemicalsMaterialsPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-fabricated", "Fabricated metal products (except machinery and equipment)"),
          ("sector-label-wood", "Wood, cork and straw products (except furniture)"),
          ("sector-label-rubber", "Rubber and plastic products"),
          ("sector-label-chemicals", "Chemicals and chemical products"),
          ("sector-label-basic", "Basic metals"),
          ("sector-label-coke", "Coke and refined petroleum products"),
          ("sector-label-nonmetallic", "Other non-metallic mineral products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMetalsChemicalsMaterialsPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (fabricatedMetalProducts, navigator.nextPage(fabricatedMetalProducts, "").url),
          (woodProducts, navigator.nextPage(woodProducts, "").url),
          (rubberPlasticProducts, navigator.nextPage(rubberPlasticProducts, "").url),
          (chemicalProducts, navigator.nextPage(chemicalProducts, "").url),
          (basicMetals, navigator.nextPage(basicMetals, "").url),
          (cokeProducts, navigator.nextPage(cokeProducts, "").url),
          (otherNonMetallicProducts, navigator.nextPage(otherNonMetallicProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMetalsChemicalsMaterialsPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitMetalsChemicalsMaterialsPage().url
              )
                .withFormUrlEncodedBody("manu-g5" -> value)
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
          controller.submitMetalsChemicalsMaterialsPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitMetalsChemicalsMaterialsPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of metals, chemicals or materials your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPaperPrintedProductsPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPaperPrintedProductsPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-paper", "Paper and paper products"),
          ("sector-label-printed", "Printed products")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPaperPrintedProductsPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (paperRelated, navigator.nextPage(paperRelated, "").url),
          (printedProducts, navigator.nextPage(printedProducts, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPaperPrintedProductsPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitPaperPrintedProductsPage().url
              )
                .withFormUrlEncodedBody("manu-g6" -> value)
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
          controller.submitPaperPrintedProductsPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitPaperPrintedProductsPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of paper or printed products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadVehiclesTransportPage" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadVehiclesTransportPage()(
            FakeRequest(GET, routes.GeneralTradeGroupsController.loadVehiclesTransportPage().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-motor", "Motor vehicles, trailers and parts"),
          ("sector-label-other", "Other transport equipment")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitVehiclesTransportPage" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (motorVehicles, navigator.nextPage(motorVehicles, "").url),
          (otherTransportEquipment, navigator.nextPage(otherTransportEquipment, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitVehiclesTransportPage()(
              FakeRequest(
                POST,
                routes.GeneralTradeGroupsController.submitVehiclesTransportPage().url
              )
                .withFormUrlEncodedBody("manu-g7" -> value)
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
          controller.submitVehiclesTransportPage()(
            FakeRequest(POST, routes.GeneralTradeGroupsController.submitVehiclesTransportPage().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of vehicles or transport your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
