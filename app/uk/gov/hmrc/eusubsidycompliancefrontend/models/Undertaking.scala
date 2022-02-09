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

import play.api.libs.json.{Json, OFormat}

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
)

object Undertaking {
  implicit val undertakingFormat: OFormat[Undertaking] = Json.format[Undertaking]

  implicit class UndertakingOps(private val undertaking: Undertaking) extends AnyVal {

    //Check if the eori entered is Lead EORI or not
    def isLeadEORI(eori: EORI): Boolean = {
      val leadEORI: BusinessEntity = undertaking
        .undertakingBusinessEntity
        .filter(_.leadEORI)
        .headOption
        .getOrElse(throw new IllegalStateException("Missing Lead EORI"))
    leadEORI.businessEntityIdentifier.toString == eori.toString
    }

    //Fetches the Business entity for the given EORI
    def getBusinessEntityByEORI(eori: EORI): BusinessEntity = {
      undertaking.undertakingBusinessEntity
        .filter(be => be.businessEntityIdentifier == eori)
        .headOption
        .getOrElse(throw new IllegalStateException(s"BE with eori $eori is missing"))
    }
  }

}
