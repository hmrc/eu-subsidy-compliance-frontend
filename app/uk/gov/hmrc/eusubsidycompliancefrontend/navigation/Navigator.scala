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
    case true => updateRoutes()
    case false => normalRoutes(previousAnswer, isUpdate)
  }

  private val normalRoutes: (String, Boolean) => Call = {
    case ("0", isUpdate)     => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage(isUpdate)
    case ("INT00", isUpdate) => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage(isUpdate)
    case ("INT01", isUpdate) => routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage(isUpdate)
    case ("INT02", isUpdate) => routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage(isUpdate)
    case ("INT03", isUpdate) => routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage(isUpdate)
    case ("INT04", isUpdate) => routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage(isUpdate)
    case ("INT05", isUpdate) => routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage(isUpdate)
    case ("INT06", isUpdate) => routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage(isUpdate)
    case ("INT07", isUpdate) => routes.GeneralTradeGroupsController.loadVehiclesTransportPage(isUpdate)

    case ("2", isUpdate) => routes.AgricultureController.loadAgricultureLvl3Page(isUpdate)
    case ("01.1", isUpdate) => routes.AgricultureController.loadNonPerennialCropLvl4Page(isUpdate)
    case ("01.2", isUpdate) => routes.AgricultureController.loadPerennialCropLvl4Page(isUpdate)
    case ("01.4", isUpdate) => routes.AgricultureController.loadAnimalProductionLvl4Page(isUpdate)
    case ("01.6", isUpdate) => routes.AgricultureController.loadSupportActivitiesLvl4Page(isUpdate)
    case ("02", isUpdate) => routes.AgricultureController.loadForestryLvl3Page(isUpdate)
    case ("3", isUpdate) => routes.AgricultureController.loadFishingAndAquacultureLvl3Page(isUpdate)
    case ("03.1", isUpdate) => routes.AgricultureController.loadFishingLvl4Page(isUpdate)
    case ("03.2", isUpdate) => routes.AgricultureController.loadAquacultureLvl4Page(isUpdate)

     case ("B", isUpdate) => routes.MiningController.loadMiningLvl2Page(isUpdate)
     case ("05", isUpdate) => routes.MiningController.loadCoalMiningLvl3Page(isUpdate)
     case ("06", isUpdate) => routes.MiningController.loadGasMiningLvl3Page(isUpdate)
     case ("07", isUpdate) => routes.MiningController.loadMetalMiningLvl3Page(isUpdate)
     case ("07.2", isUpdate) => routes.MiningController.loadNonFeMetalMiningLvl4Page(isUpdate)
     case ("08", isUpdate) => routes.MiningController.loadOtherMiningLvl3Page(isUpdate)
     case ("08.1", isUpdate) => routes.MiningController.loadQuarryingLvl4Page(isUpdate)
     case ("08.9", isUpdate) => routes.MiningController.loadOtherMiningLvl4Page(isUpdate)
     case ("09", isUpdate) => routes.MiningController.loadMiningSupportLvl3Page(isUpdate)

    case ("10", isUpdate) => routes.FoodBeveragesController.loadFoodLvl3Page(isUpdate)
    case ("10.9", isUpdate) => routes.FoodBeveragesController.loadAnimalFeedsLvl4Page(isUpdate)
    case ("10.7", isUpdate) => routes.FoodBeveragesController.loadBakeryAndFarinaceousLvl4Page(isUpdate)
    case ("10.5", isUpdate) => routes.FoodBeveragesController.loadDairyProductsLvl4Page(isUpdate)
    case ("10.3", isUpdate) => routes.FoodBeveragesController.loadFruitAndVegLvl4Page(isUpdate)
    case ("10.6", isUpdate) => routes.FoodBeveragesController.loadGrainAndStarchLvl4Page(isUpdate)
    case ("10.1", isUpdate) => routes.FoodBeveragesController.loadMeatLvl4Page(isUpdate)
    case ("10.4", isUpdate) => routes.FoodBeveragesController.loadOilsAndFatsLvl4Page(isUpdate)
    case ("10.8", isUpdate) => routes.FoodBeveragesController.loadOtherFoodProductsLvl4Page(isUpdate)
    case ("11", isUpdate) => routes.FoodBeveragesController.loadBeveragesLvl4Page(isUpdate)

    case ("13", isUpdate)  => routes.ClothesTextilesHomewareController.loadTextilesLvl3Page(isUpdate)
    case ("13.9", isUpdate)  => routes.ClothesTextilesHomewareController.loadManufactureOfTextilesLvl4Page(isUpdate)
    case ("14", isUpdate)  => routes.ClothesTextilesHomewareController.loadClothingLvl3Page(isUpdate)
    case ("14.2", isUpdate)  => routes.ClothesTextilesHomewareController.loadOtherClothingLvl4Page(isUpdate)
    case ("15", isUpdate)  => routes.ClothesTextilesHomewareController.loadLeatherLvl3Page(isUpdate)
    case ("15.1", isUpdate)  => routes.ClothesTextilesHomewareController.loadTanningDressingDyeingLvl4Page(isUpdate)
    case ("16", isUpdate)  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawLvl3Page(isUpdate)
    case ("16.1", isUpdate)  => routes.ClothesTextilesHomewareController.loadSawmillingWoodworkLvl4Page(isUpdate)
    case ("16.2", isUpdate)  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawPlaitingLvl4Page(isUpdate)
    case ("22", isUpdate)  => routes.ClothesTextilesHomewareController.loadRubberPlasticLvl3Page(isUpdate)
    case ("22.2", isUpdate)  => routes.ClothesTextilesHomewareController.loadPlasticLvl4Page(isUpdate)
    case ("22.1", isUpdate)  => routes.ClothesTextilesHomewareController.loadRubberLvl4Page(isUpdate)

    case ("17.2", isUpdate) => routes.PaperPrintedController.loadArticlesPaperPaperboardLvl4Page(isUpdate)
    case ("17", isUpdate) => routes.PaperPrintedController.loadPaperLvl3Page(isUpdate)
    case ("18", isUpdate) => routes.PaperPrintedController.loadPrintedLvl3Page(isUpdate)
    case ("18.1", isUpdate) => routes.PaperPrintedController.loadPrintingServicesLvl4Page(isUpdate)
    case ("17.1", isUpdate) => routes.PaperPrintedController.loadPulpPaperPaperboardLvl4Page(isUpdate)

    case ("20.1", isUpdate) => routes.MetalsChemicalsController.loadBasicLvl4Page(isUpdate)
    case ("24", isUpdate) => routes.MetalsChemicalsController.loadBasicMetalsLvl3Page(isUpdate)
    case ("24.5", isUpdate) => routes.MetalsChemicalsController.loadCastingMetalsLvl4Page(isUpdate)
    case ("20", isUpdate) => routes.MetalsChemicalsController.loadChemicalsProductsLvl3Page(isUpdate)
    case ("19", isUpdate) => routes.MetalsChemicalsController.loadCokePetroleumLvl3Page(isUpdate)
    case ("25.6", isUpdate) => routes.MetalsChemicalsController.loadCutleryToolsHardwareLvl4Page(isUpdate)
    case ("25", isUpdate) => routes.MetalsChemicalsController.loadFabricatedMetalsLvl3Page(isUpdate)
    case ("24.3", isUpdate) => routes.MetalsChemicalsController.loadFirstProcessingSteelLvl4Page(isUpdate)
    case ("25.9", isUpdate) => routes.MetalsChemicalsController.loadOtherFabricatedProductsLvl4Page(isUpdate)
    case ("20.5", isUpdate) => routes.MetalsChemicalsController.loadOtherProductsLvl4Page(isUpdate)
    case ("21", isUpdate) => routes.MetalsChemicalsController.loadPharmaceuticalsLvl3Page(isUpdate)
    case ("24.4", isUpdate) => routes.MetalsChemicalsController.loadPreciousNonFerrousLvl4Page(isUpdate)
    case ("25.1", isUpdate) => routes.MetalsChemicalsController.loadStructuralMetalLvl4Page(isUpdate)
    case ("25.2", isUpdate) => routes.MetalsChemicalsController.loadTanksReservoirsContainersLvl4Page(isUpdate)
    case ("25.5", isUpdate) => routes.MetalsChemicalsController.loadTreatmentCoatingMachiningLvl4Page(isUpdate)
    case ("20.4", isUpdate) => routes.MetalsChemicalsController.loadWashingLvl4Page(isUpdate)

    case ("23", isUpdate) => routes.NonMetallicOtherController.loadNonMetallicMineralLvl3Page(isUpdate)
    case ("23.1", isUpdate) => routes.NonMetallicOtherController.loadGlassProductsLvl4Page(isUpdate)
    case ("23.3", isUpdate) => routes.NonMetallicOtherController.loadClayBuildingMaterialsLvl4Page(isUpdate)
    case ("23.4", isUpdate) => routes.NonMetallicOtherController.loadOtherPorcelainAndCeramicsLvl4Page(isUpdate)
    case ("23.5", isUpdate) => routes.NonMetallicOtherController.loadCementLimePlasterLvl4Page(isUpdate)
    case ("23.6", isUpdate) => routes.NonMetallicOtherController.loadConcreteCementPlasterLvl4Page(isUpdate)
    case ("23.9", isUpdate) => routes.NonMetallicOtherController.loadAnotherTypeLvl4Page(isUpdate)

    case ("26.1", isUpdate) => routes.ComputersElectronicsController.loadComponentsBoardsLvl4Page(isUpdate)
    case ("26", isUpdate) => routes.ComputersElectronicsController.loadComputersElectronicsOpticalLvl3Page(isUpdate)
    case ("27.5", isUpdate) => routes.ComputersElectronicsController.loadDomesticAppliancesLvl4Page(isUpdate)
    case ("27", isUpdate) => routes.ComputersElectronicsController.loadElectricalEquipmentLvl3Page(isUpdate)
    case ("28.1", isUpdate) => routes.ComputersElectronicsController.loadGeneralPurposeLvl4Page(isUpdate)
    case ("26.5", isUpdate) => routes.ComputersElectronicsController.loadMeasuringTestingInstrumentsLvl4Page(isUpdate)
    case ("28.4", isUpdate) => routes.ComputersElectronicsController.loadMetalFormingLvl4Page(isUpdate)
    case ("27.1", isUpdate) => routes.ComputersElectronicsController.loadMotorsGeneratorsLvl4Page(isUpdate)
    case ("28.2", isUpdate) => routes.ComputersElectronicsController.loadOtherGeneralPurposeLvl4Page(isUpdate)
    case ("28", isUpdate) => routes.ComputersElectronicsController.loadOtherMachineryLvl3Page(isUpdate)
    case ("28.9", isUpdate) => routes.ComputersElectronicsController.loadOtherSpecialPurposeLvl4Page(isUpdate)
    case ("33.1", isUpdate) => routes.ComputersElectronicsController.loadRepairMaintenanceLvl4Page(isUpdate)
    case ("33", isUpdate) => routes.ComputersElectronicsController.loadRepairsMaintainInstallLvl3Page(isUpdate)
    case ("27.3", isUpdate) => routes.ComputersElectronicsController.loadWiringAndDevicesLvl4Page(isUpdate)

    case ("30.3", isUpdate) => routes.VehiclesManuTransportController.loadAircraftSpacecraftLvl4Page(isUpdate)
    case ("29", isUpdate) => routes.VehiclesManuTransportController.loadMotorVehiclesLvl3Page(isUpdate)
    case ("30", isUpdate) => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl3Page(isUpdate)
    case ("30.9", isUpdate) => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl4Page(isUpdate)
    case ("29.3", isUpdate) => routes.VehiclesManuTransportController.loadPartsAccessoriesLvl4Page(isUpdate)
    case ("30.1", isUpdate) => routes.VehiclesManuTransportController.loadShipsBoatsLvl4Page(isUpdate)

    case ("32", isUpdate) => routes.NonMetallicOtherController.loadOtherManufacturingLvl3Page(isUpdate)
    case ("32.1", isUpdate) => routes.NonMetallicOtherController.loadJewelleryCoinsLvl4Page(isUpdate)
    case ("32.9", isUpdate) => routes.NonMetallicOtherController.loadOtherProductsLvl4Page(isUpdate)

    case ("D", isUpdate)  => routes.AccomodationUtilitiesController.loadElectricityLvl3Page(isUpdate)
    case ("35.1", isUpdate)  => routes.AccomodationUtilitiesController.loadElectricityLvl4Page(isUpdate)
    case ("35.2", isUpdate)  => routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page(isUpdate)

    case ("38.1", isUpdate)  => routes.AccomodationUtilitiesController.loadWasteCollectionLvl4Page(isUpdate)
    case ("38", isUpdate)  => routes.AccomodationUtilitiesController.loadWasteCollectionRecoveryLvl3Page(isUpdate)
    case ("38.3", isUpdate)  => routes.AccomodationUtilitiesController.loadWasteDisposalLvl4Page(isUpdate)
    case ("38.2", isUpdate)  => routes.AccomodationUtilitiesController.loadWasteRecoveryLvl4Page(isUpdate)
    case ("E", isUpdate)  => routes.AccomodationUtilitiesController.loadWaterLvl2Page(isUpdate)

    case ("F", isUpdate)     => routes.ConstructionController.loadConstructionLvl2Page(isUpdate)
    case ("42", isUpdate)    => routes.ConstructionController.loadCivilEngineeringLvl3Page(isUpdate)
    case ("42.1", isUpdate)  => routes.ConstructionController.loadConstructionRoadsRailwaysLvl4Page(isUpdate)
    case ("42.2", isUpdate)  => routes.ConstructionController.loadConstructionUtilityProjectsLvl4Page(isUpdate)
    case ("42.9", isUpdate)  => routes.ConstructionController.loadOtherCivilEngineeringProjectsLvl4Page(isUpdate)
    case ("43", isUpdate)    => routes.ConstructionController.loadSpecialisedConstructionLvl3Page(isUpdate)
    case ("43.1", isUpdate)  => routes.ConstructionController.loadDemolitionSitePreparationLvl4Page(isUpdate)
    case ("43.2", isUpdate)  => routes.ConstructionController.loadElectricalPlumbingConstructionLvl4Page(isUpdate)
    case ("43.3", isUpdate)  => routes.ConstructionController.loadBuildingCompletionLvl4Page(isUpdate)
    case ("43.4", isUpdate)  => routes.ConstructionController.loadSpecialisedConstructionActivitiesLvl4Page(isUpdate)
    case ("43.9", isUpdate)  => routes.ConstructionController.loadOtherSpecialisedConstructionLvl4Page(isUpdate)

    case ("G", isUpdate)  => routes.RetailWholesaleController.loadRetailWholesaleLvl2Page(isUpdate)
    case ("46", isUpdate)  => routes.RetailWholesaleController.loadWholesaleLvl3Page(isUpdate)
    case ("46.1", isUpdate)  => routes.RetailWholesaleController.loadContractBasisLvl4Page(isUpdate)
    case ("46.2", isUpdate) => routes.RetailWholesaleController.loadAgriculturalLvl4Page(isUpdate)
    case ("46.3", isUpdate)  => routes.RetailWholesaleController.loadFoodWholesaleLvl4Page(isUpdate)
    case ("46.4", isUpdate)  => routes.RetailWholesaleController.loadHouseholdWholesaleLvl4Page(isUpdate)
    case ("46.6", isUpdate)  => routes.RetailWholesaleController.loadMachineryLvl4Page(isUpdate)
    case ("46.7", isUpdate)  => routes.RetailWholesaleController.loadMotorVehiclesWholesaleLvl4Page(isUpdate)
    case ("46.8", isUpdate)  => routes.RetailWholesaleController.loadSpecialisedLvl4Page(isUpdate)
    case ("47", isUpdate)  => routes.RetailWholesaleController.loadRetailLvl3Page(isUpdate)
    case ("47.1", isUpdate)  => routes.RetailWholesaleController.loadNonSpecialisedLvl4Page(isUpdate)
    case ("47.2", isUpdate)  => routes.RetailWholesaleController.loadFoodLvl4Page(isUpdate)
    case ("47.5", isUpdate) => routes.RetailWholesaleController.loadHouseholdLvl4Page(isUpdate)
    case ("47.6", isUpdate)  => routes.RetailWholesaleController.loadCulturalLvl4Page(isUpdate)
    case ("47.7", isUpdate)  => routes.RetailWholesaleController.loadOtherGoodsLvl4Page(isUpdate)
    case ("47.8", isUpdate)  => routes.RetailWholesaleController.loadMotorVehiclesLvl4Page(isUpdate)
    case ("47.9", isUpdate)  => routes.RetailWholesaleController.loadIntermediationLvl4Page(isUpdate)

    case ("H", isUpdate)  => routes.TransportController.loadTransportLvl2Page(isUpdate)
    case ("49", isUpdate)  => routes.TransportController.loadLandTransportLvl3Page(isUpdate)
    case ("49.1", isUpdate)  => routes.TransportController.loadLandTransportPassengerRailLvl4Page(isUpdate)
    case ("49.3", isUpdate)  => routes.TransportController.loadLandTransportOtherPassengerLvl4Page(isUpdate)
    case ("49.4", isUpdate)  => routes.TransportController.loadLandTransportFreightTransportLvl4Page(isUpdate)
    case ("50", isUpdate)  => routes.TransportController.loadWaterTransportLvl3Page(isUpdate)
    case ("51", isUpdate)  => routes.TransportController.loadAirTransportLvl3Page(isUpdate)
    case ("51.2", isUpdate)  => routes.TransportController.loadAirTransportFreightAirLvl4Page(isUpdate)
    case ("52", isUpdate)  => routes.TransportController.loadWarehousingSupportLvl3Page(isUpdate)
    case ("52.2", isUpdate)  => routes.TransportController.loadWarehousingSupportActivitiesTransportLvl4Page(isUpdate)
    case ("52.3", isUpdate)  => routes.TransportController.loadWarehousingIntermediationLvl4Page(isUpdate)
    case ("53", isUpdate)  => routes.TransportController.loadPostalAndCourierLvl3Page(isUpdate)

    case ("I", isUpdate)  => routes.AccomodationUtilitiesController.loadAccommodationFoodLvl2Page(isUpdate)
    case ("55", isUpdate)  => routes.AccomodationUtilitiesController.loadAccommodationLvl3Page(isUpdate)
    case ("56", isUpdate)  => routes.AccomodationUtilitiesController.loadFoodBeverageActivitiesLvl3Page(isUpdate)
    case ("56.1", isUpdate)  => routes.AccomodationUtilitiesController.loadRestaurantFoodServicesLvl4Page(isUpdate)
    case ("56.2", isUpdate)  => routes.AccomodationUtilitiesController.loadEventCateringOtherFoodActivitiesLvl4Page(isUpdate)

    case ("J", isUpdate)  => routes.PublishingTelecomsController.loadPublishingLvl2Page(isUpdate)
    case ("58", isUpdate)  => routes.PublishingTelecomsController.loadPublishingLvl3Page(isUpdate)
    case ("58.1", isUpdate)  => routes.PublishingTelecomsController.loadBookPublishingLvl4Page(isUpdate)
    case ("58.2", isUpdate)  => routes.PublishingTelecomsController.loadSoftwarePublishingLvl4Page(isUpdate)
    case ("59", isUpdate)  => routes.PublishingTelecomsController.loadFilmMusicPublishingLvl3Page(isUpdate)
    case ("59.1", isUpdate)  => routes.PublishingTelecomsController.loadFilmVideoActivitiesLvl4Page(isUpdate)
    case ("60", isUpdate)  => routes.PublishingTelecomsController.loadProgrammingBroadcastingDistributionLvl3Page(isUpdate)
    case ("60.3", isUpdate)  => routes.PublishingTelecomsController.loadNewsOtherContentDistributionLvl4Page(isUpdate)

    case ("K", isUpdate)  => routes.PublishingTelecomsController.loadTelecommunicationLvl2Page(isUpdate)
    case ("61", isUpdate)  => routes.PublishingTelecomsController.loadTelecommunicationLvl3Page(isUpdate)
    case ("62", isUpdate)  => routes.PublishingTelecomsController.loadComputerProgrammingConsultancyLvl3Page(isUpdate)
    case ("63", isUpdate)  => routes.PublishingTelecomsController.loadComputerInfrastructureDataHostingLvl3Page(isUpdate)
    case ("63.9", isUpdate)  => routes.PublishingTelecomsController.loadWebSearchPortalLvl4Page(isUpdate)

    case ("L", isUpdate) => routes.FinanceRealEstateController.loadFinanceInsuranceLvl2Page(isUpdate)
    case ("64", isUpdate) => routes.FinanceRealEstateController.loadFinancialServicesLvl3Page(isUpdate)
    case ("64.1", isUpdate) => routes.FinanceRealEstateController.loadMonetaryIntermediationLvl4Page(isUpdate)
    case ("64.2", isUpdate) => routes.FinanceRealEstateController.loadHoldingCompaniesLvl4Page(isUpdate)
    case ("64.3", isUpdate) => routes.FinanceRealEstateController.loadTrustsFundsLvl4Page(isUpdate)
    case ("64.9", isUpdate) => routes.FinanceRealEstateController.loadOtherFinancialLvl4Page(isUpdate)
    case ("65", isUpdate) => routes.FinanceRealEstateController.loadInsuranceLvl3Page(isUpdate)
    case ("65.1", isUpdate) => routes.FinanceRealEstateController.loadInsuranceTypeLvl4Page(isUpdate)
    case ("66", isUpdate) => routes.FinanceRealEstateController.loadAuxiliaryFinancialLvl3Page(isUpdate)
    case ("66.1", isUpdate) => routes.FinanceRealEstateController.loadAuxiliaryNonInsuranceLvl4Page(isUpdate)
    case ("66.2", isUpdate) => routes.FinanceRealEstateController.loadAuxiliaryInsuranceLvl4Page(isUpdate)

    case ("M", isUpdate) => routes.FinanceRealEstateController.loadRealEstateLvl3Page(isUpdate)
    case ("68.1", isUpdate) => routes.FinanceRealEstateController.loadPropertyDevelopmentLvl4Page(isUpdate)
    case ("68.3", isUpdate) => routes.FinanceRealEstateController.loadFeeContractLvl4Page(isUpdate)

    case ("N", isUpdate) => routes.ProfAndPAdminController.loadProfessionalLvl2Page(isUpdate)
    case ("69", isUpdate) => routes.ProfAndPAdminController.loadLegalAndAccountingLvl3Page(isUpdate)
    case ("70", isUpdate) => routes.ProfAndPAdminController.loadHeadOfficesLvl3Page(isUpdate)
    case ("71", isUpdate) => routes.ProfAndPAdminController.loadArchitecturalLvl3Page(isUpdate)
    case ("71.1", isUpdate) => routes.ProfAndPAdminController.loadArchitecturalLvl4Page(isUpdate)
    case ("72", isUpdate) => routes.ProfAndPAdminController.loadScientificRAndDLvl3Page(isUpdate)
    case ("73", isUpdate) => routes.ProfAndPAdminController.loadAdvertisingLvl3Page(isUpdate)
    case ("73.1", isUpdate) => routes.ProfAndPAdminController.loadAdvertisingLvl4Page(isUpdate)
    case ("74", isUpdate) => routes.ProfAndPAdminController.loadOtherProfessionalLvl3Page(isUpdate)
    case ("74.1", isUpdate) => routes.ProfAndPAdminController.loadSpecialisedDesignLvl4Page(isUpdate)
    case ("74.9", isUpdate) => routes.ProfAndPAdminController.loadOtherProfessionalLvl4Page(isUpdate)

    case ("O", isUpdate) => routes.AdminController.loadAdministrativeLvl2Page(isUpdate)
    case ("77", isUpdate) => routes.AdminController.loadRentalLvl3Page(isUpdate)
    case ("77.1", isUpdate) => routes.AdminController.loadMotorVehiclesLvl4Page(isUpdate)
    case ("77.2", isUpdate) => routes.AdminController.loadPersonalHouseholdLvl4Page(isUpdate)
    case ("77.3", isUpdate) => routes.AdminController.loadMachineryEquipmentLvl4Page(isUpdate)
    case ("77.5", isUpdate) => routes.AdminController.loadIntermediationServicesLvl4Page(isUpdate)
    case ("78", isUpdate) => routes.AdminController.loadEmploymentLvl3Page(isUpdate)
    case ("79", isUpdate) => routes.AdminController.loadTravelLvl3Page(isUpdate)
    case ("79.1", isUpdate) => routes.AdminController.loadTravelAgencyLvl4Page(isUpdate)
    case ("80", isUpdate) => routes.AdminController.loadInvestigationLvl4Page(isUpdate)
    case ("81", isUpdate) => routes.AdminController.loadBuildingsLvl3Page(isUpdate)
    case ("81.2", isUpdate) => routes.AdminController.loadCleaningLvl4Page(isUpdate)
    case ("82", isUpdate) => routes.AdminController.loadOfficeLvl3Page(isUpdate)
    case ("82.9", isUpdate) => routes.AdminController.loadOtherBusinessSupportLvl4Page(isUpdate)

    case ("84", isUpdate) => routes.ProfAndPAdminController.loadPublicAdminDefenceLvl3Page(isUpdate)
    case ("84.1", isUpdate) => routes.ProfAndPAdminController.loadPublicAdminLvl4Page(isUpdate)
    case ("84.2", isUpdate) => routes.ProfAndPAdminController.loadServiceProvisionLvl4Page(isUpdate)

    case ("85", isUpdate)  => routes.HouseHealthEducationController.loadEducationLvl3Page(isUpdate)
    case ("85.3", isUpdate)  => routes.HouseHealthEducationController.loadSecondaryEducationLvl4Page(isUpdate)
    case ("85.5", isUpdate)  => routes.HouseHealthEducationController.loadOtherEducationLvl4Page(isUpdate)
    case ("85.6", isUpdate)  => routes.HouseHealthEducationController.loadEducationalSupportLvl4Page(isUpdate)

    case ("R", isUpdate)  => routes.HouseHealthEducationController.loadHumanHealthLvl2Page(isUpdate)
    case ("86", isUpdate) => routes.HouseHealthEducationController.loadHumanHealthLvl3Page(isUpdate)
    case ("86.2", isUpdate)  => routes.HouseHealthEducationController.loadMedicalDentalLvl4Page(isUpdate)
    case ("86.9", isUpdate)  => routes.HouseHealthEducationController.loadOtherHumanHealthLvl4Page(isUpdate)
    case ("87", isUpdate)  => routes.HouseHealthEducationController.loadResidentialCareLvl3Page(isUpdate)
    case ("87.9", isUpdate)  => routes.HouseHealthEducationController.loadOtherResidentialCareLvl4Page(isUpdate)
    case ("88", isUpdate)  => routes.HouseHealthEducationController.loadSocialWorkLvl3Page(isUpdate)
    case ("88.9", isUpdate)  => routes.HouseHealthEducationController.loadOtherSocialWorkLvl4Page(isUpdate)

    case ("S", isUpdate)  => routes.ArtsController.loadArtsSportsRecreationLvl2Page(isUpdate)
    case ("90", isUpdate)  => routes.ArtsController.loadArtsCreationPerformingLvl3Page(isUpdate)
    case ("90.1", isUpdate)  => routes.ArtsController.loadArtsCreationLvl4Page(isUpdate)
    case ("90.3", isUpdate)  => routes.ArtsController.loadArtsPerformingSupportActivitiesLvl4Page(isUpdate)
    case ("91", isUpdate)  => routes.ArtsController.loadLibrariesArchivesCulturalLvl3Page(isUpdate)
    case ("91.1", isUpdate) => routes.ArtsController.loadLibrariesArchivesLvl4Page(isUpdate)
    case ("91.2", isUpdate)  => routes.ArtsController.loadMuseumsCollectionsMomumentsLvl4Page(isUpdate)
    case ("91.4", isUpdate) => routes.ArtsController.loadBotanicalZoologicalReservesLvl4Page(isUpdate)
    case ("93", isUpdate)  => routes.ArtsController.loadSportsAmusementRecreationLvl3Page(isUpdate)
    case ("93.1", isUpdate) => routes.ArtsController.loadSportsLvl4Page(isUpdate)
    case ("93.2", isUpdate) => routes.ArtsController.loadAmusementAndRecreationLvl4Page(isUpdate)

    case ("U", isUpdate)  => routes.HouseHealthEducationController.loadHouseholdsLvl2Page(isUpdate)
    case ("98", isUpdate)  => routes.HouseHealthEducationController.loadUndifferentiatedProducingActivitiesLvl4Page(isUpdate)

    case ("T", isUpdate)  => routes.OtherServicesController.loadOtherLvl2Page(isUpdate)
    case ("94", isUpdate)  => routes.OtherServicesController.loadMembershipOrgActivitiesLvl3Page(isUpdate)
    case ("94.1", isUpdate)  => routes.OtherServicesController.loadMembershipOrgsLvl4Page(isUpdate)
    case ("94.9", isUpdate)  => routes.OtherServicesController.loadOtherMembershipOrgsLvl4Page(isUpdate)
    case ("95", isUpdate)  => routes.OtherServicesController.loadRepairsLvl3Page(isUpdate)
    case ("95.2", isUpdate)  => routes.OtherServicesController.loadHouseholdRepairLvl4Page(isUpdate)
    case ("95.3", isUpdate)  => routes.OtherServicesController.loadMotorVehiclesRepairLvl4Page(isUpdate)
    case ("96", isUpdate)  => routes.OtherServicesController.loadPersonalServicesLvl3Page(isUpdate)
    case ("96.2", isUpdate)  => routes.OtherServicesController.loadHairdressingLvl4Page(isUpdate)
    case ("96.9", isUpdate)  => routes.OtherServicesController.loadOtherPersonalServicesLvl4Page(isUpdate)

    case      => routes.NACECheckDetailsController.getCheckDetails()

  }
}
