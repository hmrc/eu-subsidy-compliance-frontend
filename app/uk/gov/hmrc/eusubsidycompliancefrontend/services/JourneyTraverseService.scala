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

package uk.gov.hmrc.eusubsidycompliancefrontend.services

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.libs.json.Reads
import play.api.mvc.Request
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@ImplementedBy(classOf[JourneyTraverseServiceImpl])
trait JourneyTraverseService {

  def getPrevious[A <: Journey : ClassTag](implicit eori: EORI, request: Request[_], reads: Reads[A]): Future[Uri]

}

@Singleton
class JourneyTraverseServiceImpl @Inject() (store: Store)(implicit ec: ExecutionContext) extends JourneyTraverseService {
  override def getPrevious[A <: Journey : ClassTag](implicit eori: EORI, request: Request[_], reads: Reads[A]): Future[Uri]  =
    {
      val journeyType: Uri = implicitly[ClassTag[A]].runtimeClass.getSimpleName
      store.get[A].map { opt =>
        opt.fold(throw new IllegalStateException(s"$journeyType should be there")) { value =>
          value.previous
        }
      }
    }
}
