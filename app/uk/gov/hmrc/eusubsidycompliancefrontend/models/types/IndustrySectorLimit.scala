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

import IndustrySectorLimit.IndustrySectorLimit
import play.api.libs.json.Format

object IndustrySectorLimit:

  opaque type IndustrySectorLimit = BigDecimal
  
  def from(value: BigDecimal): IndustrySectorLimit =
    Option(value).filter { x =>
      x <= MaxInputValue &&
        x.scale <= 2
    }
      .getOrElse(throw new IllegalArgumentException(s"$value is not a valid IndustrySectorLimit"))

  extension (x: IndustrySectorLimit)
    def value: BigDecimal = x

  given Format[IndustrySectorLimit] =
    BigDecimalCodec.format(
      "IndustrySectorLimit",
      from,
      _.value
    )