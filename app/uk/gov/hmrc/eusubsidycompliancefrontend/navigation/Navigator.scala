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

  def nextPage(previousAnswer: String, mode: String): Call = mode match {
    case "NewRegMode" => normalRoutes(previousAnswer, mode)
  }

  private val normalRoutes: (String, String) => Call = {
    case ("0", mode)     => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage(mode)
    case ("INT00", mode) => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage(mode)
    case ("INT01", mode) => routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage(mode)
    case ("INT02", mode) => routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage(mode)
    case ("INT03", mode) => routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage(mode)
    case ("INT04", mode) => routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage(mode)
    case ("INT05", mode) => routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage(mode)
    case ("INT06", mode) => routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage(mode)
    case ("INT07", mode) => routes.GeneralTradeGroupsController.loadVehiclesTransportPage(mode)

    case ("2", mode) => routes.AgricultureController.loadAgricultureLvl3Page(mode)
    case ("01.1", mode) => routes.AgricultureController.loadNonPerennialCropLvl4Page(mode)
    case ("01.2", mode) => routes.AgricultureController.loadPerennialCropLvl4Page(mode)
    case ("01.4", mode) => routes.AgricultureController.loadAnimalProductionLvl4Page(mode)
    case ("01.6", mode) => routes.AgricultureController.loadSupportActivitiesLvl4Page(mode)
    case ("02", mode) => routes.AgricultureController.loadForestryLvl3Page(mode)
    case ("3", mode) => routes.AgricultureController.loadFishingAndAquacultureLvl3Page(mode)
    case ("03.1", mode) => routes.AgricultureController.loadFishingLvl4Page(mode)
    case ("03.2", mode) => routes.AgricultureController.loadAquacultureLvl4Page(mode)

     case ("B", mode) => routes.MiningController.loadMiningLvl2Page(mode)
     case ("05", mode) => routes.MiningController.loadCoalMiningLvl3Page(mode)
     case ("06", mode) => routes.MiningController.loadGasMiningLvl3Page(mode)
     case ("07", mode) => routes.MiningController.loadMetalMiningLvl3Page(mode)
     case ("07.2", mode) => routes.MiningController.loadNonFeMetalMiningLvl4Page(mode)
     case ("08", mode) => routes.MiningController.loadOtherMiningLvl3Page(mode)
     case ("08.1", mode) => routes.MiningController.loadQuarryingLvl4Page(mode)
     case ("08.9", mode) => routes.MiningController.loadOtherMiningLvl4Page(mode)
     case ("09", mode) => routes.MiningController.loadMiningSupportLvl3Page(mode)

    case ("10", mode) => routes.FoodBeveragesController.loadFoodLvl3Page(mode)
    case ("10.9", mode) => routes.FoodBeveragesController.loadAnimalFeedsLvl4Page(mode)
    case ("10.7", mode) => routes.FoodBeveragesController.loadBakeryAndFarinaceousLvl4Page(mode)
    case ("10.5", mode) => routes.FoodBeveragesController.loadDairyProductsLvl4Page(mode)
    case ("10.3", mode) => routes.FoodBeveragesController.loadFruitAndVegLvl4Page(mode)
    case ("10.6", mode) => routes.FoodBeveragesController.loadGrainAndStarchLvl4Page(mode)
    case ("10.1", mode) => routes.FoodBeveragesController.loadMeatLvl4Page(mode)
    case ("10.4", mode) => routes.FoodBeveragesController.loadOilsAndFatsLvl4Page(mode)
    case ("10.8", mode) => routes.FoodBeveragesController.loadOtherFoodProductsLvl4Page(mode)
    case ("11", mode) => routes.FoodBeveragesController.loadBeveragesLvl4Page(mode)

    case ("13", mode)  => routes.ClothesTextilesHomewareController.loadTextilesLvl3Page(mode)
    case ("13.9", mode)  => routes.ClothesTextilesHomewareController.loadManufactureOfTextilesLvl4Page(mode)
    case ("14", mode)  => routes.ClothesTextilesHomewareController.loadClothingLvl3Page(mode)
    case ("14.2", mode)  => routes.ClothesTextilesHomewareController.loadOtherClothingLvl4Page(mode)
    case ("15", mode)  => routes.ClothesTextilesHomewareController.loadLeatherLvl3Page(mode)
    case ("15.1", mode)  => routes.ClothesTextilesHomewareController.loadTanningDressingDyeingLvl4Page(mode)
    case ("16", mode)  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawLvl3Page(mode)
    case ("16.1", mode)  => routes.ClothesTextilesHomewareController.loadSawmillingWoodworkLvl4Page(mode)
    case ("16.2", mode)  => routes.ClothesTextilesHomewareController.loadWoodCorkStrawPlaitingLvl4Page(mode)
    case ("22", mode)  => routes.ClothesTextilesHomewareController.loadRubberPlasticLvl3Page(mode)
    case ("22.2", mode)  => routes.ClothesTextilesHomewareController.loadPlasticLvl4Page(mode)
    case ("22.1", mode)  => routes.ClothesTextilesHomewareController.loadRubberLvl4Page(mode)

    case ("17.2", mode) => routes.PaperPrintedController.loadArticlesPaperPaperboardLvl4Page(mode)
    case ("17", mode) => routes.PaperPrintedController.loadPaperLvl3Page(mode)
    case ("18", mode) => routes.PaperPrintedController.loadPrintedLvl3Page(mode)
    case ("18.1", mode) => routes.PaperPrintedController.loadPrintingServicesLvl4Page(mode)
    case ("17.1", mode) => routes.PaperPrintedController.loadPulpPaperPaperboardLvl4Page(mode)

    case ("20.1", mode) => routes.MetalsChemicalsController.loadBasicLvl4Page(mode)
    case ("24", mode) => routes.MetalsChemicalsController.loadBasicMetalsLvl3Page(mode)
    case ("24.5", mode) => routes.MetalsChemicalsController.loadCastingMetalsLvl4Page(mode)
    case ("20", mode) => routes.MetalsChemicalsController.loadChemicalsProductsLvl3Page(mode)
    case ("19", mode) => routes.MetalsChemicalsController.loadCokePetroleumLvl3Page(mode)
    case ("25.6", mode) => routes.MetalsChemicalsController.loadCutleryToolsHardwareLvl4Page(mode)
    case ("25", mode) => routes.MetalsChemicalsController.loadFabricatedMetalsLvl3Page(mode)
    case ("24.3", mode) => routes.MetalsChemicalsController.loadFirstProcessingSteelLvl4Page(mode)
    case ("25.9", mode) => routes.MetalsChemicalsController.loadOtherFabricatedProductsLvl4Page(mode)
    case ("20.5", mode) => routes.MetalsChemicalsController.loadOtherProductsLvl4Page(mode)
    case ("21", mode) => routes.MetalsChemicalsController.loadPharmaceuticalsLvl3Page(mode)
    case ("24.4", mode) => routes.MetalsChemicalsController.loadPreciousNonFerrousLvl4Page(mode)
    case ("25.1", mode) => routes.MetalsChemicalsController.loadStructuralMetalLvl4Page(mode)
    case ("25.2", mode) => routes.MetalsChemicalsController.loadTanksReservoirsContainersLvl4Page(mode)
    case ("25.5", mode) => routes.MetalsChemicalsController.loadTreatmentCoatingMachiningLvl4Page(mode)
    case ("20.4", mode) => routes.MetalsChemicalsController.loadWashingLvl4Page(mode)

    case ("23", mode) => routes.NonMetallicOtherController.loadNonMetallicMineralLvl3Page(mode)
    case ("23.1", mode) => routes.NonMetallicOtherController.loadGlassProductsLvl4Page(mode)
    case ("23.3", mode) => routes.NonMetallicOtherController.loadClayBuildingMaterialsLvl4Page(mode)
    case ("23.4", mode) => routes.NonMetallicOtherController.loadOtherPorcelainAndCeramicsLvl4Page(mode)
    case ("23.5", mode) => routes.NonMetallicOtherController.loadCementLimePlasterLvl4Page(mode)
    case ("23.6", mode) => routes.NonMetallicOtherController.loadConcreteCementPlasterLvl4Page(mode)
    case ("23.9", mode) => routes.NonMetallicOtherController.loadAnotherTypeLvl4Page(mode)

    case ("26.1", mode) => routes.ComputersElectronicsController.loadComponentsBoardsLvl4Page(mode)
    case ("26", mode) => routes.ComputersElectronicsController.loadComputersElectronicsOpticalLvl3Page(mode)
    case ("27.5", mode) => routes.ComputersElectronicsController.loadDomesticAppliancesLvl4Page(mode)
    case ("27", mode) => routes.ComputersElectronicsController.loadElectricalEquipmentLvl3Page(mode)
    case ("28.1", mode) => routes.ComputersElectronicsController.loadGeneralPurposeLvl4Page(mode)
    case ("26.5", mode) => routes.ComputersElectronicsController.loadMeasuringTestingInstrumentsLvl4Page(mode)
    case ("28.4", mode) => routes.ComputersElectronicsController.loadMetalFormingLvl4Page(mode)
    case ("27.1", mode) => routes.ComputersElectronicsController.loadMotorsGeneratorsLvl4Page(mode)
    case ("28.2", mode) => routes.ComputersElectronicsController.loadOtherGeneralPurposeLvl4Page(mode)
    case ("28", mode) => routes.ComputersElectronicsController.loadOtherMachineryLvl3Page(mode)
    case ("28.9", mode) => routes.ComputersElectronicsController.loadOtherSpecialPurposeLvl4Page(mode)
    case ("33.1", mode) => routes.ComputersElectronicsController.loadRepairMaintenanceLvl4Page(mode)
    case ("33", mode) => routes.ComputersElectronicsController.loadRepairsMaintainInstallLvl3Page(mode)
    case ("27.3", mode) => routes.ComputersElectronicsController.loadWiringAndDevicesLvl4Page(mode)

    case ("30.3", mode) => routes.VehiclesManuTransportController.loadAircraftSpacecraftLvl4Page(mode)
    case ("29", mode) => routes.VehiclesManuTransportController.loadMotorVehiclesLvl3Page(mode)
    case ("30", mode) => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl3Page(mode)
    case ("30.9", mode) => routes.VehiclesManuTransportController.loadOtherTransportEquipmentLvl4Page(mode)
    case ("29.3", mode) => routes.VehiclesManuTransportController.loadPartsAccessoriesLvl4Page(mode)
    case ("30.1", mode) => routes.VehiclesManuTransportController.loadShipsBoatsLvl4Page(mode)

    case ("32", mode) => routes.NonMetallicOtherController.loadOtherManufacturingLvl3Page(mode)
    case ("32.1", mode) => routes.NonMetallicOtherController.loadJewelleryCoinsLvl4Page(mode)
    case ("32.9", mode) => routes.NonMetallicOtherController.loadOtherProductsLvl4Page(mode)

    case ("D", mode)  => routes.AccomodationUtilitiesController.loadElectricityLvl3Page(mode)
    case ("35.1", mode)  => routes.AccomodationUtilitiesController.loadElectricityLvl4Page(mode)
    case ("35.2", mode)  => routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page(mode)

    case ("38.1", mode)  => routes.AccomodationUtilitiesController.loadWasteCollectionLvl4Page(mode)
    case ("38", mode)  => routes.AccomodationUtilitiesController.loadWasteCollectionRecoveryLvl3Page(mode)
    case ("38.3", mode)  => routes.AccomodationUtilitiesController.loadWasteDisposalLvl4Page(mode)
    case ("38.2", mode)  => routes.AccomodationUtilitiesController.loadWasteRecoveryLvl4Page(mode)
    case ("E", mode)  => routes.AccomodationUtilitiesController.loadWaterLvl2Page(mode)

    case ("F", mode)     => routes.ConstructionController.loadConstructionLvl2Page(mode)
    case ("42", mode)    => routes.ConstructionController.loadCivilEngineeringLvl3Page(mode)
    case ("42.1", mode)  => routes.ConstructionController.loadConstructionRoadsRailwaysLvl4Page(mode)
    case ("42.2", mode)  => routes.ConstructionController.loadConstructionUtilityProjectsLvl4Page(mode)
    case ("42.9", mode)  => routes.ConstructionController.loadOtherCivilEngineeringProjectsLvl4Page(mode)
    case ("43", mode)    => routes.ConstructionController.loadSpecialisedConstructionLvl3Page(mode)
    case ("43.1", mode)  => routes.ConstructionController.loadDemolitionSitePreparationLvl4Page(mode)
    case ("43.2", mode)  => routes.ConstructionController.loadElectricalPlumbingConstructionLvl4Page(mode)
    case ("43.3", mode)  => routes.ConstructionController.loadBuildingCompletionLvl4Page(mode)
    case ("43.4", mode)  => routes.ConstructionController.loadSpecialisedConstructionActivitiesLvl4Page(mode)
    case ("43.9", mode)  => routes.ConstructionController.loadOtherSpecialisedConstructionLvl4Page(mode)

    case ("G", mode)  => routes.RetailWholesaleController.loadRetailWholesaleLvl2Page(mode)
    case ("46", mode)  => routes.RetailWholesaleController.loadWholesaleLvl3Page(mode)
    case ("46.1", mode)  => routes.RetailWholesaleController.loadContractBasisLvl4Page(mode)
    case ("46.2", mode) => routes.RetailWholesaleController.loadAgriculturalLvl4Page(mode)
    case ("46.3", mode)  => routes.RetailWholesaleController.loadFoodWholesaleLvl4Page(mode)
    case ("46.4", mode)  => routes.RetailWholesaleController.loadHouseholdWholesaleLvl4Page(mode)
    case ("46.6", mode)  => routes.RetailWholesaleController.loadMachineryLvl4Page(mode)
    case ("46.7", mode)  => routes.RetailWholesaleController.loadMotorVehiclesWholesaleLvl4Page(mode)
    case ("46.8", mode)  => routes.RetailWholesaleController.loadSpecialisedLvl4Page(mode)
    case ("47", mode)  => routes.RetailWholesaleController.loadRetailLvl3Page(mode)
    case ("47.1", mode)  => routes.RetailWholesaleController.loadNonSpecialisedLvl4Page(mode)
    case ("47.2", mode)  => routes.RetailWholesaleController.loadFoodLvl4Page(mode)
    case ("47.5", mode) => routes.RetailWholesaleController.loadHouseholdLvl4Page(mode)
    case ("47.6", mode)  => routes.RetailWholesaleController.loadCulturalLvl4Page(mode)
    case ("47.7", mode)  => routes.RetailWholesaleController.loadOtherGoodsLvl4Page(mode)
    case ("47.8", mode)  => routes.RetailWholesaleController.loadMotorVehiclesLvl4Page(mode)
    case ("47.9", mode)  => routes.RetailWholesaleController.loadIntermediationLvl4Page(mode)

    case ("H", mode)  => routes.TransportController.loadTransportLvl2Page(mode)
    case ("49", mode)  => routes.TransportController.loadLandTransportLvl3Page(mode)
    case ("49.1", mode)  => routes.TransportController.loadLandTransportPassengerRailLvl4Page(mode)
    case ("49.3", mode)  => routes.TransportController.loadLandTransportOtherPassengerLvl4Page(mode)
    case ("49.4", mode)  => routes.TransportController.loadLandTransportFreightTransportLvl4Page(mode)
    case ("50", mode)  => routes.TransportController.loadWaterTransportLvl3Page(mode)
    case ("51", mode)  => routes.TransportController.loadAirTransportLvl3Page(mode)
    case ("51.2", mode)  => routes.TransportController.loadAirTransportFreightAirLvl4Page(mode)
    case ("52", mode)  => routes.TransportController.loadWarehousingSupportLvl3Page(mode)
    case ("52.2", mode)  => routes.TransportController.loadWarehousingSupportActivitiesTransportLvl4Page(mode)
    case ("52.3", mode)  => routes.TransportController.loadWarehousingIntermediationLvl4Page(mode)
    case ("53", mode)  => routes.TransportController.loadPostalAndCourierLvl3Page(mode)

    case ("I", mode)  => routes.AccomodationUtilitiesController.loadAccommodationFoodLvl2Page(mode)
    case ("55", mode)  => routes.AccomodationUtilitiesController.loadAccommodationLvl3Page(mode)
    case ("56", mode)  => routes.AccomodationUtilitiesController.loadFoodBeverageActivitiesLvl3Page(mode)
    case ("56.1", mode)  => routes.AccomodationUtilitiesController.loadRestaurantFoodServicesLvl4Page(mode)
    case ("56.2", mode)  => routes.AccomodationUtilitiesController.loadEventCateringOtherFoodActivitiesLvl4Page(mode)

    case ("J", mode)  => routes.PublishingTelecomsController.loadPublishingLvl2Page(mode)
    case ("58", mode)  => routes.PublishingTelecomsController.loadPublishingLvl3Page(mode)
    case ("58.1", mode)  => routes.PublishingTelecomsController.loadBookPublishingLvl4Page(mode)
    case ("58.2", mode)  => routes.PublishingTelecomsController.loadSoftwarePublishingLvl4Page(mode)
    case ("59", mode)  => routes.PublishingTelecomsController.loadFilmMusicPublishingLvl3Page(mode)
    case ("59.1", mode)  => routes.PublishingTelecomsController.loadFilmVideoActivitiesLvl4Page(mode)
    case ("60", mode)  => routes.PublishingTelecomsController.loadProgrammingBroadcastingDistributionLvl3Page(mode)
    case ("60.3", mode)  => routes.PublishingTelecomsController.loadNewsOtherContentDistributionLvl4Page(mode)

    case ("K", mode)  => routes.PublishingTelecomsController.loadTelecommunicationLvl2Page(mode)
    case ("61", mode)  => routes.PublishingTelecomsController.loadTelecommunicationLvl3Page(mode)
    case ("62", mode)  => routes.PublishingTelecomsController.loadComputerProgrammingConsultancyLvl3Page(mode)
    case ("63", mode)  => routes.PublishingTelecomsController.loadComputerInfrastructureDataHostingLvl3Page(mode)
    case ("63.9", mode)  => routes.PublishingTelecomsController.loadWebSearchPortalLvl4Page(mode)

    case ("L", mode) => routes.FinanceRealEstateController.loadFinanceInsuranceLvl2Page(mode)
    case ("64", mode) => routes.FinanceRealEstateController.loadFinancialServicesLvl3Page(mode)
    case ("64.1", mode) => routes.FinanceRealEstateController.loadMonetaryIntermediationLvl4Page(mode)
    case ("64.2", mode) => routes.FinanceRealEstateController.loadHoldingCompaniesLvl4Page(mode)
    case ("64.3", mode) => routes.FinanceRealEstateController.loadTrustsFundsLvl4Page(mode)
    case ("64.9", mode) => routes.FinanceRealEstateController.loadOtherFinancialLvl4Page(mode)
    case ("65", mode) => routes.FinanceRealEstateController.loadInsuranceLvl3Page(mode)
    case ("65.1", mode) => routes.FinanceRealEstateController.loadInsuranceTypeLvl4Page(mode)
    case ("66", mode) => routes.FinanceRealEstateController.loadAuxiliaryFinancialLvl3Page(mode)
    case ("66.1", mode) => routes.FinanceRealEstateController.loadAuxiliaryNonInsuranceLvl4Page(mode)
    case ("66.2", mode) => routes.FinanceRealEstateController.loadAuxiliaryInsuranceLvl4Page(mode)

    case ("M", mode) => routes.FinanceRealEstateController.loadRealEstateLvl3Page(mode)
    case ("68.1", mode) => routes.FinanceRealEstateController.loadPropertyDevelopmentLvl4Page(mode)
    case ("68.3", mode) => routes.FinanceRealEstateController.loadFeeContractLvl4Page(mode)

    case ("N", mode) => routes.ProfAndPAdminController.loadProfessionalLvl2Page(mode)
    case ("69", mode) => routes.ProfAndPAdminController.loadLegalAndAccountingLvl3Page(mode)
    case ("70", mode) => routes.ProfAndPAdminController.loadHeadOfficesLvl3Page(mode)
    case ("71", mode) => routes.ProfAndPAdminController.loadArchitecturalLvl3Page(mode)
    case ("71.1", mode) => routes.ProfAndPAdminController.loadArchitecturalLvl4Page(mode)
    case ("72", mode) => routes.ProfAndPAdminController.loadScientificRAndDLvl3Page(mode)
    case ("73", mode) => routes.ProfAndPAdminController.loadAdvertisingLvl3Page(mode)
    case ("73.1", mode) => routes.ProfAndPAdminController.loadAdvertisingLvl4Page(mode)
    case ("74", mode) => routes.ProfAndPAdminController.loadOtherProfessionalLvl3Page(mode)
    case ("74.1", mode) => routes.ProfAndPAdminController.loadSpecialisedDesignLvl4Page(mode)
    case ("74.9", mode) => routes.ProfAndPAdminController.loadOtherProfessionalLvl4Page(mode)

    case ("O", mode) => routes.AdminController.loadAdministrativeLvl2Page(mode)
    case ("77", mode) => routes.AdminController.loadRentalLvl3Page(mode)
    case ("77.1", mode) => routes.AdminController.loadMotorVehiclesLvl4Page(mode)
    case ("77.2", mode) => routes.AdminController.loadPersonalHouseholdLvl4Page(mode)
    case ("77.3", mode) => routes.AdminController.loadMachineryEquipmentLvl4Page(mode)
    case ("77.5", mode) => routes.AdminController.loadIntermediationServicesLvl4Page(mode)
    case ("78", mode) => routes.AdminController.loadEmploymentLvl3Page(mode)
    case ("79", mode) => routes.AdminController.loadTravelLvl3Page(mode)
    case ("79.1", mode) => routes.AdminController.loadTravelAgencyLvl4Page(mode)
    case ("80", mode) => routes.AdminController.loadInvestigationLvl4Page(mode)
    case ("81", mode) => routes.AdminController.loadBuildingsLvl3Page(mode)
    case ("81.2", mode) => routes.AdminController.loadCleaningLvl4Page(mode)
    case ("82", mode) => routes.AdminController.loadOfficeLvl3Page(mode)
    case ("82.9", mode) => routes.AdminController.loadOtherBusinessSupportLvl4Page(mode)

    case ("84", mode) => routes.ProfAndPAdminController.loadPublicAdminDefenceLvl3Page(mode)
    case ("84.1", mode) => routes.ProfAndPAdminController.loadPublicAdminLvl4Page(mode)
    case ("84.2", mode) => routes.ProfAndPAdminController.loadServiceProvisionLvl4Page(mode)

    case ("85", mode)  => routes.HouseHealthEducationController.loadEducationLvl3Page(mode)
    case ("85.3", mode)  => routes.HouseHealthEducationController.loadSecondaryEducationLvl4Page(mode)
    case ("85.5", mode)  => routes.HouseHealthEducationController.loadOtherEducationLvl4Page(mode)
    case ("85.6", mode)  => routes.HouseHealthEducationController.loadEducationalSupportLvl4Page(mode)

    case ("R", mode)  => routes.HouseHealthEducationController.loadHumanHealthLvl2Page(mode)
    case ("86", mode) => routes.HouseHealthEducationController.loadHumanHealthLvl3Page(mode)
    case ("86.2", mode)  => routes.HouseHealthEducationController.loadMedicalDentalLvl4Page(mode)
    case ("86.9", mode)  => routes.HouseHealthEducationController.loadOtherHumanHealthLvl4Page(mode)
    case ("87", mode)  => routes.HouseHealthEducationController.loadResidentialCareLvl3Page(mode)
    case ("87.9", mode)  => routes.HouseHealthEducationController.loadOtherResidentialCareLvl4Page(mode)
    case ("88", mode)  => routes.HouseHealthEducationController.loadSocialWorkLvl3Page(mode)
    case ("88.9", mode)  => routes.HouseHealthEducationController.loadOtherSocialWorkLvl4Page(mode)

    case ("S", mode)  => routes.ArtsController.loadArtsSportsRecreationLvl2Page(mode)
    case ("90", mode)  => routes.ArtsController.loadArtsCreationPerformingLvl3Page(mode)
    case ("90.1", mode)  => routes.ArtsController.loadArtsCreationLvl4Page(mode)
    case ("90.3", mode)  => routes.ArtsController.loadArtsPerformingSupportActivitiesLvl4Page(mode)
    case ("91", mode)  => routes.ArtsController.loadLibrariesArchivesCulturalLvl3Page(mode)
    case ("91.1", mode) => routes.ArtsController.loadLibrariesArchivesLvl4Page(mode)
    case ("91.2", mode)  => routes.ArtsController.loadMuseumsCollectionsMomumentsLvl4Page(mode)
    case ("91.4", mode) => routes.ArtsController.loadBotanicalZoologicalReservesLvl4Page(mode)
    case ("93", mode)  => routes.ArtsController.loadSportsAmusementRecreationLvl3Page(mode)
    case ("93.1", mode) => routes.ArtsController.loadSportsLvl4Page(mode)
    case ("93.2", mode) => routes.ArtsController.loadAmusementAndRecreationLvl4Page(mode)

    case ("U", mode)  => routes.HouseHealthEducationController.loadHouseholdsLvl2Page(mode)
    case ("98", mode)  => routes.HouseHealthEducationController.loadUndifferentiatedProducingActivitiesLvl4Page(mode)

    case ("T", mode)  => routes.OtherServicesController.loadOtherLvl2Page(mode)
    case ("94", mode)  => routes.OtherServicesController.loadMembershipOrgActivitiesLvl3Page(mode)
    case ("94.1", mode)  => routes.OtherServicesController.loadMembershipOrgsLvl4Page(mode)
    case ("94.9", mode)  => routes.OtherServicesController.loadOtherMembershipOrgsLvl4Page(mode)
    case ("95", mode)  => routes.OtherServicesController.loadRepairsLvl3Page(mode)
    case ("95.2", mode)  => routes.OtherServicesController.loadHouseholdRepairLvl4Page(mode)
    case ("95.3", mode)  => routes.OtherServicesController.loadMotorVehiclesRepairLvl4Page(mode)
    case ("96", mode)  => routes.OtherServicesController.loadPersonalServicesLvl3Page(mode)
    case ("96.2", mode)  => routes.OtherServicesController.loadHairdressingLvl4Page(mode)
    case ("96.9", mode)  => routes.OtherServicesController.loadOtherPersonalServicesLvl4Page(mode)

    case (other, mode)       => routes.NACECheckDetailsController.getCheckDetails(other,mode)
  }
}
