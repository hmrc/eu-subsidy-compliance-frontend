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

import play.api.i18n.Messages

final case class NaceNotes(
  desc: Option[String],
  intro: Option[String],
  bullets: Seq[String],
  outro: Option[String]
)

final case class NaceLevel4(
  code: String,
  heading: String,
  notes: NaceNotes
)

object NaceLevel4Catalogue {

  private def msgOpt(key: String)(implicit messages: Messages): Option[String] =
    if (messages.isDefinedAt(key)) Some(messages(key)) else None

  def fromMessages(code: String)(implicit messages: Messages): Option[NaceLevel4] = {
    val base = s"NACE.$code"
    val headingKey = s"$base.heading"

    if (!messages.isDefinedAt(headingKey)) {
      None
    } else {
      val heading = messages(headingKey)
      val desc = msgOpt(s"$base.desc")
      val intro = msgOpt(s"$base.intro")
      val outro = msgOpt(s"$base.outro")
      val bullets: List[String] =
        Iterator
          .from(1)
          .map(i => s"$base.b$i")
          .takeWhile(messages.isDefinedAt)
          .map(key => messages(key))
          .toList

      Some(NaceLevel4(code, heading, NaceNotes(desc, intro, bullets, outro)))
    }
  }
}
