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

class PublishingTelecomsControllerSpec
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

  private val controller = instanceOf[PublishingTelecomsController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val computingInfrastructureActivities4 = "63.10"
    val webSearchPortal = "63.9"
    val computerFacilitiesConsultancy4 = "62.20"
    val computerProgrammingActivities4 = "62.10"
    val otherComputerServiceActivities4 = "62.90"
    val computerProgrammingConsultancy = "62"
    val computingInfrastructureActivities = "63"
    val telecommunication = "61"
    val resellingTelecommunication4 = "61.20"
    val wiredTelecommunication4 = "61.10"
    val otherTelecommunications4 = "61.90"
    val webSearchPortalActivities = "63.91"
    val otherInformationServices = "63.92"
    val books = "58.11"
    val journalsPeriodicals = "58.13"
    val newspapers = "58.12"
    val otherPublishing = "58.19"
    val filmVideoActivities = "59.1"
    val soundRecordingAndMusicPublishingActivities = "59.20"
    val videoProductionDistribution = "59.13"
    val videoPostProduction = "59.12"
    val videoProduction = "59.11"
    val projection = "59.14"
    val newsAgencyActivities = "60.31"
    val otherContentDistributionActivities = "60.39"
    val radioBroadcasting4 = "60.10"
    val programmingBroadcastingVideoDistribution4 = "60.20"
    val newsAgency = "60.3"
    val filmVideoSoundPublishing = "59"
    val programmingBroadcastingNewsActivities = "60"
    val publishing = "58"
    val publishingBooksNewspapers = "58.1"
    val softwarePublishing = "58.2"
    val videoGames = "58.21"
    val otherSoftware = "58.29"
  }

  import SectorCodes._

  "PublishingTelecomsController" should {
    "loadComputerInfrastructureDataHostingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadComputerInfrastructureDataHostingLvl3Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadComputerInfrastructureDataHostingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-computingInfrastructureActivities3",
            "Computing infrastructure, data processing, hosting and related activities"
          ),
          ("sector-label-webSearchPortal", "Web search portal activities and other information services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitComputerInfrastructureDataHostingLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (computingInfrastructureActivities4, navigator.nextPage(computingInfrastructureActivities4, "").url),
          (webSearchPortal, navigator.nextPage(webSearchPortal, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitComputerInfrastructureDataHostingLvl3Page()(
              FakeRequest(
                POST,
                routes.PublishingTelecomsController.submitComputerInfrastructureDataHostingLvl3Page().url
              )
                .withFormUrlEncodedBody("infra3" -> value)
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
          controller.submitComputerInfrastructureDataHostingLvl3Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitComputerInfrastructureDataHostingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in formation services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadComputerProgrammingConsultancyLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadComputerProgrammingConsultancyLvl3Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadComputerProgrammingConsultancyLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-computerFacilitiesConsultancy", "Computer facilities management and consultancy"),
          ("sector-label-computerProgrammingActivities", "Computer programming"),
          (
            "sector-label-otherComputerServiceActivities",
            "Other information technology and computer service activities"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitComputerProgrammingConsultancyLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (computerFacilitiesConsultancy4, navigator.nextPage(computerFacilitiesConsultancy4, "").url),
          (computerProgrammingActivities4, navigator.nextPage(computerProgrammingActivities4, "").url),
          (otherComputerServiceActivities4, navigator.nextPage(otherComputerServiceActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitComputerProgrammingConsultancyLvl3Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitComputerProgrammingConsultancyLvl3Page().url)
                .withFormUrlEncodedBody("programming3" -> value)
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
          controller.submitComputerProgrammingConsultancyLvl3Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitComputerProgrammingConsultancyLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in computer programming, consultancy and related activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTelecommunicationLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTelecommunicationLvl2Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadTelecommunicationLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-computerProgrammingConsultancy", "Computer programming, consultancy and related activities"),
          (
            "sector-label-computingInfrastructureActivities",
            "Computing infrastructure, data processing, hosting and other information service activities"
          ),
          ("sector-label-telecommunication", "Telecommunication")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTelecommunicationLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (computerProgrammingConsultancy, navigator.nextPage(computerProgrammingConsultancy, "").url),
          (computingInfrastructureActivities, navigator.nextPage(computingInfrastructureActivities, "").url),
          (telecommunication, navigator.nextPage(telecommunication, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTelecommunicationLvl2Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitTelecommunicationLvl2Page().url)
                .withFormUrlEncodedBody("telecommunication2" -> value)
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
          controller.submitTelecommunicationLvl2Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitTelecommunicationLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in telecommunications and computing"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadTelecommunicationLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadTelecommunicationLvl3Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadTelecommunicationLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-resellingTelecommunication",
            "Telecommunication reselling and intermediation services for telecommunication"
          ),
          ("sector-label-wiredTelecommunication", "Wired, wireless and satellite telecommunication"),
          ("sector-label-otherTelecommunications", "Other telecommunication activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitTelecommunicationLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (resellingTelecommunication4, navigator.nextPage(resellingTelecommunication4, "").url),
          (wiredTelecommunication4, navigator.nextPage(wiredTelecommunication4, "").url),
          (otherTelecommunications4, navigator.nextPage(otherTelecommunications4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitTelecommunicationLvl3Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitTelecommunicationLvl3Page().url)
                .withFormUrlEncodedBody("telecommunication3" -> value)
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
          controller.submitTelecommunicationLvl3Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitTelecommunicationLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in telecommunications"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadWebSearchPortalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadWebSearchPortalLvl4Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadWebSearchPortalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-webSearchPortalActivities", "Web search portal activities"),
          ("sector-label-telecommunicationsComputing", "Other information service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitWebSearchPortalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (webSearchPortalActivities, navigator.nextPage(webSearchPortalActivities, "").url),
          (otherInformationServices, navigator.nextPage(otherInformationServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitWebSearchPortalLvl4Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitWebSearchPortalLvl4Page().url)
                .withFormUrlEncodedBody("webSearch4" -> value)
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
          controller.submitWebSearchPortalLvl4Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitWebSearchPortalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in web search portal activities and other information services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFilmMusicPublishingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFilmMusicPublishingLvl3Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadFilmMusicPublishingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-filmVideoActivities", "Film, TV and video activities"),
          ("sector-label-soundRecordingAndMusicPublishing", "Sound recording and music publishing")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFilmMusicPublishingLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (filmVideoActivities, navigator.nextPage(filmVideoActivities, "").url),
          (
            soundRecordingAndMusicPublishingActivities,
            navigator.nextPage(soundRecordingAndMusicPublishingActivities, "").url
          )
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFilmMusicPublishingLvl3Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitFilmMusicPublishingLvl3Page().url)
                .withFormUrlEncodedBody("filmPublishing3" -> value)
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
          controller.submitFilmMusicPublishingLvl3Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitFilmMusicPublishingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in film, TV and video production, sound recording and music publishing"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadFilmVideoActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadFilmVideoActivitiesLvl4Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadFilmVideoActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-videoProductionDistribution", "Distribution"),
          ("sector-label-videoPostProduction", "Post-production"),
          ("sector-label-videoProduction", "Production"),
          ("sector-label-projection", "Projection")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitFilmVideoActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (videoProductionDistribution, navigator.nextPage(videoProductionDistribution, "").url),
          (videoPostProduction, navigator.nextPage(videoPostProduction, "").url),
          (videoProduction, navigator.nextPage(videoProduction, "").url),
          (projection, navigator.nextPage(projection, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitFilmVideoActivitiesLvl4Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitFilmVideoActivitiesLvl4Page().url)
                .withFormUrlEncodedBody("film4" -> value)
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
          controller.submitFilmVideoActivitiesLvl4Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitFilmVideoActivitiesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of film, TV and video service your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadNewsOtherContentDistributionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadNewsOtherContentDistributionLvl4Page()(
            FakeRequest(GET, routes.PublishingTelecomsController.loadNewsOtherContentDistributionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-newsAgencyActivities", "News agency activities"),
          ("sector-label-otherContentDistributionActivities", "Other content distribution activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitNewsOtherContentDistributionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (newsAgencyActivities, navigator.nextPage(newsAgencyActivities, "").url),
          (otherContentDistributionActivities, navigator.nextPage(otherContentDistributionActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitNewsOtherContentDistributionLvl4Page()(
              FakeRequest(POST, routes.PublishingTelecomsController.submitNewsOtherContentDistributionLvl4Page().url)
                .withFormUrlEncodedBody("news4" -> value)
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
          controller.submitNewsOtherContentDistributionLvl4Page()(
            FakeRequest(POST, routes.PublishingTelecomsController.submitNewsOtherContentDistributionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in news agency and content distribution"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
