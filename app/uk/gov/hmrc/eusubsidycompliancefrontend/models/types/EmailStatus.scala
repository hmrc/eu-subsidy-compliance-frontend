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

import play.api.libs.json.*
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

enum EmailStatus(val wire: String):
  case Unverified extends EmailStatus("unverified")
  case New extends EmailStatus("new")
  case Amend extends EmailStatus("amend")
  case BecomeLead extends EmailStatus("become-lead")
  case CYA extends EmailStatus("cya")

object EmailStatus:

  private val byWire: Map[String, EmailStatus] =
    EmailStatus.values.map(s => s.wire -> s).toMap

  def withName(name: String): Option[EmailStatus] =
    byWire.get(name)

  /** JSON Format
    */
  given Format[EmailStatus] = new Format[EmailStatus]:

    override def reads(json: JsValue): JsResult[EmailStatus] =
      json match
        case JsString(v) =>
          EmailStatus.withName(v) match
            case Some(s) => JsSuccess(s)
            case None => JsError(s"Unknown EmailStatus: $v")

        case other =>
          JsError("Expected string EmailStatus")

    override def writes(o: EmailStatus): JsValue =
      JsString(o.wire)

  /** PathBindable
    */
  given PathBindable[EmailStatus] with

    override def bind(key: String, value: String): Either[String, EmailStatus] =
      EmailStatus
        .withName(value)
        .toRight(s"Cannot parse parameter '$key' as EmailStatus")

    override def unbind(key: String, value: EmailStatus): String =
      value.wire

  /** QueryStringBindable
    */
  given QueryStringBindable[EmailStatus] with

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, EmailStatus]] =
      params.get(key).collect { case Seq(value) =>
        EmailStatus.withName(value).toRight(s"Invalid EmailStatus: $value")
      }

    override def unbind(key: String, value: EmailStatus): String =
      value.wire
