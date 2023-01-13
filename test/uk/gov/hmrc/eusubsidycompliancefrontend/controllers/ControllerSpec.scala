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

package uk.gov.hmrc.eusubsidycompliancefrontend.controllers

import com.google.inject.{Inject, Singleton}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.http.HttpConfiguration
import play.api.i18n._
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import play.api._
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.PlaySupport

import scala.concurrent.Future

trait ControllerSpec extends PlaySupport with ScalaFutures with IntegrationPatience {

  def checkIsRedirect(result: Future[Result], expectedRedirectLocation: String): Unit = {
    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(expectedRedirectLocation)
  }

  def checkIsRedirect(result: Future[Result], expectedRedirectLocation: Call): Unit =
    checkIsRedirect(result, expectedRedirectLocation.url)

  def messageFromMessageKey(messageKey: String, args: Any*)(implicit messagesApi: MessagesApi): String = {
    val m = messagesApi(messageKey, args: _*)
    if (m === messageKey) sys.error(s"messageFromMessageKey: Could not find message for key `$messageKey`")
    else m
  }

  def checkPageIsDisplayed(
    result: Future[Result],
    expectedTitle: String,
    contentChecks: Document => Unit = _ => (),
    expectedStatus: Int = OK,
    isLeadJourney: Boolean = false
  ): Unit = {
    (status(result) -> redirectLocation(result)) shouldBe (expectedStatus -> None)

    val doc = Jsoup.parse(contentAsString(result))

    if (!isLeadJourney)
      withClue(s"Title '$expectedTitle' not found in page content '${doc.text()}'") {
        doc.text().contains(expectedTitle) shouldBe true
      }

    val bodyText = doc.select("body").text
    val regex = """not_found_message\((.*?)\)""".r

    val regexResult = regex.findAllMatchIn(bodyText).toList
    if (regexResult.nonEmpty) fail(s"Missing message keys: ${regexResult.map(_.group(1)).mkString(", ")}")

    contentChecks(doc)
  }

  def checkFormErrorIsDisplayed(
    result: Future[Result],
    expectedTitle: String,
    formError: String,
    expectedStatus: Int = BAD_REQUEST
  ): Unit =
    checkPageIsDisplayed(
      result,
      expectedTitle,
      { doc =>
        doc.select(".govuk-error-summary").select("a").text() shouldBe formError
        doc.select(".govuk-error-message").text() shouldBe s"Error: $formError"
      },
      expectedStatus
    )

  def checkFormErrorsAreDisplayed(
    result: Future[Result],
    expectedTitle: String,
    formErrors: List[String],
    expectedStatus: Int = BAD_REQUEST
  ): Unit =
    checkPageIsDisplayed(
      result,
      expectedTitle,
      { doc =>
        val errorSummary = doc.select(".govuk-error-summary")
        errorSummary.select("a").text() shouldBe formErrors.head

        val inputErrorMessages = doc.select(".govuk-error-message").text()
        inputErrorMessages shouldBe formErrors.map(e => s"Error: $e").mkString(" ")
      },
      expectedStatus
    )

  def testRadioButtonOptions(doc: Document, expectedRadioOptionsTexts: List[String]) = {
    val radioOptions = doc.select(".govuk-radios__item")
    radioOptions.size shouldBe expectedRadioOptionsTexts.size
    expectedRadioOptionsTexts.zipWithIndex.map({ case (text, i) =>
      radioOptions.get(i).text() shouldBe text
    })
  }

}

@Singleton
class TestMessagesApiProvider @Inject() (
  environment: Environment,
  config: Configuration,
  langs: Langs,
  httpConfiguration: HttpConfiguration
) extends DefaultMessagesApiProvider(environment, config, langs, httpConfiguration) {

  val logger = Logger(this.getClass)

  override lazy val get: MessagesApi =
    new DefaultMessagesApi(
      loadAllMessages,
      langs,
      langCookieName,
      langCookieSecure,
      langCookieHttpOnly,
      langCookieSameSite,
      httpConfiguration,
      langCookieMaxAge
    ) {
      override protected def noMatch(key: String, args: Seq[Any])(implicit lang: Lang): String =
        sys.error(s"Could not find message for key: $key ${args.mkString("-")}")
    }

}
