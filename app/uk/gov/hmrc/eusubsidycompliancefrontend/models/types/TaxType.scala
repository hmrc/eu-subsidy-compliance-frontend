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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.TaxType.TaxType

object TaxType extends StringValue[TaxType]:

  opaque type TaxType = String

  private val regex = """.{0,3}""".r

  override val name: String = "TaxType"

  def apply(taxType: String): TaxType = taxType

  override def validate(value: String): TaxType =
    Option(value)
      .map(_.trim)
      .filter(regex.findFirstIn(_).isDefined)
      .map(apply)
      .getOrElse(throw new IllegalArgumentException(s"$value is not a valid TaxType"))

  extension (x: TaxType) override def value: String = x
