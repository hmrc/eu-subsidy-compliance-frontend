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
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingRef}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, Error, Undertaking}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[EscConnector1Impl])
trait EscConnector1 {
  def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
  def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]

}

@Singleton
class EscConnector1Impl @Inject()(http: HttpClient, servicesConfig: ServicesConfig)(implicit
                                                                                    ec: ExecutionContext
) extends EscConnector1 {

  val logger: Logger = Logger(this.getClass)

  val escURL: String = servicesConfig.baseUrl("esc")
  val createUndertakingPath = "eu-subsidy-compliance/undertaking"
  val retrieveUndertakingPath = "eu-subsidy-compliance/undertaking/"
  val addMemberPath = "eu-subsidy-compliance/undertaking/member"
  val removeMemberPath = "eu-subsidy-compliance/undertaking/member/remove"



  override def createUndertaking(undertaking: Undertaking)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    val createUndertakingUrl = s"$escURL/$createUndertakingPath"
    http.
      POST[Undertaking, HttpResponse](createUndertakingUrl, undertaking)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  override def retrieveUndertaking(eori: EORI)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    {
      val retrieveUndertakingUrl = s"$escURL/$retrieveUndertakingPath$eori"
      http.
        GET[HttpResponse](retrieveUndertakingUrl)
        .map(Right(_))
        .recover {
          case _: NotFoundException => Right(HttpResponse(NOT_FOUND, ""))
          case ex => Left(Error(ex))
        }
    }
  }

  override def addMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
   val addMemberUrl = s"$escURL/$addMemberPath/$undertakingRef"
    http
      .POST[BusinessEntity, HttpResponse](addMemberUrl, businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }

  override def removeMember(undertakingRef: UndertakingRef, businessEntity: BusinessEntity)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] = {
    val removeMemberUrl = s"$escURL/$removeMemberPath/$undertakingRef"
    http
      .POST[BusinessEntity, HttpResponse](removeMemberUrl, businessEntity)
      .map(Right(_))
      .recover {
        case e => Left(Error(e))
      }
  }
}

