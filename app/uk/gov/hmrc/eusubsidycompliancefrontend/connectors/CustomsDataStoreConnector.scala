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

import com.google.inject.{Inject, Singleton}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{RetrieveEmail, UpdateEmailRequest}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDataStoreConnector @Inject() (override protected val http: HttpClientV2, servicesConfig: ServicesConfig)(
  implicit ec: ExecutionContext
) extends EmailConnector {

  lazy private val cdsURL: String = servicesConfig.baseUrl("cds")

  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val url = url"$cdsURL/customs-data-store/eori/verified-email-third-party"
    val body = Json.toJson(RetrieveEmail(eori))

    http
      .post(url)
      .withBody(body)
      .execute[HttpResponse]
  }

  def updateEmailForEori(eori: EORI, emailAddress: String, timestamp: LocalDateTime = LocalDateTime.now())(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    http
      .post(url"$cdsURL/customs-data-store/update-email")
      .withBody(Json.toJson(UpdateEmailRequest(eori, emailAddress, timestamp)))
      .execute[HttpResponse]
      .map { res =>
        res.status match {
          case Status.NO_CONTENT => ()
          case _ => sys.error(s"Error updating email address for eori: $eori, new email address: $emailAddress")
        }
      }
}
