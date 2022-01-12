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

package uk.gov.hmrc.eusubsidycompliancefrontend.actions

import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers.stubMessagesControllerComponents
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.EscAuthRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI

import scala.concurrent.{ExecutionContext, Future}

trait AuthenticatedActionBuildersSpec extends MockitoSugar
   {
  implicit lazy val ec:           ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  def preAuthenticatedActionBuilders(): EscActionBuilders = new EscActionBuilders(mock[EscRequestActionBuilder]) {
    override val escAuthentication: ActionBuilder[EscAuthRequest, AnyContent] =
      new ActionBuilder[EscAuthRequest, AnyContent] {
        override def parser: BodyParser[AnyContent] = stubMessagesControllerComponents().parsers.anyContent

        override def invokeBlock[A](request: Request[A], block: EscAuthRequest[A] => Future[Result]): Future[Result] =
          block(EscAuthRequest("testAuthorityId", "testGroupId", request, EORI("GB123456789012")))

        override protected def executionContext: ExecutionContext = ec
      }
  }
}