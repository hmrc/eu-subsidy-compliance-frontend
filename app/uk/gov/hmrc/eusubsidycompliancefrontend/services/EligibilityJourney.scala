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

import cats.implicits._
import play.api.libs.json._
import shapeless.syntax.std.tuple._
import shapeless.syntax.typeable._

case class EligibilityJourney(
  customsWaivers: FormPage[Boolean] = FormPage("do-you-claim-customs-waivers"),
  willYouClaim: FormPage[Boolean] = FormPage("will-you-claim-customs-waivers"),
  notEligible: FormPage[Boolean] = FormPage("not-eligible"),
  mainBusinessCheck: FormPage[Boolean] = FormPage("main-business-check"),
  signOut: FormPage[Boolean] = FormPage("not-eligible-to-lead"),
  acceptTerms: FormPage[Boolean] = FormPage("terms-conditions"),
  eoriCheck: FormPage[Boolean] = FormPage("eoricheck"),
  signOutBadEori: FormPage[Boolean] = FormPage("incorrect-eori"),
  createUndertaking: FormPage[Boolean] = FormPage("create-undertaking")
) extends Journey {

  override def steps: List[Option[FormPage[_]]] =
    EligibilityJourney
      .unapply(this)
      .map(_.toList)
      .fold(List.empty[Any])(identity)
      .map(_.cast[FormPage[_]])
      .filterNot(x => x.fold(false) { y =>
        y.uri === "will-you-claim-customs-waivers" &&
          this.customsWaivers.value.getOrElse(false)
      })
      .filterNot(x => x.fold(false) { y =>
        y.uri === "not-eligible" &&
          (this.customsWaivers.value.getOrElse(false) || this.willYouClaim.value.getOrElse(false))
      })
      .filterNot(x => x.fold(false) { y =>
        y.uri === "not-eligible-to-lead" &&
          this.mainBusinessCheck.value.getOrElse(false)
      })
      .filterNot(x => x.fold(false) { y =>
        y.uri === "incorrect-eori" &&
          this.eoriCheck.value.getOrElse(false)
      })

}

object EligibilityJourney {
  import Journey._ // N.B. don't let intellij delete this
  implicit val format: Format[EligibilityJourney] = Json.format[EligibilityJourney]
}


