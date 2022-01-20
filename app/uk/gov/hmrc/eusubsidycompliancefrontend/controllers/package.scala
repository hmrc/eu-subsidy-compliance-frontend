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

package uk.gov.hmrc.eusubsidycompliancefrontend

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.json.{Format, Json, Reads}
import play.api.mvc.Request
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{ContactDetails, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, PhoneNumber}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{Journey, Store}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

package object controllers {

  implicit val undertakingFormat: Format[Undertaking] = Json.format[Undertaking]

  case class FormValues(value: String)

  case class OneOf(a: Option[String], b: Option[String]){
    def toContactDetails =
      ContactDetails(
        a.map(PhoneNumber(_)),
        b.map(PhoneNumber(_))
      )
  }

  val contactForm: Form[OneOf] = Form(
    mapping(
      "phone" -> optional(text),
      "mobile"  -> optional(text)
    )(OneOf.apply)(OneOf.unapply).verifying(
      "one.or.other.mustbe.present",
      fields => fields match {
        case OneOf(Some(_), Some(_)) => true
        case OneOf(_, Some(_)) => true
        case OneOf(Some(_),_) => true
        case _ => false
      }
    ).verifying(
      "phone.regex.error",
      fields => fields match {
        case OneOf(Some(a),_) if !a.matches(PhoneNumber.regex) => false
        case _ => true
      }
    ).verifying(
      "mobile.regex.error",
      fields => fields match {
        case OneOf(_,Some(b)) if !b.matches(PhoneNumber.regex) => false
        case _ => true
      }
    )
  )

  case class OptionalEORI(
    setValue: String,
    value: Option[String]
  )

  case class OptionalTraderRef(
     setValue: String,
     value: Option[String]
   )

  def getPrevious[A <: Journey : ClassTag](
    store: Store
  )(
    implicit eori: EORI,
    request: Request[_],
    reads: Reads[A],
    executionContext: ExecutionContext
  ): Future[Uri] = {
    val journeyType = implicitly[ClassTag[A]].runtimeClass.getSimpleName
    store.get[A].map { x =>
      x.fold(throw new IllegalStateException(s"$journeyType should be there")) { y =>
        y.previous
      }
    }
  }
}
