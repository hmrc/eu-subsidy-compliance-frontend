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

import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.HelloWorldPage
import utils.UnsafePersistence

import scala.concurrent.ExecutionContext

class HelloWorldControllerSpec
  extends BaseControllerSpec
{

  private val fakeRequest = FakeRequest("GET", "/")
  private val template = app.injector.instanceOf[HelloWorldPage]

  private val controller = new HelloWorldController(
    stubMessagesControllerComponents(),
    template,
    preAuthenticatedActionBuilders(),
    new UnsafePersistence
  )

  "GET /" should {
    "return 200" in {
      val result = controller.helloWorld(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = controller.helloWorld(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result)     shouldBe Some("utf-8")
    }
  }
}
