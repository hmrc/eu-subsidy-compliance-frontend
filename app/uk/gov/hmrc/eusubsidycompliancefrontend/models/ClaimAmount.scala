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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable

sealed abstract class CurrencyCode(val symbol: Char) extends EnumEntry

object CurrencyCode extends Enum[CurrencyCode] with PlayJsonEnum[CurrencyCode] {

  case object GBP extends CurrencyCode('£')
  case object EUR extends CurrencyCode('€')

  override def values: immutable.IndexedSeq[CurrencyCode] = findValues

}

// TODO - provide a toEuros method to do the conversion with a given exchange rate.
// TODO - would potentially be nicer to have amount as a BigDecimal if form bindings work ok
case class ClaimAmount(currencyCode: CurrencyCode, amount: String)

object ClaimAmount {
  implicit val claimAmountFormat: OFormat[ClaimAmount] = Json.format[ClaimAmount]
}