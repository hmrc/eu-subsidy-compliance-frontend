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

package uk.gov.hmrc.eusubsidycompliancefrontend.connectors

import play.api.http.HeaderNames.ACCEPT
import uk.gov.hmrc.eusubsidycompliancefrontend.models.MonthlyExchangeRate
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class EuropaConnector @Inject() (
  val client: HttpClient
) {
  def retrieveMonthlyExchangeRates(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[MonthlyExchangeRate]] =
    client.GET[Seq[MonthlyExchangeRate]](
      url = "https://ec.europa.eu/budg/inforeuro/api/public/currencies/gbp",
      headers = Seq(ACCEPT -> "application/vnd.sdmx.data+json;version=1.0.0-wd")
    )

}
