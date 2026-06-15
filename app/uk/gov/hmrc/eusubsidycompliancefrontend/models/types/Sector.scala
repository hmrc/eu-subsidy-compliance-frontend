/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.models.types

import play.api.libs.json.*

import scala.util.{Failure, Success, Try}

sealed trait Sector:
  def code: String

  override def toString: String = code

object Sector:

  case object Other extends Sector:
    val code = "0"

  case object Transport extends Sector:
    val code = "1"

  case object Agriculture extends Sector:
    val code = "2"

  case object Aquaculture extends Sector:
    val code = "3"

  case object GeneralTrade extends Sector:
    val code = "00"

  case object OtherGeneralTrade extends Sector:
    val code = "INT00"

  case object ManuGroup1 extends Sector:
    val code = "INT01"

  case object ManuGroup2 extends Sector:
    val code = "INT02"

  case object ManuGroup3 extends Sector:
    val code = "INT03"

  case object ManuGroup4 extends Sector:
    val code = "INT04"

  case object ManuGroup5 extends Sector:
    val code = "INT05"

  case object ManuGroup6 extends Sector:
    val code = "INT06"

  case object ManuGroup7 extends Sector:
    val code = "INT07"

  case object Food extends Sector:
    val code = "10"

  case object Manufacturing extends Sector:
    val code = "C"

  case object Meat extends Sector:
    val code = "10.1"

  case object MeatProcessing extends Sector:
    val code = "10.11"

  case object PoultryProcessing extends Sector:
    val code = "10.12"

  case object MeatProductsProduction extends Sector:
    val code = "10.13"

  case object Fish extends Sector:
    val code = "10.2"

  case object FishProcessing extends Sector:
    val code = "10.20"

  case object FruitAndVegetables extends Sector:
    val code = "10.3"

  case object FruitAndVegetableJuiceManufacture extends Sector:
    val code = "10.32"

  case object FruitAndVegetableProcessing extends Sector:
    val code = "10.31"

  case object OtherFruitAndVegetableProcessing extends Sector:
    val code = "10.39"

  case object Oils extends Sector:
    val code = "10.4"

  case object Margarine extends Sector:
    val code = "10.42"

  case object OtherOils extends Sector:
    val code = "10.41"

  case object DairyProducts extends Sector:
    val code = "10.5"

  case object DairyProducts4 extends Sector:
    val code = "10.51"

  case object IceCream extends Sector:
    val code = "10.52"

  case object GrainAndStarchProducts extends Sector:
    val code = "10.6"

  case object GrainProducts4 extends Sector:
    val code = "10.61"

  case object StarchProducts extends Sector:
    val code = "10.62"

  case object BakeryProducts extends Sector:
    val code = "10.7"

  case object BiscuitsAndPreservedCakes extends Sector:
    val code = "10.72"

  case object BreadAndFreshPastry extends Sector:
    val code = "10.71"

  case object FarinaceousProducts extends Sector:
    val code = "10.73"

  case object OtherFoodProducts extends Sector:
    val code = "10.8"

  case object OtherFoodProduct extends Sector:
    val code = "10.89"

  case object Confectionery extends Sector:
    val code = "10.82"

  case object Condiments extends Sector:
    val code = "10.84"

  case object HomogenisedFoodPreparations extends Sector:
    val code = "10.86"

  case object PreparedMeals extends Sector:
    val code = "10.85"

  case object Sugar extends Sector:
    val code = "10.81"

  case object TeaAndCoffee extends Sector:
    val code = "10.83"

  case object PreparedAnimalFeeds extends Sector:
    val code = "10.9"

  case object FarmAnimalsFood extends Sector:
    val code = "10.91"

  case object PetFood extends Sector:
    val code = "10.92"

  case object AgricultureForestryFishing extends Sector:
    val code = "A"

  case object CropAnimalProduction extends Sector:
    val code = "01"

  case object GrowingNonPerennialCrops extends Sector:
    val code = "01.1"

  case object CerealsLeguminousCrops extends Sector:
    val code = "01.11"

  case object FibreCrops extends Sector:
    val code = "01.16"

  case object Rice extends Sector:
    val code = "01.12"

  case object SugarCane extends Sector:
    val code = "01.14"

  case object Tobacco extends Sector:
    val code = "01.15"

  case object Vegetables extends Sector:
    val code = "01.13"

  case object OtherNonPerennialCrops extends Sector:
    val code = "01.19"

  case object GrowingPerennialCrops extends Sector:
    val code = "01.2"

  case object BeverageCrops extends Sector:
    val code = "01.27"

  case object CitrusFruits extends Sector:
    val code = "01.23"

  case object Grapes extends Sector:
    val code = "01.21"

  case object OleaginousFruits extends Sector:
    val code = "01.26"

  case object StoneFruits extends Sector:
    val code = "01.24"

  case object SpicesPharmaceuticalCrops extends Sector:
    val code = "01.28"

  case object TropicalFruits extends Sector:
    val code = "01.22"

  case object OtherTree extends Sector:
    val code = "01.25"

  case object OtherPerennialCrops extends Sector:
    val code = "01.29"

  case object PlantPropagation extends Sector:
    val code = "01.3"

  case object PlantPropagation4 extends Sector:
    val code = "01.30"

  case object AnimalProduction extends Sector:
    val code = "01.4"

  case object DairyCattle extends Sector:
    val code = "01.41"

  case object OtherCattle extends Sector:
    val code = "01.42"

  case object Camels extends Sector:
    val code = "01.44"

  case object Horses extends Sector:
    val code = "01.43"

  case object Poultry extends Sector:
    val code = "01.47"

  case object Sheep extends Sector:
    val code = "01.45"

  case object Swine extends Sector:
    val code = "01.46"

  case object OtherAnimals extends Sector:
    val code = "01.48"

  case object MixedFarming extends Sector:
    val code = "01.5"

  case object MixedFarming4 extends Sector:
    val code = "01.50"

  case object SupportActivities extends Sector:
    val code = "01.6"

  case object PostHarvestActivities extends Sector:
    val code = "01.63"

  case object SupportActivitiesAnimal extends Sector:
    val code = "01.62"

  case object SupportActivitiesCrop extends Sector:
    val code = "01.61"

  case object HuntingTrapping extends Sector:
    val code = "01.7"

  case object HuntingTrapping4 extends Sector:
    val code = "01.70"

  case object Forestry extends Sector:
    val code = "02"

  case object Silviculture extends Sector:
    val code = "02.1"

  case object Silviculture4 extends Sector:
    val code = "02.10"

  case object Logging extends Sector:
    val code = "02.2"

  case object Logging4 extends Sector:
    val code = "02.20"

  case object GatheringOfWildGrowth extends Sector:
    val code = "02.3"

  case object GatheringOfWildGrowth4 extends Sector:
    val code = "02.30"

  case object ForestrySupportServices extends Sector:
    val code = "02.4"

  case object ForestrySupportServices4 extends Sector:
    val code = "02.40"

  case object FishingAndAquaculture extends Sector:
    val code = "03"

  case object Fishing extends Sector:
    val code = "03.1"

  case object FreshwaterFishing extends Sector:
    val code = "03.12"

  case object MarineFishing extends Sector:
    val code = "03.11"

  case object Aquaculture3 extends Sector:
    val code = "03.2"

  case object FreshwaterAquaculture extends Sector:
    val code = "03.22"

  case object MarineAquaculture extends Sector:
    val code = "03.21"

  case object AquacultureSupportActivities extends Sector:
    val code = "03.3"

  case object AquacultureSupportActivities4 extends Sector:
    val code = "03.30"

  case object MiningQuarrying extends Sector:
    val code = "B"

  case object CoalAndLigniteMining extends Sector:
    val code = "05"

  case object Hardcoal extends Sector:
    val code = "05.1"

  case object HardcoalMining extends Sector:
    val code = "05.10"

  case object Lignite extends Sector:
    val code = "05.2"

  case object LigniteMining extends Sector:
    val code = "05.20"

  case object PetroleumNaturalGasExtraction extends Sector:
    val code = "06"

  case object CrudePetroleum extends Sector:
    val code = "06.1"

  case object CrudePetroleumExtraction extends Sector:
    val code = "06.10"

  case object NaturalGas extends Sector:
    val code = "06.2"

  case object NaturalGasExtraction extends Sector:
    val code = "06.20"

  case object MetalOresMining extends Sector:
    val code = "07"

  case object IronOre extends Sector:
    val code = "07.1"

  case object IronOresMining extends Sector:
    val code = "07.10"

  case object NonFerrousOres extends Sector:
    val code = "07.2"

  case object UraniumThoriumOres extends Sector:
    val code = "07.21"

  case object OtherNonFerrousOres extends Sector:
    val code = "07.29"

  case object OtherMiningQuarrying extends Sector:
    val code = "08"

  case object StoneQuarrying extends Sector:
    val code = "08.1"

  case object GravelPitsOperation extends Sector:
    val code = "08.12"

  case object OrnamentalQuarrying extends Sector:
    val code = "08.11"

  case object MiningAndQuarryingNEC extends Sector:
    val code = "08.9"

  case object PeatExtraction extends Sector:
    val code = "08.92"

  case object SaltExtraction extends Sector:
    val code = "08.93"

  case object ChemicalMineralsMining extends Sector:
    val code = "08.91"

  case object OtherNEC extends Sector:
    val code = "08.99"

  case object MiningSupportServices extends Sector:
    val code = "09"

  case object MiningSupportPetroleumExtraction extends Sector:
    val code = "09.1"

  case object MiningSupportPetroleumExtraction4 extends Sector:
    val code = "09.10"

  case object OtherMiningSupport extends Sector:
    val code = "09.9"

  case object OtherMiningSupport4 extends Sector:
    val code = "09.90"

  case object Beverages extends Sector:
    val code = "11"

  case object BeverageManufacture extends Sector:
    val code = "11.0"

  case object Beer extends Sector:
    val code = "11.05"

  case object Ciders extends Sector:
    val code = "11.03"

  case object Malt extends Sector:
    val code = "11.06"

  case object SoftDrinks extends Sector:
    val code = "11.07"

  case object Spirits extends Sector:
    val code = "11.01"

  case object Wine extends Sector:
    val code = "11.02"

  case object OtherFermentedBeverages extends Sector:
    val code = "11.04"

  case object TobaccoProducts extends Sector:
    val code = "12"

  case object TobaccoProductsManufacture3 extends Sector:
    val code = "12.0"

  case object TobaccoProductsManufacture4 extends Sector:
    val code = "12.00"

  case object Textiles extends Sector:
    val code = "13"

  case object TextilesPreparation extends Sector:
    val code = "13.1"

  case object TextilesPreparation4 extends Sector:
    val code = "13.10"

  case object TextilesWeaving extends Sector:
    val code = "13.2"

  case object TextilesWeaving4 extends Sector:
    val code = "13.20"

  case object TextilesFinishing extends Sector:
    val code = "13.3"

  case object TextilesFinishing4 extends Sector:
    val code = "13.30"

  case object TextilesManufacture extends Sector:
    val code = "13.9"

  case object Carpets extends Sector:
    val code = "13.93"

  case object Cordage extends Sector:
    val code = "13.94"

  case object HouseholdTextiles extends Sector:
    val code = "13.92"

  case object KnittedFabrics extends Sector:
    val code = "13.91"

  case object NonWoven extends Sector:
    val code = "13.95"

  case object OtherTechnicalTextiles extends Sector:
    val code = "13.96"

  case object OtherTextile extends Sector:
    val code = "13.99"

  case object Clothing extends Sector:
    val code = "14"

  case object KnittedCrotchetedClothing extends Sector:
    val code = "14.1"

  case object KnittedClothingManufacture extends Sector:
    val code = "14.10"

  case object OtherClothing extends Sector:
    val code = "14.2"

  case object LeatherAndFurClothing extends Sector:
    val code = "14.24"

  case object Outerwear extends Sector:
    val code = "14.21"

  case object Underwear extends Sector:
    val code = "14.22"

  case object Workwear extends Sector:
    val code = "14.23"

  case object OtherClothing4 extends Sector:
    val code = "14.29"

  case object LeatherProducts extends Sector:
    val code = "15"

  case object LeatherManufacture extends Sector:
    val code = "15.1"

  case object LuggageManufacture extends Sector:
    val code = "15.12"

  case object LeatherTanning extends Sector:
    val code = "15.11"

  case object FootwearManufacture extends Sector:
    val code = "15.2"

  case object FootwearManufacture4 extends Sector:
    val code = "15.20"

  case object WoodProducts extends Sector:
    val code = "16"

  case object WoodProductsProcessing extends Sector:
    val code = "16.1"

  case object ProcessingAndFinishing extends Sector:
    val code = "16.12"

  case object SawmillingAndPlaning extends Sector:
    val code = "16.11"

  case object WoodProductsManufacture extends Sector:
    val code = "16.2"

  case object AssembledParquetFloors extends Sector:
    val code = "16.22"

  case object SolidFuelsVegetableBiomass extends Sector:
    val code = "16.26"

  case object VeneerSheetsPanels extends Sector:
    val code = "16.21"

  case object WoodenContainers extends Sector:
    val code = "16.24"

  case object WoodenWindowsDoors extends Sector:
    val code = "16.25"

  case object OtherBuildersCarpentryJoinery extends Sector:
    val code = "16.23"

  case object OtherWoodProducts extends Sector:
    val code = "16.28"

  case object WoodenProductsFinishing extends Sector:
    val code = "16.27"

  case object PaperRelated extends Sector:
    val code = "17"

  case object Pulp extends Sector:
    val code = "17.1"

  case object Pulp4 extends Sector:
    val code = "17.11"

  case object Paper extends Sector:
    val code = "17.12"

  case object ArticlesPaperBoard extends Sector:
    val code = "17.2"

  case object CorrugatedPaperBoardAndContainers extends Sector:
    val code = "17.21"

  case object SanitaryGoods extends Sector:
    val code = "17.22"

  case object PaperStationery extends Sector:
    val code = "17.23"

  case object Wallpaper extends Sector:
    val code = "17.24"

  case object OtherPaper extends Sector:
    val code = "17.25"

  case object PrintedProducts extends Sector:
    val code = "18"

  case object PrintingService extends Sector:
    val code = "18.1"

  case object BindingServices extends Sector:
    val code = "18.14"

  case object PreMediaServices extends Sector:
    val code = "18.13"

  case object NewspapersPrinting extends Sector:
    val code = "18.11"

  case object OtherPrinting extends Sector:
    val code = "18.12"

  case object RecordedMediaReproduction extends Sector:
    val code = "18.2"

  case object RecordedMediaReproduction4 extends Sector:
    val code = "18.20"

  case object CokeProducts extends Sector:
    val code = "19"

  case object OvenCokeProducts extends Sector:
    val code = "19.1"

  case object CokeProductsManufacture extends Sector:
    val code = "19.10"

  case object FossilFuelProducts extends Sector:
    val code = "19.2"

  case object FossilFuelProductsManufacture extends Sector:
    val code = "19.20"

  case object ChemicalProducts extends Sector:
    val code = "20"

  case object BasicChemicalProducts extends Sector:
    val code = "20.1"

  case object Dyes extends Sector:
    val code = "20.12"

  case object Fertilisers extends Sector:
    val code = "20.15"

  case object IndustrialGases extends Sector:
    val code = "20.11"

  case object PrimaryFormsPlastics extends Sector:
    val code = "20.16"

  case object SyntheticRubber extends Sector:
    val code = "20.17"

  case object OtherInorganicChemicals extends Sector:
    val code = "20.13"

  case object OtherOrganicChemicals extends Sector:
    val code = "20.14"

  case object PesticidesDisinfectants extends Sector:
    val code = "20.2"

  case object PesticidesDisinfectantsManufacture extends Sector:
    val code = "20.20"

  case object PaintsVarnishesCoatings extends Sector:
    val code = "20.3"

  case object PaintsVarnishesCoatingsManufacture extends Sector:
    val code = "20.30"

  case object WashingCleaning extends Sector:
    val code = "20.4"

  case object PerfumesToiletPreparations extends Sector:
    val code = "20.42"

  case object Soap extends Sector:
    val code = "20.41"

  case object OtherChemicalProducts extends Sector:
    val code = "20.5"

  case object LiquidBiofuels extends Sector:
    val code = "20.51"

  case object OtherChemicalProducts4 extends Sector:
    val code = "20.59"

  case object ManMadeFibre extends Sector:
    val code = "20.6"

  case object ManMadeFibreManufacture extends Sector:
    val code = "20.60"

  case object Pharmaceuticals extends Sector:
    val code = "21"

  case object BasicPharmaceuticals extends Sector:
    val code = "21.1"

  case object PharmaceuticalsManufacture extends Sector:
    val code = "21.10"

  case object PharmaceuticalPreparations extends Sector:
    val code = "21.2"

  case object PharmaceuticalPreparationsManufacture extends Sector:
    val code = "21.20"

  case object RubberPlasticProducts extends Sector:
    val code = "22"

  case object RubberProducts extends Sector:
    val code = "22.1"

  case object RubberTubes extends Sector:
    val code = "22.11"

  case object OtherRubberProducts extends Sector:
    val code = "22.12"

  case object PlasticProducts extends Sector:
    val code = "22.2"

  case object BuildersWare extends Sector:
    val code = "22.24"

  case object DoorsAndWindows extends Sector:
    val code = "22.23"

  case object PackingGoods extends Sector:
    val code = "22.22"

  case object PlasticPlatesSheetsTubes extends Sector:
    val code = "22.21"

  case object OtherPlasticProduct extends Sector:
    val code = "22.26"

  case object FinishingPlasticProducts extends Sector:
    val code = "22.25"

  case object OtherNonMetallicProducts extends Sector:
    val code = "23"

  case object GlassProducts extends Sector:
    val code = "23.1"

  case object FlatGlass extends Sector:
    val code = "23.11"

  case object GlassFibres extends Sector:
    val code = "23.14"

  case object HollowGlass extends Sector:
    val code = "23.13"

  case object OtherGlassProducts extends Sector:
    val code = "23.15"

  case object ShapingFlatGlass extends Sector:
    val code = "23.12"

  case object RefractoryProducts extends Sector:
    val code = "23.2"

  case object RefractoryProductsManufacture extends Sector:
    val code = "23.20"

  case object ClayBuildingMaterials extends Sector:
    val code = "23.3"

  case object Bricks extends Sector:
    val code = "23.32"

  case object CeramicTiles extends Sector:
    val code = "23.31"

  case object OtherCeramicProducts extends Sector:
    val code = "23.4"

  case object CeramicHousehold extends Sector:
    val code = "23.41"

  case object CeramicInsulating extends Sector:
    val code = "23.43"

  case object CeramicSanitary extends Sector:
    val code = "23.42"

  case object OtherTechnicalCeramicProducts extends Sector:
    val code = "23.44"

  case object OtherCeramicProducts4 extends Sector:
    val code = "23.45"

  case object CementLimePlaster extends Sector:
    val code = "23.5"

  case object Cement extends Sector:
    val code = "23.51"

  case object PlasterLime extends Sector:
    val code = "23.52"

  case object CementPlasterArticles extends Sector:
    val code = "23.6"

  case object ConcreteProducts extends Sector:
    val code = "23.61"

  case object FibreCement extends Sector:
    val code = "23.65"

  case object Mortars extends Sector:
    val code = "23.64"

  case object PlasterProducts extends Sector:
    val code = "23.62"

  case object ReadyMixedConcrete extends Sector:
    val code = "23.63"

  case object OtherConcreteProducts extends Sector:
    val code = "23.66"

  case object StoneCuttingFinishing extends Sector:
    val code = "23.7"

  case object StoneCuttingFinishing4 extends Sector:
    val code = "23.70"

  case object OtherAbrasiveProducts extends Sector:
    val code = "23.9"

  case object AbrasiveProducts extends Sector:
    val code = "23.91"

  case object OtherNonMetallicMineral extends Sector:
    val code = "23.99"

  case object BasicMetals extends Sector:
    val code = "24"

  case object BasicIronSteel extends Sector:
    val code = "24.1"

  case object BasicIronSteelManufacture extends Sector:
    val code = "24.10"

  case object SteelTubesFittings extends Sector:
    val code = "24.2"

  case object SteelTubesFittingsManufacture extends Sector:
    val code = "24.20"

  case object OtherSteelProductsProcessing extends Sector:
    val code = "24.3"

  case object BarsColdDrawing extends Sector:
    val code = "24.31"

  case object WireColdDrawing extends Sector:
    val code = "24.34"

  case object FoldingColdDrawing extends Sector:
    val code = "24.33"

  case object NarrowStripColdDrawing extends Sector:
    val code = "24.32"

  case object BasicPreciousAndNonFerrousMetals extends Sector:
    val code = "24.4"

  case object NuclearFuelProcessing extends Sector:
    val code = "24.46"

  case object AluminiumProduction extends Sector:
    val code = "24.42"

  case object CopperProduction extends Sector:
    val code = "24.44"

  case object LeadZincTinProduction extends Sector:
    val code = "24.43"

  case object PreciousMetalsProduction extends Sector:
    val code = "24.41"

  case object NonFerrousMetalsProduction extends Sector:
    val code = "24.45"

  case object MetalsCasting extends Sector:
    val code = "24.5"

  case object Iron extends Sector:
    val code = "24.51"

  case object LightMetals extends Sector:
    val code = "24.53"

  case object Steel extends Sector:
    val code = "24.52"

  case object OtherMetals extends Sector:
    val code = "24.54"

  case object FabricatedMetalProducts extends Sector:
    val code = "25"

  case object FabricatedStructuralMetalProducts extends Sector:
    val code = "25.1"

  case object MetalDoors extends Sector:
    val code = "25.12"

  case object MetalStructures extends Sector:
    val code = "25.11"

  case object MetalTanksManufacture extends Sector:
    val code = "25.2"

  case object CentralHeating extends Sector:
    val code = "25.21"

  case object OtherMetalTanks extends Sector:
    val code = "25.22"

  case object WeaponsManufacture extends Sector:
    val code = "25.3"

  case object WeaponsManufacture4 extends Sector:
    val code = "25.30"

  case object MetalForging extends Sector:
    val code = "25.4"

  case object MetalForging4 extends Sector:
    val code = "25.40"

  case object MetalTreatmentCoating extends Sector:
    val code = "25.5"

  case object Coating extends Sector:
    val code = "25.51"

  case object HeatTreatment extends Sector:
    val code = "25.52"

  case object Machining extends Sector:
    val code = "25.53"

  case object CutleryToolsManufacture extends Sector:
    val code = "25.6"

  case object Cutlery extends Sector:
    val code = "25.61"

  case object Locks extends Sector:
    val code = "25.62"

  case object Tools extends Sector:
    val code = "25.63"

  case object OtherFabricatedMetalProductsManufacture extends Sector:
    val code = "25.9"

  case object FastenerProducts extends Sector:
    val code = "25.94"

  case object LightMetalPackaging extends Sector:
    val code = "25.92"

  case object SteelDrums extends Sector:
    val code = "25.91"

  case object WireProducts extends Sector:
    val code = "25.93"

  case object OtherFabricatedMetalProducts extends Sector:
    val code = "25.99"

  case object ComputersProducts extends Sector:
    val code = "26"

  case object ElectronicComponents extends Sector:
    val code = "26.1"

  case object ElectronicComponents4 extends Sector:
    val code = "26.11"

  case object LoadedElectronicBoards extends Sector:
    val code = "26.12"

  case object ComputersPeripheralEquipment extends Sector:
    val code = "26.2"

  case object ComputersPeripheralEquipment4 extends Sector:
    val code = "26.20"

  case object CommunicationEquipment extends Sector:
    val code = "26.3"

  case object CommunicationEquipment4 extends Sector:
    val code = "26.30"

  case object ConsumerElectronics extends Sector:
    val code = "26.4"

  case object ConsumerElectronics4 extends Sector:
    val code = "26.40"

  case object MeasuringToolsAndClocks extends Sector:
    val code = "26.5"

  case object MeasuringInstruments extends Sector:
    val code = "26.51"

  case object WatchesAndClocks extends Sector:
    val code = "26.52"

  case object IrradiationElectromedicalAndElectrotherapeuticEquipment extends Sector:
    val code = "26.6"

  case object IrradiationElectromedicalAndElectrotherapeuticEquipment4 extends Sector:
    val code = "26.60"

  case object OpticalEquipment extends Sector:
    val code = "26.7"

  case object OpticalEquipment4 extends Sector:
    val code = "26.70"

  case object ElectricalEquipment extends Sector:
    val code = "27"

  case object ElectricMotorsGenerators extends Sector:
    val code = "27.1"

  case object ElectricMotors extends Sector:
    val code = "27.11"

  case object ElectricityDistributionAndControl extends Sector:
    val code = "27.12"

  case object Batteries extends Sector:
    val code = "27.2"

  case object BatteriesManufacture extends Sector:
    val code = "27.20"

  case object Wiring extends Sector:
    val code = "27.3"

  case object FibreOpticCables extends Sector:
    val code = "27.31"

  case object OtherElectronicWires extends Sector:
    val code = "27.32"

  case object WiringDevices extends Sector:
    val code = "27.33"

  case object LightingEquipment extends Sector:
    val code = "27.4"

  case object LightingEquipmentManufacture extends Sector:
    val code = "27.40"

  case object DomesticAppliances extends Sector:
    val code = "27.5"

  case object ElectricDomesticAppliances extends Sector:
    val code = "27.51"

  case object NonElectricDomesticAppliances extends Sector:
    val code = "27.52"

  case object OtherElectricalEquipment extends Sector:
    val code = "27.9"

  case object OtherElectricalEquipmentManufacture extends Sector:
    val code = "27.90"

  case object OtherMachineryAndEquipment extends Sector:
    val code = "28"

  case object GeneralPurposeMachinery extends Sector:
    val code = "28.1"

  case object Bearings extends Sector:
    val code = "28.15"

  case object Engines extends Sector:
    val code = "28.11"

  case object FluidPowerEquipment extends Sector:
    val code = "28.12"

  case object OtherPumps extends Sector:
    val code = "28.13"

  case object OtherTaps extends Sector:
    val code = "28.14"

  case object OtherGeneralPurposeMachinery extends Sector:
    val code = "28.2"

  case object LiftingEquipment extends Sector:
    val code = "28.22"

  case object NonDomesticAirConditioning extends Sector:
    val code = "28.25"

  case object OfficeMachinery extends Sector:
    val code = "28.23"

  case object Ovens extends Sector:
    val code = "28.21"

  case object PowerDrivenHandTools extends Sector:
    val code = "28.24"

  case object OtherGeneralPurposeMachinery4 extends Sector:
    val code = "28.29"

  case object AgriculturalMachinery extends Sector:
    val code = "28.3"

  case object AgriculturalMachineryManufacture extends Sector:
    val code = "28.30"

  case object MetalFormingMachinery extends Sector:
    val code = "28.4"

  case object MetalFormingMachinery4 extends Sector:
    val code = "28.41"

  case object OtherMachineTools extends Sector:
    val code = "28.42"

  case object OtherSpecialPurposeMachinery extends Sector:
    val code = "28.9"

  case object AdditiveManufacturingMachinery extends Sector:
    val code = "28.97"

  case object FoodMachinery extends Sector:
    val code = "28.93"

  case object MetallurgyMachinery extends Sector:
    val code = "28.91"

  case object MiningMachinery extends Sector:
    val code = "28.92"

  case object PaperMachinery extends Sector:
    val code = "28.95"

  case object TextileMachinery extends Sector:
    val code = "28.94"

  case object PlasticsMachinery extends Sector:
    val code = "28.96"

  case object OtherSpecialPurposeMachinery4 extends Sector:
    val code = "28.99"

  case object MotorVehicles extends Sector:
    val code = "29"

  case object MotorVehicles3 extends Sector:
    val code = "29.1"

  case object MotorVehiclesManufacture extends Sector:
    val code = "29.10"

  case object MotorVehiclesBodies extends Sector:
    val code = "29.2"

  case object MotorVehiclesBodies4 extends Sector:
    val code = "29.20"

  case object MotorVehiclesParts extends Sector:
    val code = "29.3"

  case object MotorVehiclesElectricalManufacture extends Sector:
    val code = "29.31"

  case object MotorVehiclesOtherManufacture extends Sector:
    val code = "29.32"

  case object OtherTransportEquipment extends Sector:
    val code = "30"

  case object Ships extends Sector:
    val code = "30.1"

  case object CivilianShips extends Sector:
    val code = "30.11"

  case object MilitaryShips extends Sector:
    val code = "30.13"

  case object PleasureShips extends Sector:
    val code = "30.12"

  case object Railway extends Sector:
    val code = "30.2"

  case object RailwayManufacture extends Sector:
    val code = "30.20"

  case object Aircraft extends Sector:
    val code = "30.3"

  case object CivilianAircraftMachinery extends Sector:
    val code = "30.31"

  case object MilitaryAircraftMachinery extends Sector:
    val code = "30.32"

  case object MilitaryFightingVehicles extends Sector:
    val code = "30.4"

  case object MilitaryFightingVehiclesManufacture extends Sector:
    val code = "30.40"

  case object OtherTransportEquipment3 extends Sector:
    val code = "30.9"

  case object Bicycles extends Sector:
    val code = "30.92"

  case object MotorCyclesEquipment extends Sector:
    val code = "30.91"

  case object OtherTransportEquipment4 extends Sector:
    val code = "30.99"

  case object FurnitureManufacture extends Sector:
    val code = "31"

  case object FurnitureManufacture3 extends Sector:
    val code = "31.0"

  case object FurnitureManufacture4 extends Sector:
    val code = "31.00"

  case object OtherManufacturing extends Sector:
    val code = "32"

  case object JewelleryAndCoins extends Sector:
    val code = "32.1"

  case object JewelleryManufacture extends Sector:
    val code = "32.12"

  case object ImitationJewelleryManufacture extends Sector:
    val code = "32.13"

  case object CoinStriking extends Sector:
    val code = "32.11"

  case object MusicalInstruments extends Sector:
    val code = "32.2"

  case object MusicalInstrumentsManufacture extends Sector:
    val code = "32.20"

  case object SportsGoods extends Sector:
    val code = "32.3"

  case object SportsGoodsManufacture extends Sector:
    val code = "32.30"

  case object GamesAndToys extends Sector:
    val code = "32.4"

  case object GamesAndToysManufacture extends Sector:
    val code = "32.40"

  case object MedicalInstruments extends Sector:
    val code = "32.5"

  case object MedicalInstrumentsManufacture extends Sector:
    val code = "32.50"

  case object OtherProducts extends Sector:
    val code = "32.9"

  case object BroomsAndBrushes extends Sector:
    val code = "32.91"

  case object OtherProductManufacture extends Sector:
    val code = "32.99"

  case object MetalProductsRepairMaintenance extends Sector:
    val code = "33"

  case object FabricatedMetalProductsRepair extends Sector:
    val code = "33.1"

  case object CivilianAirRepair extends Sector:
    val code = "33.16"

  case object CivilianShipsRepair extends Sector:
    val code = "33.15"

  case object OtherCivilianTransportEquipmentRepair extends Sector:
    val code = "33.17"

  case object ElectricalEquipmentRepair extends Sector:
    val code = "33.14"

  case object ElectronicEquipmentRepair extends Sector:
    val code = "33.13"

  case object FabricatedMetalProducts4 extends Sector:
    val code = "33.11"

  case object MachineryRepair extends Sector:
    val code = "33.12"

  case object MilitaryVehiclesRepair extends Sector:
    val code = "33.18"

  case object OtherEquipmentRepair extends Sector:
    val code = "33.19"

  case object IndustrialMachineryInstallation extends Sector:
    val code = "33.2"

  case object IndustrialMachineryInstallation4 extends Sector:
    val code = "33.20"

  case object ElectricityGas extends Sector:
    val code = "D"

  case object ElectricityGasAirConditioningSupply extends Sector:
    val code = "35"

  case object ElectricPowerGenerationAndDistribution extends Sector:
    val code = "35.1"

  case object ElectricityDistribution extends Sector:
    val code = "35.14"

  case object NonRenewableElectricityProduction extends Sector:
    val code = "35.11"

  case object RenewableElectricityProduction extends Sector:
    val code = "35.12"

  case object ElectricityStorage extends Sector:
    val code = "35.16"

  case object ElectricityTrade extends Sector:
    val code = "35.15"

  case object ElectricityTransmission extends Sector:
    val code = "35.13"

  case object GaseousFuelsManufacture extends Sector:
    val code = "35.2"

  case object GaseousFuelDistribution extends Sector:
    val code = "35.22"

  case object GaseousFuelManufacture extends Sector:
    val code = "35.21"

  case object GaseousFuelStorage extends Sector:
    val code = "35.24"

  case object GaseousFuelTrade extends Sector:
    val code = "35.23"

  case object SteamAndAirConditioningSupply extends Sector:
    val code = "35.3"

  case object SteamAndAirConditioningSupply4 extends Sector:
    val code = "35.30"

  case object ElectricPowerAndGasBrokers extends Sector:
    val code = "35.4"

  case object ElectricPowerAndGasBrokers4 extends Sector:
    val code = "35.40"

  case object WaterSupply extends Sector:
    val code = "E"

  case object WaterTreatment extends Sector:
    val code = "36"

  case object WaterTreatment3 extends Sector:
    val code = "36.0"

  case object WaterTreatment4 extends Sector:
    val code = "36.00"

  case object Sewerage extends Sector:
    val code = "37"

  case object Sewerage3 extends Sector:
    val code = "37.0"

  case object Sewerage4 extends Sector:
    val code = "37.00"

  case object WasteManagement extends Sector:
    val code = "38"

  case object WasteCollection extends Sector:
    val code = "38.1"

  case object HazardousWaste extends Sector:
    val code = "38.12"

  case object NonHazardousWaste extends Sector:
    val code = "38.11"

  case object WasteRecovery extends Sector:
    val code = "38.2"

  case object EnergyRecovery extends Sector:
    val code = "38.22"

  case object MaterialsRecovery extends Sector:
    val code = "38.21"

  case object OtherWasteRecovery extends Sector:
    val code = "38.23"

  case object WasteDisposal extends Sector:
    val code = "38.3"

  case object Incineration extends Sector:
    val code = "38.31"

  case object LandFilling extends Sector:
    val code = "38.32"

  case object OtherWasteDisposal extends Sector:
    val code = "38.33"

  case object WasteManagementRemediation extends Sector:
    val code = "39"

  case object WasteManagementRemediation3 extends Sector:
    val code = "39.0"

  case object WasteManagementRemediation4 extends Sector:
    val code = "39.00"

  case object Construction extends Sector:
    val code = "F"

  case object ResidentialNonResidentialConstruction extends Sector:
    val code = "41"

  case object ResidentialNonResidentialConstruction3 extends Sector:
    val code = "41.0"

  case object ResidentialNonResidentialConstruction4 extends Sector:
    val code = "41.00"

  case object CivilEngineering extends Sector:
    val code = "42"

  case object RoadAndRailConstruction extends Sector:
    val code = "42.1"

  case object BridgesTunnels extends Sector:
    val code = "42.13"

  case object RailwaysUnderground extends Sector:
    val code = "42.12"

  case object RoadsMotorways extends Sector:
    val code = "42.11"

  case object UtilityConstruction extends Sector:
    val code = "42.2"

  case object ElectricityUtilityConstruction extends Sector:
    val code = "42.22"

  case object FluidsUtilityConstruction extends Sector:
    val code = "42.21"

  case object OtherUtilityConstructionProjects extends Sector:
    val code = "42.9"

  case object WaterProjects extends Sector:
    val code = "42.91"

  case object OtherCivilEngineering extends Sector:
    val code = "42.99"

  case object SpecialisedConstructionActivities extends Sector:
    val code = "43"

  case object DemolitionAndPreparation extends Sector:
    val code = "43.1"

  case object Demolition extends Sector:
    val code = "43.11"

  case object SitePreparation extends Sector:
    val code = "43.12"

  case object TestDrillingBoring extends Sector:
    val code = "43.13"

  case object ElectricalAndPlumbingInstallation extends Sector:
    val code = "43.2"

  case object ElectricalInstallation extends Sector:
    val code = "43.21"

  case object InsulationInstallation extends Sector:
    val code = "43.23"

  case object PlumbingHeatingAC extends Sector:
    val code = "43.22"

  case object OtherConstructionInstallation extends Sector:
    val code = "43.24"

  case object BuildingFinishing extends Sector:
    val code = "43.3"

  case object FloorAndWallCovering extends Sector:
    val code = "43.33"

  case object JoineryInstallation extends Sector:
    val code = "43.32"

  case object PaintingAndGlazing extends Sector:
    val code = "43.34"

  case object Plastering extends Sector:
    val code = "43.31"

  case object OtherBuildingFinishing extends Sector:
    val code = "43.35"

  case object SpecialisedBuildingActivities extends Sector:
    val code = "43.4"

  case object RoofingActivities extends Sector:
    val code = "43.41"

  case object OtherSpecialisedBuildingActivities extends Sector:
    val code = "43.42"

  case object SpecialisedCivilEngineering extends Sector:
    val code = "43.5"

  case object SpecialisedCivilEngineering4 extends Sector:
    val code = "43.50"

  case object SpecialisedConstructionIntermediationServices extends Sector:
    val code = "43.6"

  case object SpecialisedConstructionIntermediationServices4 extends Sector:
    val code = "43.60"

  case object OtherSpecialisedConstruction extends Sector:
    val code = "43.9"

  case object MasonryAndBricklaying extends Sector:
    val code = "43.91"

  case object OtherSpecialisedConstructionActivities extends Sector:
    val code = "43.99"

  case object WholesaleAndRetailTrade extends Sector:
    val code = "G"

  case object Wholesale extends Sector:
    val code = "46"

  case object WholesaleContractBasis extends Sector:
    val code = "46.1"

  case object AgriculturalWholesaleContractBasis extends Sector:
    val code = "46.11"

  case object FoodWholesaleContractBasis extends Sector:
    val code = "46.17"

  case object FuelsWholesaleContractBasis extends Sector:
    val code = "46.12"

  case object FurnitureWholesaleContractBasis extends Sector:
    val code = "46.15"

  case object MachineryWholesaleContractBasis extends Sector:
    val code = "46.14"

  case object TextilesWholesaleContractBasis extends Sector:
    val code = "46.16"

  case object TimberWholesaleContractBasis extends Sector:
    val code = "46.13"

  case object OtherWholesaleContractBasis extends Sector:
    val code = "46.18"

  case object NonSpecialisedWholesaleContractBasis extends Sector:
    val code = "46.19"

  case object AgriculturalWholesale extends Sector:
    val code = "46.2"

  case object FlowersAndPlants extends Sector:
    val code = "46.22"

  case object Grain extends Sector:
    val code = "46.21"

  case object LeatherWholesale extends Sector:
    val code = "46.24"

  case object LiveAnimals extends Sector:
    val code = "46.23"

  case object FoodWholesale extends Sector:
    val code = "46.3"

  case object BeveragesWholesale extends Sector:
    val code = "46.34"

  case object CoffeeAndSpicesWholesale extends Sector:
    val code = "46.37"

  case object DairyProductsWholesale extends Sector:
    val code = "46.33"

  case object FruitWholesale extends Sector:
    val code = "46.31"

  case object MeatWholesale extends Sector:
    val code = "46.32"

  case object SugarWholesale extends Sector:
    val code = "46.36"

  case object TobaccoProductsWholesale extends Sector:
    val code = "46.35"

  case object OtherFoodWholesale extends Sector:
    val code = "46.38"

  case object NonSpecialisedFoodWholesale extends Sector:
    val code = "46.39"

  case object WholesaleHouseholdGoods extends Sector:
    val code = "46.4"

  case object ChinaWholesale extends Sector:
    val code = "46.44"

  case object ClothingWholesale extends Sector:
    val code = "46.42"

  case object ElectricalHouseholdWholesale extends Sector:
    val code = "46.43"

  case object HouseholdWholesale extends Sector:
    val code = "46.47"

  case object PerfumeWholesale extends Sector:
    val code = "46.45"

  case object PharmaceuticalWholesale extends Sector:
    val code = "46.46"

  case object TextilesWholesale extends Sector:
    val code = "46.41"

  case object WatchesWholesale extends Sector:
    val code = "46.48"

  case object OtherHouseholdGoodsWholesale extends Sector:
    val code = "46.49"

  case object InformationEquipmentWholesale extends Sector:
    val code = "46.5"

  case object InformationEquipmentWholesale4 extends Sector:
    val code = "46.50"

  case object MachineryWholesale extends Sector:
    val code = "46.6"

  case object AgriculturalMachineryWholesale extends Sector:
    val code = "46.61"

  case object MachineToolsWholesale extends Sector:
    val code = "46.62"

  case object MiningMachineryWholesale extends Sector:
    val code = "46.63"

  case object OtherMachineryWholesale extends Sector:
    val code = "46.64"

  case object MotorVehiclesWholesale extends Sector:
    val code = "46.7"

  case object MotorVehicleWholesale extends Sector:
    val code = "46.71"

  case object MotorVehiclePartsWholesale extends Sector:
    val code = "46.72"

  case object MotorcyclePartsWholesale extends Sector:
    val code = "46.73"

  case object OtherSpecialisedWholesale extends Sector:
    val code = "46.8"

  case object ChemicalProductsWholesale extends Sector:
    val code = "46.85"

  case object HardwareEquipment extends Sector:
    val code = "46.84"

  case object Metals extends Sector:
    val code = "46.82"

  case object Fuels extends Sector:
    val code = "46.81"

  case object Waste extends Sector:
    val code = "46.87"

  case object Wood extends Sector:
    val code = "46.83"

  case object OtherIntermediateProducts extends Sector:
    val code = "46.86"

  case object OtherSpecialisedWholesale4 extends Sector:
    val code = "46.89"

  case object NonSpecialisedWholesale extends Sector:
    val code = "46.9"

  case object NonSpecialisedWholesaleTrade extends Sector:
    val code = "46.90"

  case object Retail extends Sector:
    val code = "47"

  case object NonSpecialisedRetail extends Sector:
    val code = "47.1"

  case object NonSpecialisedFoodRetail extends Sector:
    val code = "47.11"

  case object OtherNonSpecialisedRetail extends Sector:
    val code = "47.12"

  case object FoodRetail extends Sector:
    val code = "47.2"

  case object BeveragesRetail extends Sector:
    val code = "47.25"

  case object BreadRetail extends Sector:
    val code = "47.24"

  case object FishRetail extends Sector:
    val code = "47.23"

  case object FruitRetail extends Sector:
    val code = "47.21"

  case object MeatRetail extends Sector:
    val code = "47.22"

  case object TobaccoRetail extends Sector:
    val code = "47.26"

  case object OtherFoodRetail extends Sector:
    val code = "47.27"

  case object AutomotiveFuelRetail extends Sector:
    val code = "47.3"

  case object AutomotiveFuelRetail4 extends Sector:
    val code = "47.30"

  case object CommunicationEquipmentRetail extends Sector:
    val code = "47.4"

  case object CommunicationEquipmentRetail4 extends Sector:
    val code = "47.40"

  case object OtherHouseholdEquipmentRetail extends Sector:
    val code = "47.5"

  case object CarpetsRetail extends Sector:
    val code = "47.53"

  case object ElectricalRetail extends Sector:
    val code = "47.54"

  case object HardwareRetail extends Sector:
    val code = "47.52"

  case object TextilesRetail extends Sector:
    val code = "47.51"

  case object FurnitureRetail extends Sector:
    val code = "47.55"

  case object CulturalRetail extends Sector:
    val code = "47.6"

  case object BooksRetail extends Sector:
    val code = "47.61"

  case object GamesRetail extends Sector:
    val code = "47.64"

  case object NewspapersRetail extends Sector:
    val code = "47.62"

  case object SportingEquipmentRetail extends Sector:
    val code = "47.63"

  case object OtherCulturalAndRecreationalGoods extends Sector:
    val code = "47.69"

  case object OtherGoodsRetail extends Sector:
    val code = "47.7"

  case object ClothingRetail extends Sector:
    val code = "47.71"

  case object CosmeticRetail extends Sector:
    val code = "47.75"

  case object FlowersRetail extends Sector:
    val code = "47.76"

  case object FootwearRetail extends Sector:
    val code = "47.72"

  case object MedicalRetail extends Sector:
    val code = "47.74"

  case object PharmaceuticalRetail extends Sector:
    val code = "47.73"

  case object WatchesRetail extends Sector:
    val code = "47.77"

  case object OtherNewGoodsRetail extends Sector:
    val code = "47.78"

  case object SecondHandGoods extends Sector:
    val code = "47.79"

  case object MotorVehiclesAndMotorcyclesRetail extends Sector:
    val code = "47.8"

  case object MotorVehiclesRetail extends Sector:
    val code = "47.81"

  case object MotorVehiclesPartsRetail extends Sector:
    val code = "47.82"

  case object MotorCyclesRetail extends Sector:
    val code = "47.83"

  case object RetailIntermediationServices extends Sector:
    val code = "47.9"

  case object NonSpecialisedRetailIntermediation extends Sector:
    val code = "47.91"

  case object SpecialisedRetailIntermediation extends Sector:
    val code = "47.92"

  case object TransportStorage extends Sector:
    val code = "H"

  case object LandTransport extends Sector:
    val code = "49"

  case object PassengerRailTransport extends Sector:
    val code = "49.1"

  case object PassengerHeavyRailTransport extends Sector:
    val code = "49.11"

  case object OtherPassengerRailTransport extends Sector:
    val code = "49.12"

  case object FreightRailTransport extends Sector:
    val code = "49.2"

  case object FreightRailTransport4 extends Sector:
    val code = "49.20"

  case object OtherPassengerLandTransport extends Sector:
    val code = "49.3"

  case object NonScheduledPassengerRoadTransport extends Sector:
    val code = "49.32"

  case object OnDemandPassengerRoadTransport extends Sector:
    val code = "49.33"

  case object CablewaysPassengerTransport extends Sector:
    val code = "49.34"

  case object ScheduledPassengerRoadTransport extends Sector:
    val code = "49.31"

  case object OtherLandTransport extends Sector:
    val code = "49.39"

  case object FreightRoadTransport extends Sector:
    val code = "49.4"

  case object FreightRoadTransport4 extends Sector:
    val code = "49.41"

  case object RemovalServices extends Sector:
    val code = "49.42"

  case object PipelineTransport extends Sector:
    val code = "49.5"

  case object PipelineTransport4 extends Sector:
    val code = "49.50"

  case object WaterTransport extends Sector:
    val code = "50"

  case object CoastalPassengerWaterTransport extends Sector:
    val code = "50.1"

  case object CoastalPassengerWaterTransport4 extends Sector:
    val code = "50.10"

  case object CoastalFreightWaterTransport extends Sector:
    val code = "50.2"

  case object CoastalFreightWaterTransport4 extends Sector:
    val code = "50.20"

  case object InlandPassengerWaterTransport extends Sector:
    val code = "50.3"

  case object InlandPassengerWaterTransport4 extends Sector:
    val code = "50.30"

  case object InlandFreightWaterTransport extends Sector:
    val code = "50.4"

  case object InlandFreightWaterTransport4 extends Sector:
    val code = "50.40"

  case object AirTransport extends Sector:
    val code = "51"

  case object PassengerAirTransport extends Sector:
    val code = "51.1"

  case object PassengerAirTransport4 extends Sector:
    val code = "51.10"

  case object FreightAndSpaceAirTransport extends Sector:
    val code = "51.2"

  case object FreightAirTransport extends Sector:
    val code = "51.21"

  case object SpaceTransport extends Sector:
    val code = "51.22"

  case object WarehousingStorageTransportSupportActivities extends Sector:
    val code = "52"

  case object WarehousingStorage extends Sector:
    val code = "52.1"

  case object WarehousingStorage4 extends Sector:
    val code = "52.10"

  case object TransportSupportActivities extends Sector:
    val code = "52.2"

  case object CargoHandling extends Sector:
    val code = "52.24"

  case object LogisticsServiceActivities extends Sector:
    val code = "52.25"

  case object AirTransportServiceActivities extends Sector:
    val code = "52.23"

  case object LandTransportServiceActivities extends Sector:
    val code = "52.21"

  case object WaterTransportServiceActivities extends Sector:
    val code = "52.22"

  case object OtherTransportSupportActivities extends Sector:
    val code = "52.26"

  case object TransportIntermediationServiceActivities extends Sector:
    val code = "52.3"

  case object FreightTransportIntermediationServiceActivities extends Sector:
    val code = "52.31"

  case object PassengerTransportIntermediationServiceActivities extends Sector:
    val code = "52.32"

  case object PostalAndCourierActivities extends Sector:
    val code = "53"

  case object PostalActivities extends Sector:
    val code = "53.1"

  case object PostalActivities4 extends Sector:
    val code = "53.10"

  case object OtherPostalActivities extends Sector:
    val code = "53.2"

  case object OtherPostalActivities4 extends Sector:
    val code = "53.20"

  case object PostalIntermediationActivities extends Sector:
    val code = "53.3"

  case object PostalIntermediationActivities4 extends Sector:
    val code = "53.30"

  case object AccommodationAndFoodService extends Sector:
    val code = "I"

  case object Accommodation extends Sector:
    val code = "55"

  case object HotelsAccommodation extends Sector:
    val code = "55.1"

  case object HotelsAccommodation4 extends Sector:
    val code = "55.10"

  case object HolidayShortStay extends Sector:
    val code = "55.2"

  case object HolidayShortStay4 extends Sector:
    val code = "55.20"

  case object CampingGrounds extends Sector:
    val code = "55.3"

  case object CampingGrounds4 extends Sector:
    val code = "55.30"

  case object IntermediationAccommodation extends Sector:
    val code = "55.4"

  case object IntermediationAccommodation4 extends Sector:
    val code = "55.40"

  case object OtherAccommodation extends Sector:
    val code = "55.9"

  case object OtherAccommodation4 extends Sector:
    val code = "55.90"

  case object FoodBeverageServiceActivities extends Sector:
    val code = "56"

  case object RestaurantsMobile extends Sector:
    val code = "56.1"

  case object RestaurantsMobile4 extends Sector:
    val code = "56.11"

  case object MobileFoodServices extends Sector:
    val code = "56.12"

  case object EventAndContractCatering extends Sector:
    val code = "56.2"

  case object EventCatering extends Sector:
    val code = "56.21"

  case object ContractCatering extends Sector:
    val code = "56.22"

  case object BeverageServing extends Sector:
    val code = "56.3"

  case object BeverageServing4 extends Sector:
    val code = "56.30"

  case object FoodBeverageServiceIntermediationActivities extends Sector:
    val code = "56.4"

  case object FoodBeverageServiceIntermediationActivities4 extends Sector:
    val code = "56.40"

  case object PublishingBroadcasting extends Sector:
    val code = "J"

  case object Publishing extends Sector:
    val code = "58"

  case object PublishingBooksNewspapers extends Sector:
    val code = "58.1"

  case object Books extends Sector:
    val code = "58.11"

  case object JournalsPeriodicals extends Sector:
    val code = "58.13"

  case object Newspapers extends Sector:
    val code = "58.12"

  case object OtherPublishing extends Sector:
    val code = "58.19"

  case object SoftwarePublishing extends Sector:
    val code = "58.2"

  case object VideoGames extends Sector:
    val code = "58.21"

  case object OtherSoftware extends Sector:
    val code = "58.29"

  case object FilmVideoSoundPublishing extends Sector:
    val code = "59"

  case object FilmVideoActivities extends Sector:
    val code = "59.1"

  case object VideoProductionDistribution extends Sector:
    val code = "59.13"

  case object VideoPostProduction extends Sector:
    val code = "59.12"

  case object VideoProduction extends Sector:
    val code = "59.11"

  case object Projection extends Sector:
    val code = "59.14"

  case object SoundRecordingAndMusicPublishing extends Sector:
    val code = "59.2"

  case object SoundRecordingAndMusicPublishingActivities extends Sector:
    val code = "59.20"

  case object ProgrammingBroadcastingNewsActivities extends Sector:
    val code = "60"

  case object RadioBroadcasting extends Sector:
    val code = "60.1"

  case object RadioBroadcasting4 extends Sector:
    val code = "60.10"

  case object ProgrammingBroadcastingVideoDistribution extends Sector:
    val code = "60.2"

  case object ProgrammingBroadcastingVideoDistribution4 extends Sector:
    val code = "60.20"

  case object NewsAgency extends Sector:
    val code = "60.3"

  case object NewsAgencyActivities extends Sector:
    val code = "60.31"

  case object OtherContentDistributionActivities extends Sector:
    val code = "60.39"

  case object Telecommunications extends Sector:
    val code = "K"

  case object Telecommunication extends Sector:
    val code = "61"

  case object WiredTelecommunication extends Sector:
    val code = "61.1"

  case object WiredTelecommunication4 extends Sector:
    val code = "61.10"

  case object ResellingTelecommunication extends Sector:
    val code = "61.2"

  case object ResellingTelecommunication4 extends Sector:
    val code = "61.20"

  case object OtherTelecommunications extends Sector:
    val code = "61.9"

  case object OtherTelecommunications4 extends Sector:
    val code = "61.90"

  case object ComputerProgrammingConsultancy extends Sector:
    val code = "62"

  case object ComputerProgrammingActivities extends Sector:
    val code = "62.1"

  case object ComputerProgrammingActivities4 extends Sector:
    val code = "62.10"

  case object ComputerFacilitiesConsultancy extends Sector:
    val code = "62.2"

  case object ComputerFacilitiesConsultancy4 extends Sector:
    val code = "62.20"

  case object OtherComputerServiceActivities extends Sector:
    val code = "62.9"

  case object OtherComputerServiceActivities4 extends Sector:
    val code = "62.90"

  case object ComputingInfrastructureActivities extends Sector:
    val code = "63"

  case object ComputingInfrastructureActivities3 extends Sector:
    val code = "63.1"

  case object ComputingInfrastructureActivities4 extends Sector:
    val code = "63.10"

  case object WebSearchPortal extends Sector:
    val code = "63.9"

  case object WebSearchPortalActivities extends Sector:
    val code = "63.91"

  case object OtherInformationServices extends Sector:
    val code = "63.92"

  case object FinancialInsuranceActivities extends Sector:
    val code = "L"

  case object FinancialServices extends Sector:
    val code = "64"

  case object MonetaryIntermediation extends Sector:
    val code = "64.1"

  case object CentralBanking extends Sector:
    val code = "64.11"

  case object OtherMonetary extends Sector:
    val code = "64.19"

  case object HoldingCompaniesFinal extends Sector:
    val code = "64.21"

  case object FinancingConduits extends Sector:
    val code = "64.22"

  case object HoldingCompanies extends Sector:
    val code = "64.2"

  case object TrustsFunds extends Sector:
    val code = "64.3"

  case object InvestmentFunds extends Sector:
    val code = "64.31"

  case object TrustEstate extends Sector:
    val code = "64.32"

  case object OtherFinancial extends Sector:
    val code = "64.9"

  case object FinancialLeasing extends Sector:
    val code = "64.91"

  case object OtherCredit extends Sector:
    val code = "64.92"

  case object OtherFinancialServices extends Sector:
    val code = "64.99"

  case object InsuranceReinsurancePensionFunding extends Sector:
    val code = "65"

  case object InsuranceServices extends Sector:
    val code = "65.1"

  case object LifeInsurance extends Sector:
    val code = "65.11"

  case object NonLifeInsurance extends Sector:
    val code = "65.12"

  case object ReinsuranceServices extends Sector:
    val code = "65.2"

  case object Reinsurance4 extends Sector:
    val code = "65.20"

  case object PensionFundingServices extends Sector:
    val code = "65.3"

  case object PensionFunding4 extends Sector:
    val code = "65.30"

  case object AuxiliaryActivities extends Sector:
    val code = "66"

  case object AuxiliaryFinancialServices extends Sector:
    val code = "66.1"

  case object FinancialMarkets extends Sector:
    val code = "66.11"

  case object SecurityBrokerage extends Sector:
    val code = "66.12"

  case object OtherAuxiliaryFinancial extends Sector:
    val code = "66.19"

  case object AuxiliaryInsurance extends Sector:
    val code = "66.2"

  case object InsuranceAgents extends Sector:
    val code = "66.22"

  case object RiskEvaluation extends Sector:
    val code = "66.21"

  case object OtherAuxiliaryInsurance extends Sector:
    val code = "66.29"

  case object FundManagementServices extends Sector:
    val code = "66.3"

  case object FundManagementActivities extends Sector:
    val code = "66.30"

  case object RealEstate extends Sector:
    val code = "M"

  case object RealEstateActivities extends Sector:
    val code = "68"

  case object PropertyDevelopment extends Sector:
    val code = "68.1"

  case object BuyingSelling extends Sector:
    val code = "68.11"

  case object DevelopmentProjects extends Sector:
    val code = "68.12"

  case object RentalOperating extends Sector:
    val code = "68.2"

  case object RealEstateRental4 extends Sector:
    val code = "68.20"

  case object FeeContract extends Sector:
    val code = "68.3"

  case object Intermediation extends Sector:
    val code = "68.31"

  case object OtherFeeContract extends Sector:
    val code = "68.32"

  case object ProfessionalScientificAndTechnicalActivities extends Sector:
    val code = "N"

  case object LegalAndAccounting extends Sector:
    val code = "69"

  case object Legal extends Sector:
    val code = "69.1"

  case object LegalActivities4 extends Sector:
    val code = "69.10"

  case object Accounting extends Sector:
    val code = "69.2"

  case object AccountingTaxConsultancy extends Sector:
    val code = "69.20"

  case object HeadOfficesAndManagementConsultancy extends Sector:
    val code = "70"

  case object HeadOffice extends Sector:
    val code = "70.1"

  case object HeadOfficesActivities4 extends Sector:
    val code = "70.10"

  case object ManagementConsultancy extends Sector:
    val code = "70.2"

  case object ManagementConsultancyActivities4 extends Sector:
    val code = "70.20"

  case object ArchitecturalAndTechnical extends Sector:
    val code = "71"

  case object ArchitecturalAndTechnicalActivities extends Sector:
    val code = "71.1"

  case object ArchitecturalEngineering extends Sector:
    val code = "71.11"

  case object EngineeringConsultancy extends Sector:
    val code = "71.12"

  case object TechnicalTesting extends Sector:
    val code = "71.2"

  case object TechnicalTesting4 extends Sector:
    val code = "71.20"

  case object ScientificResearchAndDevelopment extends Sector:
    val code = "72"

  case object NaturalScientificResearchAndDevelopment extends Sector:
    val code = "72.1"

  case object NaturalScientificResearchAndDevelopment4 extends Sector:
    val code = "72.10"

  case object SocialScientificResearchAndDevelopment extends Sector:
    val code = "72.2"

  case object SocialScientificResearchAndDevelopment4 extends Sector:
    val code = "72.20"

  case object AdvertisingMarketResearch extends Sector:
    val code = "73"

  case object Advertising extends Sector:
    val code = "73.1"

  case object AdvertisingActivities extends Sector:
    val code = "73.11"

  case object MediaRepresentation extends Sector:
    val code = "73.12"

  case object MarketResearch extends Sector:
    val code = "73.2"

  case object MarketResearch4 extends Sector:
    val code = "73.20"

  case object PublicRelations extends Sector:
    val code = "73.3"

  case object PublicRelations4 extends Sector:
    val code = "73.30"

  case object OtherProfessionalScientific extends Sector:
    val code = "74"

  case object SpecialisedDesign extends Sector:
    val code = "74.1"

  case object GraphicDesign extends Sector:
    val code = "74.12"

  case object IndustrialFashionDesign extends Sector:
    val code = "74.11"

  case object InteriorDesign extends Sector:
    val code = "74.13"

  case object OtherDesign extends Sector:
    val code = "74.14"

  case object Photography extends Sector:
    val code = "74.2"

  case object PhotographicActivities extends Sector:
    val code = "74.20"

  case object TranslationInterpretation extends Sector:
    val code = "74.3"

  case object TranslationActivities4 extends Sector:
    val code = "74.30"

  case object OtherProfessionalScientificActivities extends Sector:
    val code = "74.9"

  case object PatentBrokering extends Sector:
    val code = "74.91"

  case object OtherProfessionalScientificActivities4 extends Sector:
    val code = "74.99"

  case object Veterinary extends Sector:
    val code = "75"

  case object VeterinaryActivities3 extends Sector:
    val code = "75.0"

  case object VeterinaryActivities4 extends Sector:
    val code = "75.00"

  case object Administration extends Sector:
    val code = "O"

  case object RentalAndLeasing extends Sector:
    val code = "77"

  case object MotorVehiclesRental extends Sector:
    val code = "77.1"

  case object CarsLightVehicles extends Sector:
    val code = "77.11"

  case object Trucks extends Sector:
    val code = "77.12"

  case object PersonalRental extends Sector:
    val code = "77.2"

  case object RecreationalSportsGoods extends Sector:
    val code = "77.21"

  case object OtherPersonalHouseholdGoods extends Sector:
    val code = "77.22"

  case object OtherMachineryRental extends Sector:
    val code = "77.3"

  case object AgriculturalMachineryRental extends Sector:
    val code = "77.31"

  case object AirTransportRental extends Sector:
    val code = "77.35"

  case object ConstructionMachineryRental extends Sector:
    val code = "77.32"

  case object OfficeMachineryRental extends Sector:
    val code = "77.33"

  case object WaterTransportRental extends Sector:
    val code = "77.34"

  case object OtherMachineryRental4 extends Sector:
    val code = "77.39"

  case object IntellectualProperty extends Sector:
    val code = "77.4"

  case object LeasingIntellectualProperty extends Sector:
    val code = "77.40"

  case object TangibleGoodsRentalIntermediation extends Sector:
    val code = "77.5"

  case object CarsMotorhomesTrailers extends Sector:
    val code = "77.51"

  case object OtherTangibleGoodsRental extends Sector:
    val code = "77.52"

  case object Employment extends Sector:
    val code = "78"

  case object EmploymentPlacementActivities extends Sector:
    val code = "78.1"

  case object EmploymentPlacementActivities4 extends Sector:
    val code = "78.10"

  case object TempEmploymentPlacementActivities extends Sector:
    val code = "78.2"

  case object TempEmploymentPlacementActivities4 extends Sector:
    val code = "78.20"

  case object TravelAgencyAndReservation extends Sector:
    val code = "79"

  case object TravelAgency extends Sector:
    val code = "79.1"

  case object TourOperator extends Sector:
    val code = "79.12"

  case object TravelAgencyActivities extends Sector:
    val code = "79.11"

  case object OtherReservationServices extends Sector:
    val code = "79.9"

  case object OtherReservationServices4 extends Sector:
    val code = "79.90"

  case object InvestigationAndSecurity extends Sector:
    val code = "80"

  case object InvestigationAndSecurityActivities extends Sector:
    val code = "80.0"

  case object PrivateInvestigationAndSecurityActivities extends Sector:
    val code = "80.01"

  case object OtherInvestigationAndSecurityActivities extends Sector:
    val code = "80.09"

  case object BuildingsAndLandscapingServices extends Sector:
    val code = "81"

  case object CombinedFacilitiesSupport extends Sector:
    val code = "81.1"

  case object CombinedFacilitiesSupport4 extends Sector:
    val code = "81.10"

  case object Cleaning extends Sector:
    val code = "81.2"

  case object GeneralCleaning extends Sector:
    val code = "81.21"

  case object IndustrialCleaning extends Sector:
    val code = "81.22"

  case object OtherCleaning extends Sector:
    val code = "81.23"

  case object Landscaping extends Sector:
    val code = "81.3"

  case object LandscapeServiceActivities extends Sector:
    val code = "81.30"

  case object OfficeAdministrativeSupport extends Sector:
    val code = "82"

  case object OfficeAdministrative extends Sector:
    val code = "82.1"

  case object OfficeAdministrativeActivities4 extends Sector:
    val code = "82.10"

  case object CallCentres extends Sector:
    val code = "82.2"

  case object CallCentresActivities extends Sector:
    val code = "82.20"

  case object ConventionsOrganisation extends Sector:
    val code = "82.3"

  case object ConventionsOrganisation4 extends Sector:
    val code = "82.30"

  case object BusinessSupportIntermediation extends Sector:
    val code = "82.4"

  case object BusinessSupportIntermediation4 extends Sector:
    val code = "82.40"

  case object OtherBusinessSupportIntermediationService extends Sector:
    val code = "82.9"

  case object CollectionAgencies extends Sector:
    val code = "82.91"

  case object PackagingActivities extends Sector:
    val code = "82.92"

  case object OtherBusinessSupport extends Sector:
    val code = "82.99"

  case object PublicAdministration extends Sector:
    val code = "P"

  case object PublicAdministrationSocialSecurity extends Sector:
    val code = "84"

  case object AdministrationGeneral extends Sector:
    val code = "84.1"

  case object GeneralPublicAdmin extends Sector:
    val code = "84.11"

  case object HealthEducationRegulation extends Sector:
    val code = "84.12"

  case object BusinessRegulation extends Sector:
    val code = "84.13"

  case object CommunityServices extends Sector:
    val code = "84.2"

  case object Defence extends Sector:
    val code = "84.22"

  case object FireService extends Sector:
    val code = "84.25"

  case object ForeignAffairs extends Sector:
    val code = "84.21"

  case object JusticeJudicial extends Sector:
    val code = "84.23"

  case object PublicOrderSafety extends Sector:
    val code = "84.24"

  case object CompulsorySocialSecurity extends Sector:
    val code = "84.3"

  case object CompulsorySocialSecurity4 extends Sector:
    val code = "84.30"

  case object Education extends Sector:
    val code = "Q"

  case object Education2 extends Sector:
    val code = "85"

  case object PrePrimaryEducation extends Sector:
    val code = "85.1"

  case object PrePrimaryEducation4 extends Sector:
    val code = "85.10"

  case object PrimaryEducation extends Sector:
    val code = "85.2"

  case object PrimaryEducation4 extends Sector:
    val code = "85.20"

  case object SecondaryEducation extends Sector:
    val code = "85.3"

  case object GeneralSecondaryEducation extends Sector:
    val code = "85.31"

  case object VocationalSecondaryEducation extends Sector:
    val code = "85.32"

  case object PostSecondaryEducationNonTertiary extends Sector:
    val code = "85.33"

  case object TertiaryEducation extends Sector:
    val code = "85.4"

  case object TertiaryEducation4 extends Sector:
    val code = "85.40"

  case object OtherEducation extends Sector:
    val code = "85.5"

  case object CulturalEducation extends Sector:
    val code = "85.52"

  case object DrivingSchool extends Sector:
    val code = "85.53"

  case object SportsRecreationEducation extends Sector:
    val code = "85.51"

  case object OtherEducationNEC extends Sector:
    val code = "85.59"

  case object EducationalSupport extends Sector:
    val code = "85.6"

  case object EducationalSupportIntermediation extends Sector:
    val code = "85.61"

  case object OtherEducationalSupport extends Sector:
    val code = "85.69"

  case object HumanHealthSocialWork extends Sector:
    val code = "R"

  case object HumanHealthActivities extends Sector:
    val code = "86"

  case object Hospital extends Sector:
    val code = "86.1"

  case object HospitalActivities extends Sector:
    val code = "86.10"

  case object MedicalDental extends Sector:
    val code = "86.2"

  case object Dental extends Sector:
    val code = "86.23"

  case object GeneralPractice extends Sector:
    val code = "86.21"

  case object Specialists extends Sector:
    val code = "86.22"

  case object OtherHumanHealth extends Sector:
    val code = "86.9"

  case object Psychologists extends Sector:
    val code = "86.93"

  case object DiagnosticImaging extends Sector:
    val code = "86.91"

  case object NursingMidwifery extends Sector:
    val code = "86.94"

  case object Ambulance extends Sector:
    val code = "86.92"

  case object Physiotherapy extends Sector:
    val code = "86.95"

  case object TraditionalMedicine extends Sector:
    val code = "86.96"

  case object HumanHealthActivitiesIntermediationServices extends Sector:
    val code = "86.97"

  case object OtherHumanHealthActivities4 extends Sector:
    val code = "86.99"

  case object ResidentialCareActivities extends Sector:
    val code = "87"

  case object ResidentialNursing extends Sector:
    val code = "87.1"

  case object ResidentialNursing4 extends Sector:
    val code = "87.10"

  case object ResidentialCareActivitiesMentalIllness extends Sector:
    val code = "87.2"

  case object ResidentialCareActivitiesMentalIllness4 extends Sector:
    val code = "87.20"

  case object DisabledResidentialCareActivitiesMentalIllness extends Sector:
    val code = "87.3"

  case object DisabledResidentialCareActivitiesMentalIllness4 extends Sector:
    val code = "87.30"

  case object OtherResidentialCare extends Sector:
    val code = "87.9"

  case object OtherResidentialCareIntermediation extends Sector:
    val code = "87.91"

  case object OtherResidentialCare4 extends Sector:
    val code = "87.99"

  case object OtherSocialWork extends Sector:
    val code = "88"

  case object SocialWorkDisabilities extends Sector:
    val code = "88.1"

  case object SocialWorkDisabilities4 extends Sector:
    val code = "88.10"

  case object OtherSocialWorkWithoutAccommodation extends Sector:
    val code = "88.9"

  case object ChildDayCare extends Sector:
    val code = "88.91"

  case object OtherSocialWorkNEC extends Sector:
    val code = "88.99"

  case object ArtsSportsRecreation extends Sector:
    val code = "S"

  case object LiteraryMusical extends Sector:
    val code = "90.11"

  case object ArtsCreationPerforming extends Sector:
    val code = "90"

  case object ArtsCreation extends Sector:
    val code = "90.1"

  case object VisualArtsCreation extends Sector:
    val code = "90.12"

  case object OtherArtsCreation extends Sector:
    val code = "90.13"

  case object PerformingArts extends Sector:
    val code = "90.2"

  case object PerformingArtsActivities extends Sector:
    val code = "90.20"

  case object PerformingArtsSupport extends Sector:
    val code = "90.3"

  case object ArtsFacilitiesOperation extends Sector:
    val code = "90.31"

  case object OtherPerformingArtsSupport extends Sector:
    val code = "90.39"

  case object LibrariesArchivesMuseums extends Sector:
    val code = "91"

  case object LibraryArchives extends Sector:
    val code = "91.1"

  case object Archives extends Sector:
    val code = "91.12"

  case object Libraries extends Sector:
    val code = "91.11"

  case object MuseumCollectionsMonuments extends Sector:
    val code = "91.2"

  case object HistoricalSitesMonuments extends Sector:
    val code = "91.22"

  case object MuseumsCollections extends Sector:
    val code = "91.21"

  case object CulturalHeritageConservation extends Sector:
    val code = "91.3"

  case object CulturalHeritageConservation4 extends Sector:
    val code = "91.30"

  case object BotanicalGardensAndNatureReserves extends Sector:
    val code = "91.4"

  case object BotanicalZoologicalGardens extends Sector:
    val code = "91.41"

  case object NatureReserves extends Sector:
    val code = "91.42"

  case object GamblingBetting extends Sector:
    val code = "92"

  case object GamblingActivities extends Sector:
    val code = "92.0"

  case object GamblingActivities4 extends Sector:
    val code = "92.00"

  case object SportsAndRecreation extends Sector:
    val code = "93"

  case object Sports extends Sector:
    val code = "93.1"

  case object FitnessCentresActivities extends Sector:
    val code = "93.13"

  case object SportsClubsActivities extends Sector:
    val code = "93.12"

  case object SportsFacilitiesOperation extends Sector:
    val code = "93.11"

  case object OtherSportsActivities extends Sector:
    val code = "93.19"

  case object AmusementRecreation extends Sector:
    val code = "93.2"

  case object AmusementParks extends Sector:
    val code = "93.21"

  case object OtherRecreationActivities extends Sector:
    val code = "93.29"

  case object OtherService extends Sector:
    val code = "T"

  case object MembershipOrganisations extends Sector:
    val code = "94"

  case object BusinessEmployersProfessional extends Sector:
    val code = "94.1"

  case object BusinessEmployersMembership extends Sector:
    val code = "94.11"

  case object ProfessionalMembership extends Sector:
    val code = "94.12"

  case object TradeUnions extends Sector:
    val code = "94.2"

  case object TradeUnionsActivities extends Sector:
    val code = "94.20"

  case object OtherMembership extends Sector:
    val code = "94.9"

  case object PoliticalOrganisations extends Sector:
    val code = "94.92"

  case object ReligiousOrganisations extends Sector:
    val code = "94.91"

  case object OtherMembershipOrganisations extends Sector:
    val code = "94.99"

  case object RepairMaintenance extends Sector:
    val code = "95"

  case object ComputerEquipment extends Sector:
    val code = "95.1"

  case object RepairComputersCommunication extends Sector:
    val code = "95.10"

  case object RepairHouseholdGoods extends Sector:
    val code = "95.2"

  case object RepairConsumerElectronics extends Sector:
    val code = "95.21"

  case object RepairHouseholdAppliances extends Sector:
    val code = "95.22"

  case object RepairFootwearLeather extends Sector:
    val code = "95.23"

  case object RepairFurnitureHome extends Sector:
    val code = "95.24"

  case object WatchesAndJewellery extends Sector:
    val code = "95.25"

  case object OtherHouseholdGoods extends Sector:
    val code = "95.29"

  case object RepairMotorVehiclesMotorcycles extends Sector:
    val code = "95.3"

  case object RepairMotorVehicles extends Sector:
    val code = "95.31"

  case object RepairMotorcycles extends Sector:
    val code = "95.32"

  case object IntermediationRepairMaintenance extends Sector:
    val code = "95.4"

  case object ComputerAndMotorcycleMaintenanceIntermediationServiceActivities4 extends Sector:
    val code = "95.40"

  case object PersonalServices extends Sector:
    val code = "96"

  case object TextileFurCleaning extends Sector:
    val code = "96.1"

  case object CleaningTextileProducts4 extends Sector:
    val code = "96.10"

  case object BeautyTreatment extends Sector:
    val code = "96.2"

  case object BeautyCare extends Sector:
    val code = "96.22"

  case object SpaActivities extends Sector:
    val code = "96.23"

  case object Hairdressing extends Sector:
    val code = "96.21"

  case object FuneralActivities extends Sector:
    val code = "96.3"

  case object FuneralActivities4 extends Sector:
    val code = "96.30"

  case object PersonalServicesIntermediation extends Sector:
    val code = "96.4"

  case object PersonalServicesIntermediation4 extends Sector:
    val code = "96.40"

  case object OtherPersonalServices extends Sector:
    val code = "96.9"

  case object DomesticPersonalServices extends Sector:
    val code = "96.91"

  case object OtherPersonalServices4 extends Sector:
    val code = "96.99"

  case object Households extends Sector:
    val code = "U"

  case object HouseholdsActivitiesOfDomesticPersonnel extends Sector:
    val code = "97"

  case object HouseholdsActivitiesOfDomesticPersonnel3 extends Sector:
    val code = "97.0"

  case object DomesticPersonnel extends Sector:
    val code = "97.00"

  case object UndifferentiatedGoodsAndServiceActivities extends Sector:
    val code = "98"

  case object UndifferentiatedGoods extends Sector:
    val code = "98.1"

  case object UndifferentiatedGoods4 extends Sector:
    val code = "98.10"

  case object UndifferentiatedServices extends Sector:
    val code = "98.2"

  case object UndifferentiatedProduction extends Sector:
    val code = "98.20"

  case object ActivitiesExtraterritorial extends Sector:
    val code = "V"

  case object ExtraterritorialOrganisationsActivities extends Sector:
    val code = "99"

  case object ExtraterritorialOrganisationsActivities3 extends Sector:
    val code = "99.0"

  case object ExtraterritorialOrganisationsActivities4 extends Sector:
    val code = "99.00"

  val all: List[Sector] =
    List(
      Other,
      Transport,
      Agriculture,
      Aquaculture,
      GeneralTrade,
      OtherGeneralTrade,
      ManuGroup1,
      ManuGroup2,
      ManuGroup3,
      ManuGroup4,
      ManuGroup5,
      ManuGroup6,
      ManuGroup7,
      AgricultureForestryFishing,
      CropAnimalProduction,
      GrowingNonPerennialCrops,
      CerealsLeguminousCrops,
      FibreCrops,
      Rice,
      SugarCane,
      Tobacco,
      Vegetables,
      OtherNonPerennialCrops,
      GrowingPerennialCrops,
      BeverageCrops,
      CitrusFruits,
      Grapes,
      OleaginousFruits,
      StoneFruits,
      SpicesPharmaceuticalCrops,
      TropicalFruits,
      OtherTree,
      OtherPerennialCrops,
      PlantPropagation,
      PlantPropagation4,
      AnimalProduction,
      DairyCattle,
      OtherCattle,
      Camels,
      Horses,
      Poultry,
      Sheep,
      Swine,
      OtherAnimals,
      MixedFarming,
      MixedFarming4,
      SupportActivities,
      PostHarvestActivities,
      SupportActivitiesAnimal,
      SupportActivitiesCrop,
      HuntingTrapping,
      HuntingTrapping4,
      Forestry,
      Silviculture,
      Silviculture4,
      Logging,
      Logging4,
      GatheringOfWildGrowth,
      GatheringOfWildGrowth4,
      ForestrySupportServices,
      ForestrySupportServices4,
      FishingAndAquaculture,
      Fishing,
      FreshwaterFishing,
      MarineFishing,
      Aquaculture3,
      FreshwaterAquaculture,
      MarineAquaculture,
      AquacultureSupportActivities,
      AquacultureSupportActivities4,
      MiningQuarrying,
      CoalAndLigniteMining,
      Hardcoal,
      HardcoalMining,
      Lignite,
      LigniteMining,
      PetroleumNaturalGasExtraction,
      CrudePetroleum,
      CrudePetroleumExtraction,
      NaturalGas,
      NaturalGasExtraction,
      MetalOresMining,
      IronOre,
      IronOresMining,
      NonFerrousOres,
      UraniumThoriumOres,
      OtherNonFerrousOres,
      OtherMiningQuarrying,
      StoneQuarrying,
      GravelPitsOperation,
      OrnamentalQuarrying,
      MiningAndQuarryingNEC,
      PeatExtraction,
      SaltExtraction,
      ChemicalMineralsMining,
      OtherNEC,
      MiningSupportServices,
      MiningSupportPetroleumExtraction,
      MiningSupportPetroleumExtraction4,
      OtherMiningSupport,
      OtherMiningSupport4,
      Beverages,
      BeverageManufacture,
      Beer,
      Ciders,
      Malt,
      SoftDrinks,
      Spirits,
      Wine,
      OtherFermentedBeverages,
      TobaccoProducts,
      TobaccoProductsManufacture3,
      TobaccoProductsManufacture4,
      Textiles,
      TextilesPreparation,
      TextilesPreparation4,
      TextilesWeaving,
      TextilesWeaving4,
      TextilesFinishing,
      TextilesFinishing4,
      TextilesManufacture,
      Carpets,
      Cordage,
      HouseholdTextiles,
      KnittedFabrics,
      NonWoven,
      OtherTechnicalTextiles,
      OtherTextile,
      Clothing,
      KnittedCrotchetedClothing,
      KnittedClothingManufacture,
      OtherClothing,
      LeatherAndFurClothing,
      Outerwear,
      Underwear,
      Workwear,
      OtherClothing4,
      LeatherProducts,
      LeatherManufacture,
      LuggageManufacture,
      LeatherTanning,
      FootwearManufacture,
      FootwearManufacture4,
      WoodProducts,
      WoodProductsProcessing,
      ProcessingAndFinishing,
      SawmillingAndPlaning,
      WoodProductsManufacture,
      AssembledParquetFloors,
      SolidFuelsVegetableBiomass,
      VeneerSheetsPanels,
      WoodenContainers,
      WoodenWindowsDoors,
      OtherBuildersCarpentryJoinery,
      OtherWoodProducts,
      WoodenProductsFinishing,
      PaperRelated,
      Pulp,
      Pulp4,
      Paper,
      ArticlesPaperBoard,
      CorrugatedPaperBoardAndContainers,
      SanitaryGoods,
      PaperStationery,
      Wallpaper,
      OtherPaper,
      PrintedProducts,
      PrintingService,
      BindingServices,
      PreMediaServices,
      NewspapersPrinting,
      OtherPrinting,
      RecordedMediaReproduction,
      RecordedMediaReproduction4,
      CokeProducts,
      OvenCokeProducts,
      CokeProductsManufacture,
      FossilFuelProducts,
      FossilFuelProductsManufacture,
      ChemicalProducts,
      BasicChemicalProducts,
      Dyes,
      Fertilisers,
      IndustrialGases,
      PrimaryFormsPlastics,
      SyntheticRubber,
      OtherInorganicChemicals,
      OtherOrganicChemicals,
      PesticidesDisinfectants,
      PesticidesDisinfectantsManufacture,
      PaintsVarnishesCoatings,
      PaintsVarnishesCoatingsManufacture,
      WashingCleaning,
      PerfumesToiletPreparations,
      Soap,
      OtherChemicalProducts,
      LiquidBiofuels,
      OtherChemicalProducts4,
      ManMadeFibre,
      ManMadeFibreManufacture,
      Pharmaceuticals,
      BasicPharmaceuticals,
      PharmaceuticalsManufacture,
      PharmaceuticalPreparations,
      PharmaceuticalPreparationsManufacture,
      RubberPlasticProducts,
      RubberProducts,
      RubberTubes,
      OtherRubberProducts,
      PlasticProducts,
      BuildersWare,
      DoorsAndWindows,
      PackingGoods,
      PlasticPlatesSheetsTubes,
      OtherPlasticProduct,
      FinishingPlasticProducts,
      OtherNonMetallicProducts,
      GlassProducts,
      FlatGlass,
      GlassFibres,
      HollowGlass,
      OtherGlassProducts,
      ShapingFlatGlass,
      RefractoryProducts,
      RefractoryProductsManufacture,
      ClayBuildingMaterials,
      Bricks,
      CeramicTiles,
      OtherCeramicProducts,
      CeramicHousehold,
      CeramicInsulating,
      CeramicSanitary,
      OtherTechnicalCeramicProducts,
      OtherCeramicProducts4,
      CementLimePlaster,
      Cement,
      PlasterLime,
      CementPlasterArticles,
      ConcreteProducts,
      FibreCement,
      Mortars,
      PlasterProducts,
      ReadyMixedConcrete,
      OtherConcreteProducts,
      StoneCuttingFinishing,
      StoneCuttingFinishing4,
      OtherAbrasiveProducts,
      AbrasiveProducts,
      OtherNonMetallicMineral,
      BasicMetals,
      BasicIronSteel,
      BasicIronSteelManufacture,
      SteelTubesFittings,
      SteelTubesFittingsManufacture,
      OtherSteelProductsProcessing,
      BarsColdDrawing,
      WireColdDrawing,
      FoldingColdDrawing,
      NarrowStripColdDrawing,
      BasicPreciousAndNonFerrousMetals,
      NuclearFuelProcessing,
      AluminiumProduction,
      CopperProduction,
      LeadZincTinProduction,
      PreciousMetalsProduction,
      NonFerrousMetalsProduction,
      MetalsCasting,
      Iron,
      LightMetals,
      Steel,
      OtherMetals,
      FabricatedMetalProducts,
      FabricatedStructuralMetalProducts,
      MetalDoors,
      MetalStructures,
      MetalTanksManufacture,
      CentralHeating,
      OtherMetalTanks,
      WeaponsManufacture,
      WeaponsManufacture4,
      MetalForging,
      MetalForging4,
      MetalTreatmentCoating,
      Coating,
      HeatTreatment,
      Machining,
      CutleryToolsManufacture,
      Cutlery,
      Locks,
      Tools,
      OtherFabricatedMetalProductsManufacture,
      FastenerProducts,
      LightMetalPackaging,
      SteelDrums,
      WireProducts,
      OtherFabricatedMetalProducts,
      ComputersProducts,
      ElectronicComponents,
      ElectronicComponents4,
      LoadedElectronicBoards,
      ComputersPeripheralEquipment,
      ComputersPeripheralEquipment4,
      CommunicationEquipment,
      CommunicationEquipment4,
      ConsumerElectronics,
      ConsumerElectronics4,
      MeasuringToolsAndClocks,
      MeasuringInstruments,
      WatchesAndClocks,
      IrradiationElectromedicalAndElectrotherapeuticEquipment,
      IrradiationElectromedicalAndElectrotherapeuticEquipment4,
      OpticalEquipment,
      OpticalEquipment4,
      ElectricalEquipment,
      ElectricMotorsGenerators,
      ElectricMotors,
      ElectricityDistributionAndControl,
      Batteries,
      BatteriesManufacture,
      Wiring,
      FibreOpticCables,
      OtherElectronicWires,
      WiringDevices,
      LightingEquipment,
      LightingEquipmentManufacture,
      DomesticAppliances,
      ElectricDomesticAppliances,
      NonElectricDomesticAppliances,
      OtherElectricalEquipment,
      OtherElectricalEquipmentManufacture,
      OtherMachineryAndEquipment,
      GeneralPurposeMachinery,
      Bearings,
      Engines,
      FluidPowerEquipment,
      OtherPumps,
      OtherTaps,
      OtherGeneralPurposeMachinery,
      LiftingEquipment,
      NonDomesticAirConditioning,
      OfficeMachinery,
      Ovens,
      PowerDrivenHandTools,
      OtherGeneralPurposeMachinery4,
      AgriculturalMachinery,
      AgriculturalMachineryManufacture,
      MetalFormingMachinery,
      MetalFormingMachinery4,
      OtherMachineTools,
      OtherSpecialPurposeMachinery,
      AdditiveManufacturingMachinery,
      FoodMachinery,
      MetallurgyMachinery,
      MiningMachinery,
      PaperMachinery,
      TextileMachinery,
      PlasticsMachinery,
      OtherSpecialPurposeMachinery4,
      MotorVehicles,
      MotorVehicles3,
      MotorVehiclesManufacture,
      MotorVehiclesBodies,
      MotorVehiclesBodies4,
      MotorVehiclesParts,
      MotorVehiclesElectricalManufacture,
      MotorVehiclesOtherManufacture,
      OtherTransportEquipment,
      Ships,
      CivilianShips,
      MilitaryShips,
      PleasureShips,
      Railway,
      RailwayManufacture,
      Aircraft,
      CivilianAircraftMachinery,
      MilitaryAircraftMachinery,
      MilitaryFightingVehicles,
      MilitaryFightingVehiclesManufacture,
      OtherTransportEquipment3,
      Bicycles,
      MotorCyclesEquipment,
      OtherTransportEquipment4,
      FurnitureManufacture,
      FurnitureManufacture3,
      FurnitureManufacture4,
      OtherManufacturing,
      JewelleryAndCoins,
      JewelleryManufacture,
      ImitationJewelleryManufacture,
      CoinStriking,
      MusicalInstruments,
      MusicalInstrumentsManufacture,
      SportsGoods,
      SportsGoodsManufacture,
      GamesAndToys,
      GamesAndToysManufacture,
      MedicalInstruments,
      MedicalInstrumentsManufacture,
      OtherProducts,
      BroomsAndBrushes,
      OtherProductManufacture,
      MetalProductsRepairMaintenance,
      FabricatedMetalProductsRepair,
      CivilianAirRepair,
      CivilianShipsRepair,
      OtherCivilianTransportEquipmentRepair,
      ElectricalEquipmentRepair,
      ElectronicEquipmentRepair,
      FabricatedMetalProducts4,
      MachineryRepair,
      MilitaryVehiclesRepair,
      OtherEquipmentRepair,
      IndustrialMachineryInstallation,
      IndustrialMachineryInstallation4,
      ElectricityGas,
      ElectricityGasAirConditioningSupply,
      ElectricPowerGenerationAndDistribution,
      ElectricityDistribution,
      NonRenewableElectricityProduction,
      RenewableElectricityProduction,
      ElectricityStorage,
      ElectricityTrade,
      ElectricityTransmission,
      GaseousFuelsManufacture,
      GaseousFuelDistribution,
      GaseousFuelManufacture,
      GaseousFuelStorage,
      GaseousFuelTrade,
      SteamAndAirConditioningSupply,
      SteamAndAirConditioningSupply4,
      ElectricPowerAndGasBrokers,
      ElectricPowerAndGasBrokers4,
      WaterSupply,
      WaterTreatment,
      WaterTreatment3,
      WaterTreatment4,
      Sewerage,
      Sewerage3,
      Sewerage4,
      WasteManagement,
      WasteCollection,
      HazardousWaste,
      NonHazardousWaste,
      WasteRecovery,
      EnergyRecovery,
      MaterialsRecovery,
      OtherWasteRecovery,
      WasteDisposal,
      Incineration,
      LandFilling,
      OtherWasteDisposal,
      WasteManagementRemediation,
      WasteManagementRemediation3,
      WasteManagementRemediation4,
      Construction,
      ResidentialNonResidentialConstruction,
      ResidentialNonResidentialConstruction3,
      ResidentialNonResidentialConstruction4,
      CivilEngineering,
      RoadAndRailConstruction,
      BridgesTunnels,
      RailwaysUnderground,
      RoadsMotorways,
      UtilityConstruction,
      ElectricityUtilityConstruction,
      FluidsUtilityConstruction,
      OtherUtilityConstructionProjects,
      WaterProjects,
      OtherCivilEngineering,
      SpecialisedConstructionActivities,
      DemolitionAndPreparation,
      Demolition,
      SitePreparation,
      TestDrillingBoring,
      ElectricalAndPlumbingInstallation,
      ElectricalInstallation,
      InsulationInstallation,
      PlumbingHeatingAC,
      OtherConstructionInstallation,
      BuildingFinishing,
      FloorAndWallCovering,
      JoineryInstallation,
      PaintingAndGlazing,
      Plastering,
      OtherBuildingFinishing,
      SpecialisedBuildingActivities,
      RoofingActivities,
      OtherSpecialisedBuildingActivities,
      SpecialisedCivilEngineering,
      SpecialisedCivilEngineering4,
      SpecialisedConstructionIntermediationServices,
      SpecialisedConstructionIntermediationServices4,
      OtherSpecialisedConstruction,
      MasonryAndBricklaying,
      OtherSpecialisedConstructionActivities,
      WholesaleAndRetailTrade,
      Wholesale,
      WholesaleContractBasis,
      AgriculturalWholesaleContractBasis,
      FoodWholesaleContractBasis,
      FuelsWholesaleContractBasis,
      FurnitureWholesaleContractBasis,
      MachineryWholesaleContractBasis,
      TextilesWholesaleContractBasis,
      TimberWholesaleContractBasis,
      OtherWholesaleContractBasis,
      NonSpecialisedWholesaleContractBasis,
      AgriculturalWholesale,
      FlowersAndPlants,
      Grain,
      LeatherWholesale,
      LiveAnimals,
      FoodWholesale,
      BeveragesWholesale,
      CoffeeAndSpicesWholesale,
      DairyProductsWholesale,
      FruitWholesale,
      MeatWholesale,
      SugarWholesale,
      TobaccoProductsWholesale,
      OtherFoodWholesale,
      NonSpecialisedFoodWholesale,
      WholesaleHouseholdGoods,
      ChinaWholesale,
      ClothingWholesale,
      ElectricalHouseholdWholesale,
      HouseholdWholesale,
      PerfumeWholesale,
      PharmaceuticalWholesale,
      TextilesWholesale,
      WatchesWholesale,
      OtherHouseholdGoodsWholesale,
      InformationEquipmentWholesale,
      InformationEquipmentWholesale4,
      MachineryWholesale,
      AgriculturalMachineryWholesale,
      MachineToolsWholesale,
      MiningMachineryWholesale,
      OtherMachineryWholesale,
      MotorVehiclesWholesale,
      MotorVehicleWholesale,
      MotorVehiclePartsWholesale,
      MotorcyclePartsWholesale,
      OtherSpecialisedWholesale,
      ChemicalProductsWholesale,
      HardwareEquipment,
      Metals,
      Fuels,
      Waste,
      Wood,
      OtherIntermediateProducts,
      OtherSpecialisedWholesale4,
      NonSpecialisedWholesale,
      NonSpecialisedWholesaleTrade,
      Retail,
      NonSpecialisedRetail,
      NonSpecialisedFoodRetail,
      OtherNonSpecialisedRetail,
      FoodRetail,
      BeveragesRetail,
      BreadRetail,
      FishRetail,
      FruitRetail,
      MeatRetail,
      TobaccoRetail,
      OtherFoodRetail,
      AutomotiveFuelRetail,
      AutomotiveFuelRetail4,
      CommunicationEquipmentRetail,
      CommunicationEquipmentRetail4,
      OtherHouseholdEquipmentRetail,
      CarpetsRetail,
      ElectricalRetail,
      HardwareRetail,
      TextilesRetail,
      FurnitureRetail,
      CulturalRetail,
      BooksRetail,
      GamesRetail,
      NewspapersRetail,
      SportingEquipmentRetail,
      OtherCulturalAndRecreationalGoods,
      OtherGoodsRetail,
      ClothingRetail,
      CosmeticRetail,
      FlowersRetail,
      FootwearRetail,
      MedicalRetail,
      PharmaceuticalRetail,
      WatchesRetail,
      OtherNewGoodsRetail,
      SecondHandGoods,
      MotorVehiclesAndMotorcyclesRetail,
      MotorVehiclesRetail,
      MotorVehiclesPartsRetail,
      MotorCyclesRetail,
      RetailIntermediationServices,
      NonSpecialisedRetailIntermediation,
      SpecialisedRetailIntermediation,
      TransportStorage,
      LandTransport,
      PassengerRailTransport,
      PassengerHeavyRailTransport,
      OtherPassengerRailTransport,
      FreightRailTransport,
      FreightRailTransport4,
      OtherPassengerLandTransport,
      NonScheduledPassengerRoadTransport,
      OnDemandPassengerRoadTransport,
      CablewaysPassengerTransport,
      ScheduledPassengerRoadTransport,
      OtherLandTransport,
      FreightRoadTransport,
      FreightRoadTransport4,
      RemovalServices,
      PipelineTransport,
      PipelineTransport4,
      WaterTransport,
      CoastalPassengerWaterTransport,
      CoastalPassengerWaterTransport4,
      CoastalFreightWaterTransport,
      CoastalFreightWaterTransport4,
      InlandPassengerWaterTransport,
      InlandPassengerWaterTransport4,
      InlandFreightWaterTransport,
      InlandFreightWaterTransport4,
      AirTransport,
      PassengerAirTransport,
      PassengerAirTransport4,
      FreightAndSpaceAirTransport,
      FreightAirTransport,
      SpaceTransport,
      WarehousingStorageTransportSupportActivities,
      WarehousingStorage,
      WarehousingStorage4,
      TransportSupportActivities,
      CargoHandling,
      LogisticsServiceActivities,
      AirTransportServiceActivities,
      LandTransportServiceActivities,
      WaterTransportServiceActivities,
      OtherTransportSupportActivities,
      TransportIntermediationServiceActivities,
      FreightTransportIntermediationServiceActivities,
      PassengerTransportIntermediationServiceActivities,
      PostalAndCourierActivities,
      PostalActivities,
      PostalActivities4,
      OtherPostalActivities,
      OtherPostalActivities4,
      PostalIntermediationActivities,
      PostalIntermediationActivities4,
      AccommodationAndFoodService,
      Accommodation,
      HotelsAccommodation,
      HotelsAccommodation4,
      HolidayShortStay,
      HolidayShortStay4,
      CampingGrounds,
      CampingGrounds4,
      IntermediationAccommodation,
      IntermediationAccommodation4,
      OtherAccommodation,
      OtherAccommodation4,
      FoodBeverageServiceActivities,
      RestaurantsMobile,
      RestaurantsMobile4,
      MobileFoodServices,
      EventAndContractCatering,
      EventCatering,
      ContractCatering,
      BeverageServing,
      BeverageServing4,
      FoodBeverageServiceIntermediationActivities,
      FoodBeverageServiceIntermediationActivities4,
      PublishingBroadcasting,
      Publishing,
      PublishingBooksNewspapers,
      Books,
      JournalsPeriodicals,
      Newspapers,
      OtherPublishing,
      SoftwarePublishing,
      VideoGames,
      OtherSoftware,
      FilmVideoSoundPublishing,
      FilmVideoActivities,
      VideoProductionDistribution,
      VideoPostProduction,
      VideoProduction,
      Projection,
      SoundRecordingAndMusicPublishing,
      SoundRecordingAndMusicPublishingActivities,
      ProgrammingBroadcastingNewsActivities,
      RadioBroadcasting,
      RadioBroadcasting4,
      ProgrammingBroadcastingVideoDistribution,
      ProgrammingBroadcastingVideoDistribution4,
      NewsAgency,
      NewsAgencyActivities,
      OtherContentDistributionActivities,
      Telecommunications,
      Telecommunication,
      WiredTelecommunication,
      WiredTelecommunication4,
      ResellingTelecommunication,
      ResellingTelecommunication4,
      OtherTelecommunications,
      OtherTelecommunications4,
      ComputerProgrammingConsultancy,
      ComputerProgrammingActivities,
      ComputerProgrammingActivities4,
      ComputerFacilitiesConsultancy,
      ComputerFacilitiesConsultancy4,
      OtherComputerServiceActivities,
      OtherComputerServiceActivities4,
      ComputingInfrastructureActivities,
      ComputingInfrastructureActivities3,
      ComputingInfrastructureActivities4,
      WebSearchPortal,
      WebSearchPortalActivities,
      OtherInformationServices,
      FinancialInsuranceActivities,
      FinancialServices,
      MonetaryIntermediation,
      CentralBanking,
      OtherMonetary,
      HoldingCompaniesFinal,
      FinancingConduits,
      HoldingCompanies,
      TrustsFunds,
      InvestmentFunds,
      TrustEstate,
      OtherFinancial,
      FinancialLeasing,
      OtherCredit,
      OtherFinancialServices,
      InsuranceReinsurancePensionFunding,
      InsuranceServices,
      LifeInsurance,
      NonLifeInsurance,
      ReinsuranceServices,
      Reinsurance4,
      PensionFundingServices,
      PensionFunding4,
      AuxiliaryActivities,
      AuxiliaryFinancialServices,
      FinancialMarkets,
      SecurityBrokerage,
      OtherAuxiliaryFinancial,
      AuxiliaryInsurance,
      InsuranceAgents,
      RiskEvaluation,
      OtherAuxiliaryInsurance,
      FundManagementServices,
      FundManagementActivities,
      RealEstate,
      RealEstateActivities,
      PropertyDevelopment,
      BuyingSelling,
      DevelopmentProjects,
      RentalOperating,
      RealEstateRental4,
      FeeContract,
      Intermediation,
      OtherFeeContract,
      ProfessionalScientificAndTechnicalActivities,
      LegalAndAccounting,
      Legal,
      LegalActivities4,
      Accounting,
      AccountingTaxConsultancy,
      HeadOfficesAndManagementConsultancy,
      HeadOffice,
      HeadOfficesActivities4,
      ManagementConsultancy,
      ManagementConsultancyActivities4,
      ArchitecturalAndTechnical,
      ArchitecturalAndTechnicalActivities,
      ArchitecturalEngineering,
      EngineeringConsultancy,
      TechnicalTesting,
      TechnicalTesting4,
      ScientificResearchAndDevelopment,
      NaturalScientificResearchAndDevelopment,
      NaturalScientificResearchAndDevelopment4,
      SocialScientificResearchAndDevelopment,
      SocialScientificResearchAndDevelopment4,
      AdvertisingMarketResearch,
      Advertising,
      AdvertisingActivities,
      MediaRepresentation,
      MarketResearch,
      MarketResearch4,
      PublicRelations,
      PublicRelations4,
      OtherProfessionalScientific,
      SpecialisedDesign,
      GraphicDesign,
      IndustrialFashionDesign,
      InteriorDesign,
      OtherDesign,
      Photography,
      PhotographicActivities,
      TranslationInterpretation,
      TranslationActivities4,
      OtherProfessionalScientificActivities,
      PatentBrokering,
      OtherProfessionalScientificActivities4,
      Veterinary,
      VeterinaryActivities3,
      VeterinaryActivities4,
      Administration,
      RentalAndLeasing,
      MotorVehiclesRental,
      CarsLightVehicles,
      Trucks,
      PersonalRental,
      RecreationalSportsGoods,
      OtherPersonalHouseholdGoods,
      OtherMachineryRental,
      AgriculturalMachineryRental,
      AirTransportRental,
      ConstructionMachineryRental,
      OfficeMachineryRental,
      WaterTransportRental,
      OtherMachineryRental4,
      IntellectualProperty,
      LeasingIntellectualProperty,
      TangibleGoodsRentalIntermediation,
      CarsMotorhomesTrailers,
      OtherTangibleGoodsRental,
      Employment,
      EmploymentPlacementActivities,
      EmploymentPlacementActivities4,
      TempEmploymentPlacementActivities,
      TempEmploymentPlacementActivities4,
      TravelAgencyAndReservation,
      TravelAgency,
      TourOperator,
      TravelAgencyActivities,
      OtherReservationServices,
      OtherReservationServices4,
      InvestigationAndSecurity,
      InvestigationAndSecurityActivities,
      PrivateInvestigationAndSecurityActivities,
      OtherInvestigationAndSecurityActivities,
      BuildingsAndLandscapingServices,
      CombinedFacilitiesSupport,
      CombinedFacilitiesSupport4,
      Cleaning,
      GeneralCleaning,
      IndustrialCleaning,
      OtherCleaning,
      Landscaping,
      LandscapeServiceActivities,
      OfficeAdministrativeSupport,
      OfficeAdministrative,
      OfficeAdministrativeActivities4,
      CallCentres,
      CallCentresActivities,
      ConventionsOrganisation,
      ConventionsOrganisation4,
      BusinessSupportIntermediation,
      BusinessSupportIntermediation4,
      OtherBusinessSupportIntermediationService,
      CollectionAgencies,
      PackagingActivities,
      OtherBusinessSupport,
      PublicAdministration,
      PublicAdministrationSocialSecurity,
      AdministrationGeneral,
      GeneralPublicAdmin,
      HealthEducationRegulation,
      BusinessRegulation,
      CommunityServices,
      Defence,
      FireService,
      ForeignAffairs,
      JusticeJudicial,
      PublicOrderSafety,
      CompulsorySocialSecurity,
      CompulsorySocialSecurity4,
      Education,
      Education2,
      PrePrimaryEducation,
      PrePrimaryEducation4,
      PrimaryEducation,
      PrimaryEducation4,
      SecondaryEducation,
      GeneralSecondaryEducation,
      VocationalSecondaryEducation,
      PostSecondaryEducationNonTertiary,
      TertiaryEducation,
      TertiaryEducation4,
      OtherEducation,
      CulturalEducation,
      DrivingSchool,
      SportsRecreationEducation,
      OtherEducationNEC,
      EducationalSupport,
      EducationalSupportIntermediation,
      OtherEducationalSupport,
      HumanHealthSocialWork,
      HumanHealthActivities,
      Hospital,
      HospitalActivities,
      MedicalDental,
      Dental,
      GeneralPractice,
      Specialists,
      OtherHumanHealth,
      Psychologists,
      DiagnosticImaging,
      NursingMidwifery,
      Ambulance,
      Physiotherapy,
      TraditionalMedicine,
      HumanHealthActivitiesIntermediationServices,
      OtherHumanHealthActivities4,
      ResidentialCareActivities,
      ResidentialNursing,
      ResidentialNursing4,
      ResidentialCareActivitiesMentalIllness,
      ResidentialCareActivitiesMentalIllness4,
      DisabledResidentialCareActivitiesMentalIllness,
      DisabledResidentialCareActivitiesMentalIllness4,
      OtherResidentialCare,
      OtherResidentialCareIntermediation,
      OtherResidentialCare4,
      OtherSocialWork,
      SocialWorkDisabilities,
      SocialWorkDisabilities4,
      OtherSocialWorkWithoutAccommodation,
      ChildDayCare,
      OtherSocialWorkNEC,
      ArtsSportsRecreation,
      ArtsCreationPerforming,
      LiteraryMusical,
      ArtsCreation,
      VisualArtsCreation,
      OtherArtsCreation,
      PerformingArts,
      PerformingArtsActivities,
      PerformingArtsSupport,
      ArtsFacilitiesOperation,
      OtherPerformingArtsSupport,
      LibrariesArchivesMuseums,
      LibraryArchives,
      Archives,
      Libraries,
      MuseumCollectionsMonuments,
      HistoricalSitesMonuments,
      MuseumsCollections,
      CulturalHeritageConservation,
      CulturalHeritageConservation4,
      BotanicalGardensAndNatureReserves,
      BotanicalZoologicalGardens,
      NatureReserves,
      GamblingBetting,
      GamblingActivities,
      GamblingActivities4,
      SportsAndRecreation,
      Sports,
      FitnessCentresActivities,
      SportsClubsActivities,
      SportsFacilitiesOperation,
      OtherSportsActivities,
      AmusementRecreation,
      AmusementParks,
      OtherRecreationActivities,
      OtherService,
      MembershipOrganisations,
      BusinessEmployersProfessional,
      BusinessEmployersMembership,
      ProfessionalMembership,
      TradeUnions,
      TradeUnionsActivities,
      OtherMembership,
      PoliticalOrganisations,
      ReligiousOrganisations,
      OtherMembershipOrganisations,
      RepairMaintenance,
      ComputerEquipment,
      RepairComputersCommunication,
      RepairHouseholdGoods,
      RepairConsumerElectronics,
      RepairHouseholdAppliances,
      RepairFootwearLeather,
      RepairFurnitureHome,
      WatchesAndJewellery,
      OtherHouseholdGoods,
      RepairMotorVehiclesMotorcycles,
      RepairMotorVehicles,
      RepairMotorcycles,
      IntermediationRepairMaintenance,
      ComputerAndMotorcycleMaintenanceIntermediationServiceActivities4,
      PersonalServices,
      TextileFurCleaning,
      CleaningTextileProducts4,
      BeautyTreatment,
      BeautyCare,
      SpaActivities,
      Hairdressing,
      FuneralActivities,
      FuneralActivities4,
      PersonalServicesIntermediation,
      PersonalServicesIntermediation4,
      OtherPersonalServices,
      DomesticPersonalServices,
      OtherPersonalServices4,
      Households,
      HouseholdsActivitiesOfDomesticPersonnel,
      HouseholdsActivitiesOfDomesticPersonnel3,
      DomesticPersonnel,
      UndifferentiatedGoodsAndServiceActivities,
      UndifferentiatedGoods,
      UndifferentiatedGoods4,
      UndifferentiatedServices,
      UndifferentiatedProduction,
      ActivitiesExtraterritorial,
      ExtraterritorialOrganisationsActivities,
      ExtraterritorialOrganisationsActivities3,
      ExtraterritorialOrganisationsActivities4,
      Food,
      Manufacturing,
      Meat,
      MeatProcessing,
      PoultryProcessing,
      MeatProductsProduction,
      Fish,
      FishProcessing,
      FruitAndVegetables,
      FruitAndVegetableJuiceManufacture,
      FruitAndVegetableProcessing,
      OtherFruitAndVegetableProcessing,
      Oils,
      Margarine,
      OtherOils,
      DairyProducts,
      DairyProducts4,
      IceCream,
      GrainAndStarchProducts,
      GrainProducts4,
      StarchProducts,
      BakeryProducts,
      BiscuitsAndPreservedCakes,
      BreadAndFreshPastry,
      FarinaceousProducts,
      OtherFoodProducts,
      OtherFoodProduct,
      Confectionery,
      Condiments,
      HomogenisedFoodPreparations,
      PreparedMeals,
      Sugar,
      TeaAndCoffee,
      FarmAnimalsFood,
      PreparedAnimalFeeds,
      PetFood
    )

  private val byCode: Map[String, Sector] =
    all.map(s => s.code -> s).toMap

  def fromCode(code: String): Sector =
    byCode(code)

  given Format[Sector] = new Format[Sector]:

    override def reads(json: JsValue): JsResult[Sector] =
      json match
        case JsString(code) =>
          Try(Sector.fromCode(code)) match
            case Success(v) => JsSuccess(v)
            case Failure(_) => JsError(s"Unknown Sector code: $code")

        case other =>
          JsError(s"Expected string Sector, got $other")

    override def writes(o: Sector): JsValue =
      JsString(o.code)
