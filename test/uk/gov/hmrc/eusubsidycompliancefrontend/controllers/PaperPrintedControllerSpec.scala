/*
 * Copyright 2025 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.TableDrivenPropertyChecks._

class PaperPrintedControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    inject.bind[AuthConnector].toInstance(mockAuthConnector),
    inject.bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[PaperPrintedController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val corrugatedPaperBoardAndContainers = "17.21"
    val sanitaryGoods = "17.22"
    val paperStationery = "17.23"
    val wallpaper = "17.24"
    val otherPaper = "17.25"
    val pulp = "17.1"
    val articlesPaperBoard = "17.2"
    val printingService = "18.1"
    val recordedMediaReproduction4 = "18.20"
    val bindingServices = "18.14"
    val preMediaServices = "18.13"
    val newspapersPrinting = "18.11"
    val otherPrinting = "18.12"
    val pulp4 = "17.11"
    val paper = "17.12"

  }

  import SectorCodes._

  "PaperPrintedController" should {
    "loadArticlesPaperPaperboardLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArticlesPaperPaperboardLvl4Page()(
            FakeRequest(GET, routes.PaperPrintedController.loadArticlesPaperPaperboardLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-corrugated", "Corrugated paper, paperboard and containers of paper and paperboard"),
          ("sector-label-sanitary", "Household and sanitary goods and of toilet requisites"),
          ("sector-label-stationary", "Paper stationery"),
          ("sector-label-wallpaper", "Wallpaper"),
          ("sector-label-other", "Other articles of paper and paperboard")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitArticlesPaperPaperboardLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (corrugatedPaperBoardAndContainers, navigator.nextPage(corrugatedPaperBoardAndContainers, "").url),
          (sanitaryGoods, navigator.nextPage(sanitaryGoods, "").url),
          (paperStationery, navigator.nextPage(paperStationery, "").url),
          (wallpaper, navigator.nextPage(wallpaper, "").url),
          (otherPaper, navigator.nextPage(otherPaper, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArticlesPaperPaperboardLvl4Page()(
              FakeRequest(
                POST,
                routes.PaperPrintedController.submitArticlesPaperPaperboardLvl4Page().url
              )
                .withFormUrlEncodedBody("paper4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitArticlesPaperPaperboardLvl4Page()(
            FakeRequest(POST, routes.PaperPrintedController.submitArticlesPaperPaperboardLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of paper or paperboard products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPaperLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPaperLvl3Page()(
            FakeRequest(GET, routes.PaperPrintedController.loadPaperLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-pulp-paper", "Pulp, paper and paperboard"),
          ("sector-label-paper-board", "Articles of paper and paperboard")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPaperLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (pulp, navigator.nextPage(pulp, "").url),
          (articlesPaperBoard, navigator.nextPage(articlesPaperBoard, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPaperLvl3Page()(
              FakeRequest(
                POST,
                routes.PaperPrintedController.submitPaperLvl3Page().url
              )
                .withFormUrlEncodedBody("paper3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitPaperLvl3Page()(
            FakeRequest(POST, routes.PaperPrintedController.submitPaperLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of paper products your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPrintedLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPrintedLvl3Page()(
            FakeRequest(GET, routes.PaperPrintedController.loadPrintedLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-printing", "Printing and service activities related to printing"),
          ("sector-label-media", "Reproduction of recorded media")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPrintedLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (printingService, navigator.nextPage(printingService, "").url),
          (recordedMediaReproduction4, navigator.nextPage(recordedMediaReproduction4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPrintedLvl3Page()(
              FakeRequest(
                POST,
                routes.PaperPrintedController.submitPrintedLvl3Page().url
              )
                .withFormUrlEncodedBody("printed3" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitPrintedLvl3Page()(
            FakeRequest(POST, routes.PaperPrintedController.submitPrintedLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in printing"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPrintingServicesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPrintingServicesLvl4Page()(
            FakeRequest(GET, routes.PaperPrintedController.loadPrintingServicesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-binding", "Binding and related services"),
          ("sector-label-pre-press", "Pre-press and pre-media services"),
          ("sector-label-newspapers", "Printing of newspapers"),
          ("sector-label-media", "Other printing")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPrintingServicesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (bindingServices, navigator.nextPage(bindingServices, "").url),
          (preMediaServices, navigator.nextPage(preMediaServices, "").url),
          (newspapersPrinting, navigator.nextPage(newspapersPrinting, "").url),
          (otherPrinting, navigator.nextPage(otherPrinting, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPrintingServicesLvl4Page()(
              FakeRequest(
                POST,
                routes.PaperPrintedController.submitPrintingServicesLvl4Page().url
              )
                .withFormUrlEncodedBody("printing4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitPrintingServicesLvl4Page()(
            FakeRequest(POST, routes.PaperPrintedController.submitPrintingServicesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of printing or service activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPulpPaperPaperboardLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPulpPaperPaperboardLvl4Page()(
            FakeRequest(GET, routes.PaperPrintedController.loadPulpPaperPaperboardLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-pulp", "Pulp"),
          ("sector-label-paper", "Paper and paperboard")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPulpPaperPaperboardLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (pulp4, navigator.nextPage(pulp4, "").url),
          (paper, navigator.nextPage(paper, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPulpPaperPaperboardLvl4Page()(
              FakeRequest(
                POST,
                routes.PaperPrintedController.submitPulpPaperPaperboardLvl4Page().url
              )
                .withFormUrlEncodedBody("pulp4" -> value)
            )
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(expectedUrl)
        }
      }
      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitPulpPaperPaperboardLvl4Page()(
            FakeRequest(POST, routes.PaperPrintedController.submitPulpPaperPaperboardLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of pulp, paper or paperboard your undertaking manufactures"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
