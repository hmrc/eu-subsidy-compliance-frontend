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

import com.codahale.metrics.SharedMetricRegistries
import com.google.inject.{Inject, Singleton}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api._
import play.api.http.HttpConfiguration
import play.api.i18n._
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.eusubsidycompliancefrontend.controllers.UndertakingControllerSpec.SectorRadioOption
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.PlaySupport

import scala.concurrent.Future

trait ControllerSpec extends PlaySupport with ScalaFutures with IntegrationPatience {

  SharedMetricRegistries.clear()

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
    expectedStatus: Int = BAD_REQUEST,
    backLinkOpt: Option[String] = None
  ): Unit =
    checkPageIsDisplayed(
      result,
      expectedTitle,
      { doc =>
        doc.select(".govuk-error-summary").select("a").text() shouldBe formError
        doc.select(".govuk-error-message").text() shouldBe s"Error: $formError"
        backLinkOpt.map(backLink => doc.getElementById("back-link").attr("href") shouldBe backLink)
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

  def testRadioButtonOptions(doc: Document, expectedRadioOptionsTexts: List[SectorRadioOption]) = {
    val radioOptions = doc.select(".govuk-radios__item")
    radioOptions.size shouldBe expectedRadioOptionsTexts.size
    expectedRadioOptionsTexts.map { option =>
      doc.getElementById(s"sector-label-${option.sector}").text shouldBe option.label
      doc.getElementById(s"sector-hint-${option.sector}").text shouldBe option.hint
    }
  }

  def verifyScp08Banner(document: Document) = {
    document.getElementById("scp08-maintenance-banner") should not be null
    document
      .getElementById("scp08-banner-p1")
      .text shouldBe "Please be aware we are currently experiencing some technical difficulties with the Customs Duty Waiver Scheme service, causing some traders to see a miscalculation of the undertaking balance. We are working urgently to resolve but if you are still within balance on the system and your records you can continue to submit supplementary declarations as normal and we will rectify the online balance. We apologise for any inconvenience caused."
    document
      .getElementById("scp08-banner-p2")
      .text shouldBe "If you are experiencing issues preventing you from submitting your supplementary declaration then please contact - customs.duty-waivers@hmrc.gov.uk"
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
