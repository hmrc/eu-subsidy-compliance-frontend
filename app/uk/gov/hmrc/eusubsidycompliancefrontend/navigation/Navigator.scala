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

package uk.gov.hmrc.eusubsidycompliancefrontend.navigation


import play.api.mvc.Call
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.routes

import javax.inject.{Inject, Singleton}
import scala.language.postfixOps

@Singleton
class Navigator @Inject(){

  def nextPage(previousAnswer: String, isUpdate : Boolean): Call = isUpdate match {
    case true =>
      normalRoutes(previousAnswer)
    case false =>
      //this will change to update routes maybe.
      normalRoutes(previousAnswer)
  }

  private val normalRoutes: String => Call = {
    case "2" => routes.AgricultureController.loadAgricultureLvl3Page()
    case "01.1" => routes.AgricultureController.loadNonPerennialCropLvl4Page()
    case "01.2" => routes.AgricultureController.loadPerennialCropLvl4Page()
    case "01.4" => routes.AgricultureController.loadAnimalProductionLvl4Page()
    case "01.6" => routes.AgricultureController.loadSupportActivitiesLvl4Page()
    case "02" => routes.AgricultureController.loadForestryLvl3Page()
    case "3" => routes.AgricultureController.loadFishingAndAquacultureLvl3Page()
    case "03.1" => routes.AgricultureController.loadFishingLvl4Page()
    case "03.2" => routes.AgricultureController.loadAquacultureLvl4Page()

     case "B" => routes.MiningController.loadMiningLvl2Page()
     case "05" => routes.MiningController.loadCoalMiningLvl3Page()
     case "06" => routes.MiningController.loadGasMiningLvl3Page()
     case "07" => routes.MiningController.loadMetalMiningLvl3Page()
     case "07.2" => routes.MiningController.loadNonFeMetalMiningLvl4Page()
     case "08" => routes.MiningController.loadOtherMiningLvl3Page()
     case "08.1" => routes.MiningController.loadQuarryingLvl4Page()
     case "08.9" => routes.MiningController.loadOtherMiningLvl4Page()
     case "09" => routes.MiningController.loadMiningSupportLvl3Page()

    case "0"     => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage
    case "INT00" => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage
    case "INT01" => routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage
    case "INT02" => routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage
    case "INT03" => routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage
    case "INT04" => routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage
    case "INT05" => routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage
    case "INT06" => routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage
    case "INT07" => routes.GeneralTradeGroupsController.loadVehiclesTransportPage

    case "F"     => routes.ConstructionController.loadConstructionLvl2Page
    case "42"    => routes.ConstructionController.loadCivilEngineeringLvl3Page
    case "42.1"  => routes.ConstructionController.loadConstructionRoadsRailwaysLvl4Page
    case "42.2"  => routes.ConstructionController.loadConstructionUtilityProjectsLvl4Page
    case "42.9"  => routes.ConstructionController.loadOtherCivilEngineeringProjectsLvl4Page
    case "43"    => routes.ConstructionController.loadSpecialisedConstructionLvl3Page
    case "43.1"  => routes.ConstructionController.loadDemolitionSitePreparationLvl4Page
    case "43.2"  => routes.ConstructionController.loadElectricalPlumbingConstructionLvl4Page
    case "43.3"  => routes.ConstructionController.loadBuildingCompletionLvl4Page
    case "43.4"  => routes.ConstructionController.loadSpecialisedConstructionActivitiesLvl4Page
    case "43.9"  => routes.ConstructionController.loadOtherSpecialisedConstructionLvl4Page

    case "N" => routes.ProfAndPAdminController.loadProfessionalLvl2Page()
    case "69" => routes.ProfAndPAdminController.loadLegalAndAccountingLvl3Page()
    case "70" => routes.ProfAndPAdminController.loadHeadOfficesLvl3Page()
    case "71" => routes.ProfAndPAdminController.loadArchitecturalLvl3Page()
    case "71.1" => routes.ProfAndPAdminController.loadArchitecturalLvl4Page()
    case "72" => routes.ProfAndPAdminController.loadScientificRAndDLvl3Page()
    case "73" => routes.ProfAndPAdminController.loadAdvertisingLvl3Page()
    case "73.1" => routes.ProfAndPAdminController.loadAdvertisingLvl4Page()
    case "74" => routes.ProfAndPAdminController.loadOtherProfessionalLvl3Page()
    case "74.1" => routes.ProfAndPAdminController.loadSpecialisedDesignLvl4Page()
    case "74.9" => routes.ProfAndPAdminController.loadOtherProfessionalLvl4Page()

    case "O" => routes.AdminController.loadAdministrativeLvl2Page()
    case "77" => routes.AdminController.loadRentalLvl3Page()
    case "77.1" => routes.AdminController.loadMotorVehiclesLvl4Page()
    case "77.2" => routes.AdminController.loadPersonalHouseholdLvl4Page()
    case "77.3" => routes.AdminController.loadMachineryEquipmentLvl4Page()
    case "77.5" => routes.AdminController.loadIntermediationServicesLvl4Page()
    case "78" => routes.AdminController.loadEmploymentLvl3Page()
    case "79" => routes.AdminController.loadTravelLvl3Page()
    case "79.1" => routes.AdminController.loadTravelAgencyLvl4Page()
    case "80" => routes.AdminController.loadInvestigationLvl4Page()
    case "81" => routes.AdminController.loadBuildingsLvl3Page()
    case "81.2" => routes.AdminController.loadCleaningLvl4Page()
    case "82" => routes.AdminController.loadOfficeLvl3Page()
    case "82.9" => routes.AdminController.loadOtherBusinessSupportLvl4Page()

    case "84" => routes.ProfAndPAdminController.loadPublicAdminDefenceLvl3Page()
    case "84.1" => routes.ProfAndPAdminController.loadPublicAdminLvl4Page()
    case "84.2" => routes.ProfAndPAdminController.loadServiceProvisionLvl4Page()

    case _       => routes.UndertakingController.getSector
  }
}
