/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.eusubsidycompliancefrontend.config

import org.scalatest.matchers.should.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate
import uk.gov.hmrc.eusubsidycompliancefrontend.util.IntegrationBaseSpec

class AppConfigSpec extends IntegrationBaseSpec with Matchers {

  "AppConfig" when {
    "all EmailTemplate values are present in default app configuration" in {
      val app = new GuiceApplicationBuilder().build()
      running(app) {
        val underTest = app.injector.instanceOf[AppConfig]
        EmailTemplate.values.foreach { template =>
          underTest.getTemplateId(template) shouldBe defined
        }
      }
    }
  }

}
