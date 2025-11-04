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

package uk.gov.hmrc.eusubsidycompliancefrontend.models

import play.api.libs.json._

sealed trait NACESector01

object NACESector01 extends {

  case object Agriculture extends WithName("agriculture") with NACESector01

  case object FisheryAquaculture extends WithName("fisheryAquaculture") with NACESector01

  case object AccommodationFoodServices extends WithName("accommodationFoodServices") with NACESector01

  case object Administration extends WithName("administration") with NACESector01

  case object ArtsSportsRecreation extends WithName("artsSportsRecreation") with NACESector01

  case object Construction extends WithName("construction") with NACESector01

  case object Education extends WithName("education") with NACESector01

  case object ElectricityGas extends WithName("electricityGas") with NACESector01

  case object FinancialServicesInsurance extends WithName("financialServicesInsurance") with NACESector01

  case object Forestry extends WithName("forestry") with NACESector01

  case object HumanHealthSocialWork extends WithName("humanHealthSocialWork") with NACESector01

  case object Manufacturing extends WithName("manufacturing") with NACESector01

  case object MiningQuarrying extends WithName("miningQuarrying") with NACESector01

  case object ProfessionalScientificTechnicalServices
      extends WithName("professionalScientificTechnicalServices")
      with NACESector01

  case object PublishingBroadcasting extends WithName("publishingBroadcasting") with NACESector01

  case object RealEstate extends WithName("realEstate") with NACESector01

  case object Telecommunications extends WithName("telecommunications") with NACESector01

  case object TransportStorage extends WithName("transportStorage") with NACESector01

  case object WaterSupply extends WithName("waterSupply") with NACESector01

  case object WholesaleRetail extends WithName("wholesaleRetail") with NACESector01

  case object PublicAdministration extends WithName("publicAdministration") with NACESector01

  case object OtherService extends WithName("otherService") with NACESector01

  case object Households extends WithName("households") with NACESector01

  final object ActivitiesExtraterritorial extends WithName("activitiesExtraterritorial") with NACESector01

  val enumerableValues: List[NACESector01] = List(
    Agriculture,
    FisheryAquaculture,
    AccommodationFoodServices,
    Administration,
    ArtsSportsRecreation,
    Construction,
    Education,
    ElectricityGas,
    FinancialServicesInsurance,
    Forestry,
    HumanHealthSocialWork,
    Manufacturing,
    MiningQuarrying,
    ProfessionalScientificTechnicalServices,
    PublishingBroadcasting,
    RealEstate,
    Telecommunications,
    TransportStorage,
    WaterSupply,
    WholesaleRetail,
    PublicAdministration,
    OtherService,
    Households,
    ActivitiesExtraterritorial
  )

  implicit lazy val reads: Reads[NACESector01] =
    (__ \ "type").read[String].flatMap {
      case "agriculture" => Reads.pure(Agriculture)
      case "fisheryAquaculture" => Reads.pure(FisheryAquaculture)
      case "accommodationFoodServices" => Reads.pure(AccommodationFoodServices)
      case "administration" => Reads.pure(Administration)
      case "artsSportsRecreation" => Reads.pure(ArtsSportsRecreation)
      case "construction" => Reads.pure(Construction)
      case "education" => Reads.pure(Education)
      case "electricityGas" => Reads.pure(ElectricityGas)
      case "financialServicesInsurance" => Reads.pure(FinancialServicesInsurance)
      case "forestry" => Reads.pure(Forestry)
      case "humanHealthSocialWork" => Reads.pure(HumanHealthSocialWork)
      case "manufacturing" => Reads.pure(Manufacturing)
      case "miningQuarrying" => Reads.pure(MiningQuarrying)
      case "professionalScientificTechnicalServices" => Reads.pure(ProfessionalScientificTechnicalServices)
      case "publishingBroadcasting" => Reads.pure(PublishingBroadcasting)
      case "realEstate" => Reads.pure(RealEstate)
      case "tTelecommunications" => Reads.pure(Telecommunications)
      case "transportStorage" => Reads.pure(TransportStorage)
      case "waterSupply" => Reads.pure(WaterSupply)
      case "wholesaleRetail" => Reads.pure(WholesaleRetail)
      case "publicAdministration" => Reads.pure(PublicAdministration)
      case "otherService" => Reads.pure(OtherService)
      case "households" => Reads.pure(Households)
      case "activitiesExtraterritorial" => Reads.pure(ActivitiesExtraterritorial)
    }

}
