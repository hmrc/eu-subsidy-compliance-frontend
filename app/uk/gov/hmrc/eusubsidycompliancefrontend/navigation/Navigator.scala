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
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.households.HouseholdsLvl2Page
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.humanHealth.{SocialWorkLvl3Page => HouseholdsLvl2Page}

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

    case "10" => routes.FoodBeveragesController.loadFoodLvl3Page
    case "10.9" => routes.FoodBeveragesController.loadAnimalFeedsLvl4Page
    case "10.7" => routes.FoodBeveragesController.loadBakeryAndFarinaceousLvl4Page
    case "10.5" => routes.FoodBeveragesController.loadDairyProductsLvl4Page
    case "10.3" => routes.FoodBeveragesController.loadFruitAndVegLvl4Page
    case "10.6" => routes.FoodBeveragesController.loadGrainAndStarchLvl4Page
    case "10.1" => routes.FoodBeveragesController.loadMeatLvl4Page
    case "10.4" => routes.FoodBeveragesController.loadOilsAndFatsLvl4Page
    case "10.8" => routes.FoodBeveragesController.loadOtherFoodProductsLvl4Page
    case "11" => routes.FoodBeveragesController.loadBeveragesLvl4Page

    case "20.1" => routes.MetalsChemicalsController.loadBasicLvl4Page
    case "24" => routes.MetalsChemicalsController.loadBasicMetalsLvl3Page
    case "24.5" => routes.MetalsChemicalsController.loadCastingMetalsLvl4Page
    case "20" => routes.MetalsChemicalsController.loadChemicalsProductsLvl3Page
    case "19" => routes.MetalsChemicalsController.loadCokePetroleumLvl3Page
    case "25.6" => routes.MetalsChemicalsController.loadCutleryToolsHardwareLvl4Page
    case "25" => routes.MetalsChemicalsController.loadFabricatedMetalsLvl3Page
    case "24.3" => routes.MetalsChemicalsController.loadFirstProcessingSteelLvl4Page
    case "25.9" => routes.MetalsChemicalsController.loadOtherFabricatedProductsLvl4Page
    case "20.5" => routes.MetalsChemicalsController.loadOtherProductsLvl4Page
    case "21" => routes.MetalsChemicalsController.loadPharmaceuticalsLvl3Page
    case "24.4" => routes.MetalsChemicalsController.loadPreciousNonFerrousLvl4Page
    case "25.1" => routes.MetalsChemicalsController.loadStructuralMetalLvl4Page
    case "25.2" => routes.MetalsChemicalsController.loadTanksReservoirsContainersLvl4Page
    case "25.5" => routes.MetalsChemicalsController.loadTreatmentCoatingMachiningLvl4Page
    case "20.4" => routes.MetalsChemicalsController.loadWashingLvl4Page

    case "23" => routes.NonMetallicOtherController.loadNonMetallicMineralLvl3Page
    case "23.1" => routes.NonMetallicOtherController.loadGlassProductsLvl4Page
    case "23.3" => routes.NonMetallicOtherController.loadClayBuildingMaterialsLvl4Page
    case "23.4" => routes.NonMetallicOtherController.loadOtherPorcelainAndCeramicsLvl4Page
    case "23.5" => routes.NonMetallicOtherController.loadCementLimePlasterLvl4Page
    case "23.6" => routes.NonMetallicOtherController.loadConcreteCementPlasterLvl4Page
    case "23.9" => routes.NonMetallicOtherController.loadAnotherTypeLvl4Page

    case "17.2" => routes.PaperPrintedController.loadArticlesPaperPaperboardLvl4Page
     case "17" => routes.PaperPrintedController.loadPaperLvl3Page
     case "18" => routes.PaperPrintedController.loadPrintedLvl3Page
     case "18.1" => routes.PaperPrintedController.loadPrintingServicesLvl4Page
     case "17.1" => routes.PaperPrintedController.loadPulpPaperPaperboardLvl4Page

    case "26.1" => routes.ComputersElectronicsController.loadComponentsBoardsLvl4Page
    case "26" => routes.ComputersElectronicsController.loadComputersElectronicsOpticalLvl3Page
    case "27.5" => routes.ComputersElectronicsController.loadDomesticAppliancesLvl4Page
    case "27" => routes.ComputersElectronicsController.loadElectricalEquipmentLvl3Page
    case "28.1" => routes.ComputersElectronicsController.loadGeneralPurposeLvl4Page
    case "26.5" => routes.ComputersElectronicsController.loadMeasuringTestingInstrumentsLvl4Page
    case "28.4" => routes.ComputersElectronicsController.loadMetalFormingLvl4Page
    case "27.1" => routes.ComputersElectronicsController.loadMotorsGeneratorsLvl4Page
    case "28.2" => routes.ComputersElectronicsController.loadOtherGeneralPurposeLvl4Page
    case "28" => routes.ComputersElectronicsController.loadOtherMachineryLvl3Page
    case "28.9" => routes.ComputersElectronicsController.loadOtherSpecialPurposeLvl4Page
    case "33.1" => routes.ComputersElectronicsController.loadRepairMaintenanceLvl4Page
    case "33" => routes.ComputersElectronicsController.loadRepairsMaintainInstallLvl3Page
    case "27.3" => routes.ComputersElectronicsController.loadWiringAndDevicesLvl4Page

      case "30.3" => routes.VehiclesManuTransportController.loadAircraftSpacecraftLvl4Page
      case "29" => routes.VehiclesManuTransportController.loadMotorVehiclesLvl3Page
      case "30" => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl3Page
      case "30.9" => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl4Page
      case "29.3" => routes.VehiclesManuTransportController.loadPartsAccessoriesLvl4Page
      case "30.1" => routes.VehiclesManuTransportController.loadShipsBoatsLvl4Page

     case "17.2" => routes.PaperPrintedController.loadArticlesPaperPaperboardLvl4Page
     case "17" => routes.PaperPrintedController.loadPaperLvl3Page
     case "18" => routes.PaperPrintedController.loadPrintedLvl3Page
     case "18.1" => routes.PaperPrintedController.loadPrintingServicesLvl4Page
     case "17.1" => routes.PaperPrintedController.loadPulpPaperPaperboardLvl4Page

    case "0"     => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage
    case "INT00" => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage
    case "INT01" => routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage
    case "INT02" => routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage
    case "INT03" => routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage
    case "INT04" => routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage
    case "INT05" => routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage
    case "INT06" => routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage
    case "INT07" => routes.GeneralTradeGroupsController.loadVehiclesTransportPage

    case "32" => routes.NonMetallicOtherController.loadOtherManufacturingLvl3Page
    case "32.1" => routes.NonMetallicOtherController.loadJewelleryCoinsLvl4Page
    case "32.9" => routes.NonMetallicOtherController.loadOtherProductsLvl4Page

    case "35"  => routes.AccomodationUtilitiesController.loadElectricityLvl3Page()
    case "35.1"  => routes.AccomodationUtilitiesController.loadElectricityLvl4Page()
    case "35.2"  => routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page()

    case "38.1"  => routes.AccomodationUtilitiesController.loadWasteCollectionLvl4Page()
    case "38"  => routes.AccomodationUtilitiesController.loadWasteCollectionRecoveryLvl3Page()
    case "38.3"  => routes.AccomodationUtilitiesController.loadWasteDisposalLvl4Page()
    case "38.2"  => routes.AccomodationUtilitiesController.loadWasteRecoveryLvl4Page()
    case "E"  => routes.AccomodationUtilitiesController.loadWaterLvl2Page()

    case "I"  => routes.AccomodationUtilitiesController.loadAccommodationFoodLvl2Page()
    case "55"  => routes.AccomodationUtilitiesController.loadAccommodationLvl3Page()
    case "56.2"  => routes.AccomodationUtilitiesController.loadEventCateringOtherFoodActivitiesLvl4Page()
    case "56"  => routes.AccomodationUtilitiesController.loadFoodBeverageActivitiesLvl3Page()
    case "56.1"  => routes.AccomodationUtilitiesController.loadRestaurantFoodServicesLvl4Page()

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

    case "51.2"  => routes.TransportController.loadAirTransportFreightAirLvl4Page()
    case "51"  => routes.TransportController.loadAirTransportLvl3Page()
    case "49.4"  => routes.TransportController.loadLandTransportFreightTransportLvl4Page()
    case "49"  => routes.TransportController.loadLandTransportLvl3Page()
    case "49.3"  => routes.TransportController.loadLandTransportOtherPassengerLvl4Page()
    case "49.1"  => routes.TransportController.loadLandTransportPassengerRailLvl4Page()
    case "53"  => routes.TransportController.loadPostalAndCourierLvl3Page()
    case "H"  => routes.TransportController.loadTransportLvl2Page()
    case "52.2"  => routes.TransportController.loadWarehousingSupportActivitiesTransportLvl4Page()
    case "52.3"  => routes.TransportController.loadWarehousingIntermediationLvl4Page()
    case "52"  => routes.TransportController.loadWarehousingSupportLvl3Page()
    case "50"  => routes.TransportController.loadWaterTransportLvl3Page()

    case "58.1"  => routes.PublishingTelecomsController.loadBookPublishingLvl4Page()
    case "59"  => routes.PublishingTelecomsController.loadFilmMusicPublishingLvl3Page()
    case "59.1"  => routes.PublishingTelecomsController.loadFilmVideoActivitiesLvl4Page()
    case "60.3"  => routes.PublishingTelecomsController.loadNewsOtherContentDistributionLvl4Page()
    case "60"  => routes.PublishingTelecomsController.loadProgrammingBroadcastingDistributionLvl3Page()
    case "J"  => routes.PublishingTelecomsController.loadPublishingLvl2Page()
    case "58"  => routes.PublishingTelecomsController.loadPublishingLvl3Page()
    case "58.2"  => routes.PublishingTelecomsController.loadSoftwarePublishingLvl4Page()

    case "63"  => routes.PublishingTelecomsController.loadComputerInfrastructureDataHostingLvl3Page()
    case "62"  => routes.PublishingTelecomsController.loadComputerProgrammingConsultancyLvl3Page()
    case "K"  => routes.PublishingTelecomsController.loadTelecommunicationLvl2Page()
    case "61"  => routes.PublishingTelecomsController.loadTelecommunicationLvl3Page()
    case "63.9"  => routes.PublishingTelecomsController.loadWebSearchPortalLvl4Page()

    case "L" => routes.FinanceRealEstateController.loadFinanceInsuranceLvl2Page()
    case "64" => routes.FinanceRealEstateController.loadFinancialServicesLvl3Page()
    case "64.1" => routes.FinanceRealEstateController.loadMonetaryIntermediationLvl4Page()
    case "64.2" => routes.FinanceRealEstateController.loadHoldingCompaniesLvl4Page()
    case "64.3" => routes.FinanceRealEstateController.loadTrustsFundsLvl4Page()
    case "64.9" => routes.FinanceRealEstateController.loadOtherFinancialLvl4Page()
    case "65" => routes.FinanceRealEstateController.loadInsuranceLvl3Page()
    case "65.1" => routes.FinanceRealEstateController.loadInsuranceTypeLvl4Page()
    case "66" => routes.FinanceRealEstateController.loadAuxiliaryFinancialLvl3Page()
    case "66.1" => routes.FinanceRealEstateController.loadAuxiliaryNonInsuranceLvl4Page()
    case "66.2" => routes.FinanceRealEstateController.loadAuxiliaryInsuranceLvl4Page()

    case "M" => routes.FinanceRealEstateController.loadRealEstateLvl3Page()
    case "68.1" => routes.FinanceRealEstateController.loadPropertyDevelopmentLvl4Page()
    case "68.3" => routes.FinanceRealEstateController.loadFeeContractLvl4Page()

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

    case "85"  => routes.HouseHealthEducationController.loadEducationLvl3Page()
    case "85.3"  => routes.HouseHealthEducationController.loadSecondaryEducationLvl4Page()
    case "85.5"  => routes.HouseHealthEducationController.loadOtherEducationLvl4Page()
    case "85.6"  => routes.HouseHealthEducationController.loadEducationalSupportLvl4Page()

    case "R"  => routes.HouseHealthEducationController.loadHumanHealthLvl2Page()
    case "86"  => routes.HouseHealthEducationController.loadHumanHealthLvl3Page()
    case "86.2"  => routes.HouseHealthEducationController.loadMedicalDentalLvl4Page()
    case "86.9"  => routes.HouseHealthEducationController.loadOtherHumanHealthLvl4Page()
    case "87"  => routes.HouseHealthEducationController.loadResidentialCareLvl3Page()
    case "87.9"  => routes.HouseHealthEducationController.loadOtherResidentialCareLvl4Page()
    case "88"  => routes.HouseHealthEducationController.loadSocialWorkLvl3Page()
    case "88.9"  => routes.HouseHealthEducationController.loadOtherSocialWorkLvl4Page()

    case "S"  => routes.ArtsController.loadArtsSportsRecreationLvl2Page()
    case "90"  => routes.ArtsController.loadArtsCreationPerformingLvl3Page()
    case "90.1"  => routes.ArtsController.loadArtsCreationLvl4Page()
    case "90.3"  => routes.ArtsController.loadArtsPerformingSupportActivitiesLvl4Page()
    case "91"  => routes.ArtsController.loadLibrariesArchivesCulturalLvl3Page()
    case "91.1"  => routes.ArtsController.loadLibrariesArchivesLvl4Page()
    case "91.2"  => routes.ArtsController.loadMuseumsCollectionsMomumentsLvl4Page()
    case "91.4"  => routes.ArtsController.loadBotanicalZoologicalReservesLvl4Page()
    case "93"  => routes.ArtsController.loadSportsAmusementRecreationLvl3Page()
    case "93.1"  => routes.ArtsController.loadSportsLvl4Page()
    case "93.2"  => routes.ArtsController.loadAmusementAndRecreationLvl4Page()

    case "U"  => routes.HouseHealthEducationController.loadHouseholdsLvl2Page()
    case "98"  => routes.HouseHealthEducationController.loadUndifferentiatedProducingActivitiesLvl4Page()

    case "T"  => routes.OtherServicesController.loadOtherLvl2Page()
    case "94"  => routes.OtherServicesController.loadMembershipOrgActivitiesLvl3Page()
    case "94.1"  => routes.OtherServicesController.loadMembershipOrgsLvl4Page()
    case "94.9"  => routes.OtherServicesController.loadOtherMembershipOrgsLvl4Page()
    case "95"  => routes.OtherServicesController.loadRepairsLvl3Page()
    case "95.2"  => routes.OtherServicesController.loadHouseholdRepairLvl4Page()
    case "95.3"  => routes.OtherServicesController.loadMotorVehiclesRepairLvl4Page()
    case "96"  => routes.OtherServicesController.loadPersonalServicesLvl3Page()
    case "96.2"  => routes.OtherServicesController.loadHairdressingLvl4Page()
    case "96.9"  => routes.OtherServicesController.loadOtherPersonalServicesLvl4Page()

    case "G"  => routes.RetailWholesaleController.loadRetailWholesaleLvl2Page()
    case "46"  => routes.RetailWholesaleController.loadWholesaleLvl3Page()
    case "46.1"  => routes.RetailWholesaleController.loadContractBasisLvl4Page()
    case "46.2"  => routes.RetailWholesaleController.loadAgriculturalLvl4Page()
    case "46.3"  => routes.RetailWholesaleController.loadFoodWholesaleLvl4Page()
    case "46.4"  => routes.RetailWholesaleController.loadHouseholdWholesaleLvl4Page()
    case "46.6"  => routes.RetailWholesaleController.loadMachineryLvl4Page()
    case "46.7"  => routes.RetailWholesaleController.loadMotorVehiclesWholesaleLvl4Page()
    case "46.8"  => routes.RetailWholesaleController.loadSpecialisedLvl4Page()
    case "47"  => routes.RetailWholesaleController.loadRetailLvl3Page()
    case "47.1"  => routes.RetailWholesaleController.loadNonSpecialisedLvl4Page()
    case "47.2"  => routes.RetailWholesaleController.loadFoodLvl4Page()
    case "47.5"  => routes.RetailWholesaleController.loadHouseholdLvl4Page()
    case "47.6"  => routes.RetailWholesaleController.loadCulturalLvl4Page()
    case "47.7"  => routes.RetailWholesaleController.loadOtherGoodsLvl4Page()
    case "47.8"  => routes.RetailWholesaleController.loadMotorVehiclesLvl4Page()
    case "47.9"  => routes.RetailWholesaleController.loadIntermediationLvl4Page()

    case "14"  => routes.ClothesTextilesHomewareController.loadClothingLvl3Page
    case "15"  => routes.ClothesTextilesHomewareController.loadLeatherLvl3Page
    case "22"  => routes.ClothesTextilesHomewareController.loadRubberPlasticLvl3Page
    case "13"  => routes.ClothesTextilesHomewareController.loadTextilesLvl3Page
    case "16"  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawLvl3Page
    case "13.9"  => routes.ClothesTextilesHomewareController.loadManufactureOfTextilesLvl4Page
    case "14.2"  => routes.ClothesTextilesHomewareController.loadOtherClothingLvl4Page
    case "22.2"  => routes.ClothesTextilesHomewareController.loadPlasticLvl4Page
    case "22.1"  => routes.ClothesTextilesHomewareController.loadRubberLvl4Page
    case "16.1"  => routes.ClothesTextilesHomewareController.loadSawmillingWoodworkLvl4Page
    case "15.1"  => routes.ClothesTextilesHomewareController.loadTanningDressingDyeingLvl4Page
    case "16.2"  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawPlaitingLvl4Page

    case _       => routes.UndertakingController.getSector
  }
}
