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

package uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters

import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode
import uk.gov.hmrc.eusubsidycompliancefrontend.models.CurrencyCode.{EUR, GBP}

import java.text.NumberFormat
import java.util.{Currency, Locale}
import scala.math.BigDecimal.RoundingMode

object BigDecimalFormatter {

  private val eurFormatter: NumberFormat = numberFormatForCurrency(EUR)
  private val gbpFormatter: NumberFormat = numberFormatForCurrency(GBP)

  private def numberFormatForCurrency(c: CurrencyCode) = {
    val cf = NumberFormat.getCurrencyInstance(new Locale("en", "GB"))
    cf.setCurrency(Currency.getInstance(c.entryName))
    cf
  }

  val zero: BigDecimal = BigDecimal(0)

  private def roundingMode = RoundingMode.DOWN

  object Syntax:
    extension(b: BigDecimal)

      def toEuros: String =
        eurFormatter.format(b.toRoundedAmount)

      def toPounds: String =
        gbpFormatter.format(b.toRoundedAmount)

      def toRoundedAmount: BigDecimal =
        b.setScale(2, roundingMode)
}
