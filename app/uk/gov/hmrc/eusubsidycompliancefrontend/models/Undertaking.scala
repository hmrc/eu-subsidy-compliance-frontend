/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types._

import java.time.LocalDate

case class Undertaking(
  reference: UndertakingRef,
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

  def hasEORI(eori: EORI): Boolean =
    undertakingBusinessEntity
      .exists(_.businessEntityIdentifier == eori)

  def getBusinessEntityByEORI(eori: EORI): BusinessEntity =
    findBusinessEntity(eori)
      .getOrElse(throw new NoSuchElementException("No business entity found for given eori"))

  def findBusinessEntity(eori: EORI): Option[BusinessEntity] =
    undertakingBusinessEntity.find(_.businessEntityIdentifier == eori)

  def getAllNonLeadEORIs: List[EORI] =
    undertakingBusinessEntity.filter(!_.leadEORI).map(_.businessEntityIdentifier)

  def getLeadEORI: EORI = undertakingBusinessEntity
    .filter(_.leadEORI)
    .map(_.businessEntityIdentifier)
    .headOption
    .getOrElse(throw new IllegalStateException(s"Lead EORI is missing"))

  def getLeadBusinessEntity: BusinessEntity = getBusinessEntityByEORI(getLeadEORI)
}

object Undertaking {
  implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]
}
