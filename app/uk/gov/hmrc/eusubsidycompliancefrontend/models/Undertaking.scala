/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.implicits.catsSyntaxOptionId
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsObject, JsResult, JsSuccess, JsValue, Json, OFormat}

import java.time.LocalDate
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types._

case class Undertaking(
  reference: Option[UndertakingRef],
  name: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: Option[IndustrySectorLimit],
  lastSubsidyUsageUpdt: Option[LocalDate],
  undertakingBusinessEntity: List[BusinessEntity]
) {

  def isLeadEORI(eori: EORI): Boolean = {
    val leadEORI: BusinessEntity = undertakingBusinessEntity
      .find(_.leadEORI)
      .getOrElse(throw new IllegalStateException("Missing Lead EORI"))

    leadEORI.businessEntityIdentifier == eori
  }

  def getBusinessEntityByEORI(eori: EORI): BusinessEntity =
    undertakingBusinessEntity
      .find(be => be.businessEntityIdentifier == eori)
      .getOrElse(throw new IllegalStateException(s"BE with eori $eori is missing"))

  def getAllNonLeadEORIs(): List[EORI] =
    undertakingBusinessEntity.filter(!_.leadEORI).map(_.businessEntityIdentifier)

  def getLeadEORI = undertakingBusinessEntity
    .filter(_.leadEORI)
    .map(_.businessEntityIdentifier)
    .headOption
    .getOrElse(throw new IllegalStateException(s"Lead EORI is missing"))
}

object Undertaking {
  implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
  implicit val undertakingOptFormat: OFormat[Option[Undertaking]] = new OFormat[Option[Undertaking]] {
    override def reads(json: JsValue): JsResult[Option[Undertaking]] =
      (json \ "name")
        .validateOpt[UndertakingName]
        .flatMap {
          case Some(_) =>
            JsSuccess(
              Undertaking(
                (json \ "reference").asOpt[UndertakingRef],
                (json \ "name").as[UndertakingName],
                (json \ "industrySector").as[Sector],
                (json \ "industrySectorLimit").asOpt[IndustrySectorLimit],
                (json \ "lastSubsidyUsageUpdt").asOpt[LocalDate],
                (json \ "undertakingBusinessEntity").as[List[BusinessEntity]]
              ).some
            )

          case None => JsSuccess(None)
        }

    override def writes(o: Option[Undertaking]): JsObject = o match {
      case Some(u) => Json.writes[Undertaking].writes(u)
      case None => JsObject.empty
    }
  }
}
