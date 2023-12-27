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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{IndustrySectorLimit, UndertakingName}

case class UndertakingCreateWithSectorLimit(
  name: UndertakingName,
  industrySector: Sector,
  industrySectorLimit: IndustrySectorLimit,
  undertakingBusinessEntity: List[BusinessEntity]
)
object UndertakingCreateWithSectorLimit {

  implicit val writeableUndertakingFormat: OFormat[UndertakingCreateWithSectorLimit] =
    Json.format[UndertakingCreateWithSectorLimit]
  def apply(undertakingCreate: UndertakingCreate, sectorCap: IndustrySectorLimit): UndertakingCreateWithSectorLimit =
    UndertakingCreateWithSectorLimit(
      name = undertakingCreate.name,
      industrySector = undertakingCreate.industrySector,
      industrySectorLimit = sectorCap,
      undertakingBusinessEntity = undertakingCreate.undertakingBusinessEntity
    )

}
