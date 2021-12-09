/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

object journey {
  type Form[+T] = Option[T]
  type Uri = String
}

case class FormPage[+T](
  uri: journey.Uri,
  value: journey.Form[T] = None
)

trait Journey {

  private val logger = Logger(getClass)

  def steps: List[Option[FormPage[_]]]

  val formPages: List[FormPage[_]] =
    steps
      .filter(_.nonEmpty)
      .flatten

  // TODO strip/add the server path prefix instead of endsWith
  def currentIndex(implicit request: Request[_]): Int =
    formPages.indexWhere(x => request.uri.endsWith(x.uri))

  def previous(implicit request: Request[_]): journey.Uri =
    formPages
      .zipWithIndex
      .find(_._2 == currentIndex -1)
      .fold(throw new IllegalArgumentException()){
        _._1.uri
      }

  def next(implicit request: Request[_]): Future[Result] =
    formPages
      .find(_.value.isEmpty)
      .map { fp =>
        Future.successful(
          Redirect(fp.uri)
            .withSession(request.session))
      }.getOrElse(throw new IllegalStateException(""))

  def isEmptyFormPage(
    indexedFormPage: (FormPage[_],Int)
  )(
    implicit request: Request[_]
  ): Boolean = indexedFormPage match {
    case (formPage, index) =>
      formPage.value.isEmpty && index < currentIndex
  }

  def redirect(implicit request: Request[AnyContent]): Option[Future[Result]] = {
    val firstUnfilledFormUri: journey.Uri =
      formPages
        .zipWithIndex
        .find(isEmptyFormPage)
        .fold(request.uri)({case (a,_) => a.uri})
    if (firstUnfilledFormUri != request.uri) {
      logger.info(s"redirecting ${request.uri} to $firstUnfilledFormUri")
      Some(Future.successful(Redirect(firstUnfilledFormUri).withSession(request.session)))
    }
    else None
  }

}
