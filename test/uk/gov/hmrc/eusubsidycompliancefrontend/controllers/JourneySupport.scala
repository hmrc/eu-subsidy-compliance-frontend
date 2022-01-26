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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import play.api.libs.json.Reads
import play.api.mvc.Request
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{Journey, JourneyTraverseService}

import scala.concurrent.Future
import scala.reflect.ClassTag

trait JourneySupport { this: ControllerSpec =>

  val mockJourneyTraverseService = mock[JourneyTraverseService]

  def mockGetPrevious[A <: Journey : ClassTag](eori: EORI)(result: Either[Error, Uri]) = {
    (mockJourneyTraverseService
      .getPrevious(_: ClassTag[A], _: EORI, _: Request[_], _: Reads[A]))
      .expects(*, eori, *, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)),Future.successful(_)))
  }

}
