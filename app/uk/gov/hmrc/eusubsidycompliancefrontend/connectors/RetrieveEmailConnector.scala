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

package uk.gov.hmrc.eusubsidycompliancefrontend.connectors

import com.google.inject.{ImplementedBy, Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[RetrieveEmailConnectorImpl])
trait RetrieveEmailConnector {

  def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]

}

@Singleton
class RetrieveEmailConnectorImpl @Inject() (http: HttpClient, servicesConfig: ServicesConfig)(implicit
  ec: ExecutionContext
) extends RetrieveEmailConnector {

  val cdsURL: String = servicesConfig.baseUrl("cds")

  def getUri(eori: EORI)                                                                                        = s"$cdsURL/customs-data-store/eori/${eori.toString}/verified-email"
  override def retrieveEmailByEORI(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    http
      .GET[HttpResponse](getUri(eori))
      .map(Right(_))
      .recover { case e =>
        Left(Error(e))
      }
}
