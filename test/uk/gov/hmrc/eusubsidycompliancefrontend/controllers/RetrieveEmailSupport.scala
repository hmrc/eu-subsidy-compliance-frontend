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

import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.RetrieveEmailResponse
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.RetrieveEmailService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait RetrieveEmailSupport { this: ControllerSpec =>

  val mockRetrieveEmailService = mock[RetrieveEmailService]

  def mockRetrieveEmail(eori: EORI)(result: Either[Error, RetrieveEmailResponse]) =
    (mockRetrieveEmailService
      .retrieveEmailByEORI(_: EORI)(_: HeaderCarrier))
      .expects(eori, *)
      .returning(result.fold(e => Future.failed(e.value.fold(s => new Exception(s), identity)), Future.successful(_)))
}
