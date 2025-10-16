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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.models

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector.Sector

case class NaceCheckDetailsViewModel(
  naceLevel1Display: String,
  naceLevel1_1Display: String,
  naceLevel2Display: String,
  naceLevel3Display: String,
  naceLevel4Display: String,
  naceLevel1Code: String,
  naceLevel2Code: String,
  naceLevel3Code: String,
  naceLevel4Code: String,
  sector: Sector,
  changeSectorUrl: String,
  changeLevel1Url: String,
  changeLevel1_1Url: String,
  changeLevel2Url: String,
  changeLevel3Url: String,
  changeLevel4Url: String,
  showLevel1: Boolean,
  showLevel1_1: Boolean,
  showLevel2: Boolean,
  showLevel3: Boolean,
  showLevel4: Boolean
)
