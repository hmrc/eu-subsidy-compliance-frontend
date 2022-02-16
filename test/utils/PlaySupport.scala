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

package utils

import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.{Application, Configuration, Play}
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.TestMessagesApiProvider

import scala.reflect.ClassTag

trait PlaySupport extends AnyWordSpec with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val lang: Lang = Lang("en")

  def overrideBindings: List[GuiceableModule] = List.empty[GuiceableModule]

  def additionalConfig: Configuration = Configuration()

  private val defaultConfiguration = Map(
    "microservice.metrics.graphite.enabled" -> false,
  )

  def buildFakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(Configuration.from(defaultConfiguration).withFallback(additionalConfig))
      .overrides(overrideBindings: _*)
      .overrides(bind[MessagesApi].toProvider[TestMessagesApiProvider])
      .build()

  lazy val fakeApplication: Application = buildFakeApplication()

  lazy val appConfig: AppConfig = instanceOf[AppConfig]

  // TODO - consider replacing this with running(app) { } in tests
  abstract override def beforeAll(): Unit = {
    Play.start(fakeApplication)
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    Play.stop(fakeApplication)
    super.afterAll()
  }

  implicit lazy val messagesApi = instanceOf[MessagesApi]

  implicit lazy val messages = MessagesImpl(lang, messagesApi)

  def instanceOf[A : ClassTag]: A = fakeApplication.injector.instanceOf[A]

}
