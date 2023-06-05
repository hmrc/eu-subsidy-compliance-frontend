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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millisecond, Second, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.http.HeaderCarrier

abstract class IntegrationBaseSpec
    extends AnyWordSpec
    with ScalaFutures
    with DefaultAwaitTimeout
    with Matchers
    with BeforeAndAfter
    with IntegrationPatience {

  before {
    SharedMetricRegistries.clear()
  }

  //Default polls every 400 millis so stalls tests
  override implicit val patienceConfig: PatienceConfig = {
    PatienceConfig(Span(15, Seconds), Span(1, Millisecond))
  }

  protected implicit val headerCarrier: HeaderCarrier = new HeaderCarrier()

}
