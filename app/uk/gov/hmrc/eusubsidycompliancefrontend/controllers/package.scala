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

import play.api.libs.json.{Format, Json, Reads}
import play.api.mvc.Request
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Undertaking
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{Journey, Store}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

package object controllers {

  implicit val undertakingFormat: Format[Undertaking] = Json.format[Undertaking]

  case class FormValues(value: String)

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
