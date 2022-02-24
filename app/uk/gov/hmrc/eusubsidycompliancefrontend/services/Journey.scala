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

import play.api.Logger
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

case class FormPage[+T](
  uri: Journey.Uri,
  value: Journey.Form[T] = None
)

// TODO - this needs another pass. Potential scope for simplifying previous/next handling
trait Journey {

  private val logger = Logger(getClass)

  protected def steps: List[FormPage[_]]

  def formPages: List[FormPage[_]] = steps

  // TODO strip/add the server path prefix instead of endsWith
  // TODO especially as this may not work for listings
  def currentIndex(implicit request: Request[_]): Int =
    formPages.indexWhere(x => request.uri.endsWith(x.uri))

  // TODO - should previous and next return the same types?
  def previous(implicit request: Request[_]): Journey.Uri =
    formPages
      .zipWithIndex
      .find(_._2 == currentIndex - 1)
      .fold(throw new IllegalStateException("no previous page"))(_._1.uri)

  def next(implicit request: Request[_]): Future[Result] =
    formPages
      .zipWithIndex
      .find(_._2 == currentIndex + 1)
      .fold(throw new IllegalStateException("no next page")) { x =>
        Redirect(x._1.uri).withSession(request.session).toFuture
      }

  def isEmptyFormPage(indexedFormPage: (FormPage[_],Int))(implicit request: Request[_]): Boolean =
    indexedFormPage match {
      case (formPage, index) => formPage.value.isEmpty && index < currentIndex
    }

  def isComplete:Boolean = steps.last.value.isDefined

  def redirect(implicit request: Request[AnyContent]): Option[Future[Result]] = {
    val firstUnfilledFormUri: Journey.Uri =
      formPages
        .zipWithIndex
        .find(isEmptyFormPage)
        .fold(request.uri)({case (a,_) => a.uri})
    if (firstUnfilledFormUri != request.uri) {
      logger.info(s"redirecting ${request.uri} to $firstUnfilledFormUri")
      Some(Redirect(firstUnfilledFormUri).withSession(request.session).toFuture)
    }
    else None
  }

  def firstEmpty(implicit request: Request[_]): Option[Result] =
    formPages.find(x => x.value.isEmpty).map { x =>
      val uri = x.uri
      Redirect(uri).withSession(request.session)
    }

}

object Journey {

  type Form[+T] = Option[T]
  type Uri = String

  implicit val formPageBigDecimalFormat: OFormat[FormPage[BigDecimal]] = Json.format[FormPage[BigDecimal]]
  implicit val formPageBooleanValueFormat: OFormat[FormPage[Boolean]] = Json.format[FormPage[Boolean]]
  implicit val formPageIntFormat: OFormat[FormPage[Int]] = Json.format[FormPage[Int]]
  implicit val formPageStringValueFormat: OFormat[FormPage[String]] = Json.format[FormPage[String]]

}
