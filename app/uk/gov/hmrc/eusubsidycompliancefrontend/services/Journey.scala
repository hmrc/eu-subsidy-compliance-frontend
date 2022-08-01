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

import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

trait FormPage[+T] {
  val value: Journey.Form[T]
  def uri: Uri

  def isCurrentPage(implicit r: Request[_]): Boolean = r.uri == uri
}

trait Journey {

  def steps: Array[FormPage[_]]

  def previous(implicit r: Request[_]): Journey.Uri =
    if (currentIndex > 0) steps(previousIndex).uri
    else throw new IllegalStateException("no previous page")

  def next(implicit r: Request[_]): Future[Result] =
    if (currentIndex < lastIndex) redirectWithSession(steps(nextIndex).uri).toFuture
    else throw new IllegalStateException("no next page")

  def isComplete: Boolean = steps(lastIndex).value.isDefined

  def firstEmpty(implicit request: Request[_]): Option[Result] =
    steps
      .find(_.value.isEmpty)
      .map(e => redirectWithSession(e.uri))

  def getStepWithPath(path: String): Option[FormPage[_]] =
    steps
      .find(_.uri == path)

  def isEligibleForStep(implicit r: Request[_]): Boolean =
    if (previousIndex < 0) true
    else steps(previousIndex).value.nonEmpty

  private def currentIndex(implicit r: Request[_]): Int = steps.indexWhere(e => r.uri == e.uri)
  private def previousIndex(implicit r: Request[_]) = currentIndex - 1
  private def nextIndex(implicit r: Request[_]) = currentIndex + 1
  private def lastIndex = steps.length - 1

  def redirectWithSession(u: String)(implicit r: Request[_]): Result = Redirect(u).withSession(r.session)
}

object Journey {
  type Form[+T] = Option[T]
  type Uri = String
}
