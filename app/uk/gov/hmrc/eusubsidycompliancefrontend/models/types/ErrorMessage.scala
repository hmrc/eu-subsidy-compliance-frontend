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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.ErrorMessage.ErrorMessage

object ErrorMessage
  extends StringValue[ErrorMessage]:

  opaque type ErrorMessage = String

  private val Regex =
    """.{1,255}""".r

  override val name: String =
    "ErrorMessage"

  override def from(value: String): Option[ErrorMessage] =
    Option(value)
      .map(_.trim)
      .filter(Regex.matches)

  extension (x: ErrorMessage)
    override def value: String = x