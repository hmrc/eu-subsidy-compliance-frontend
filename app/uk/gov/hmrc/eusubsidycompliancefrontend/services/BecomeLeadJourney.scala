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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.BecomeLeadJourney.FormUrls

case class BecomeLeadJourney(
  becomeLeadEori: FormPage[Boolean] = FormPage(FormUrls.BecomeLead),
  acceptTerms: FormPage[Boolean] = FormPage(FormUrls.TermsAndConditions),
  confirmation: FormPage[Boolean] = FormPage(FormUrls.Confirmation)
) extends Journey {

  override def steps: List[FormPage[_]] =
    List(
      becomeLeadEori,
      acceptTerms,
      confirmation
    )

}

object BecomeLeadJourney {
  import Journey._

  implicit val format: Format[BecomeLeadJourney] = Json.format[BecomeLeadJourney]

  object FormUrls {

    val BecomeLead = "become-lead-eori"
    val TermsAndConditions = "accept-promote-to-lead-terms"
    val Confirmation = "lead-promotion-confirmation"
  }

}
