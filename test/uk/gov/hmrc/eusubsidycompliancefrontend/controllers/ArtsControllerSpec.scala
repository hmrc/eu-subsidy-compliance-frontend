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
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class ArtsControllerSpec
    extends ControllerSpec
    with AuthSupport
    with JourneyStoreSupport
    with AuthAndSessionDataBehaviour
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val fakeTimeProvider = FakeTimeProvider.withFixedDate(1, 1, 2022)

  override def overrideBindings: List[GuiceableModule] = List(
    bind[AuthConnector].toInstance(mockAuthConnector),
    bind[TimeProvider].toInstance(fakeTimeProvider)
  )

  private val controller = instanceOf[ArtsController]

  // Sector codes for Level 2
  private val artsCreationPerforming = Sector.artsCreationPerforming.toString
  private val gamblingActivities = Sector.gamblingActivities4.toString
  private val librariesArchivesMuseums = Sector.librariesArchivesMuseums.toString
  private val sportsAndRecreation = Sector.sportsAndRecreation.toString

  // Sector codes for Level 3 - Arts Creation and Performing
  private val artsCreation = Sector.artsCreation.toString
  private val performingArts = Sector.performingArtsActivities.toString
  private val performingArtsSupport = Sector.performingArtsSupport.toString

  // Sector codes for Level 3 - Libraries, Archives, Museums
  private val botanicalGardensNatureReserves = Sector.botanicalGardensAndNatureReserves.toString
  private val culturalHeritageConservation = Sector.culturalHeritageConservation4.toString
  private val libraryArchives = Sector.libraryArchives.toString
  private val museumCollectionsMonuments = Sector.museumCollectionsMonuments.toString

  // Sector codes for Level 3 - Sports and Recreation
  private val amusementRecreation = Sector.amusementRecreation.toString
  private val sports = Sector.sports.toString

  // Sector codes for Level 4 - Arts Creation
  private val literaryMusical = Sector.literaryMusical.toString
  private val visualArtsCreation = Sector.visualArtsCreation.toString
  private val otherArtsCreation = Sector.otherArtsCreation.toString

  // Sector codes for Level 4 - Performing Arts Support
  private val artsFacilitiesOperation = Sector.artsFacilitiesOperation.toString
  private val otherPerformingArtsSupport = Sector.otherPerformingArtsSupport.toString

  // Sector codes for Level 4 - Libraries and Archives
  private val archives = Sector.archives.toString
  private val libraries = Sector.libraries.toString

  // Sector codes for Level 4 - Museums and Collections
  private val historicalSitesMonuments = Sector.historicalSitesMonuments.toString
  private val museumsCollections = Sector.museumsCollections.toString

  // Sector codes for Level 4 - Botanical and Zoological
  private val botanicalZoologicalGardens = Sector.botanicalZoologicalGardens.toString
  private val natureReserves = Sector.natureReserves.toString

  // Sector codes for Level 4 - Sports
  private val fitnessCentres = Sector.fitnessCentresActivities.toString
  private val sportsClubs = Sector.sportsClubsActivities.toString
  private val sportsFacilitiesOperation = Sector.sportsFacilitiesOperation.toString
  private val otherSportsActivities = Sector.otherSportsActivities.toString

  // Sector codes for Level 4 - Amusement and Recreation
  private val amusementParks = Sector.amusementParks.toString
  private val otherRecreationActivities = Sector.otherRecreationActivities.toString

  "ArtsController" when {

    "loadArtsSportsRecreationLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArtsSportsRecreationLvl2Page()(
            FakeRequest(GET, routes.ArtsController.loadArtsSportsRecreationLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))

        // All radio buttons share the same ID due to view bug, so check there are 4 spans with that ID
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 4
      }
    }

    "submitArtsSportsRecreationLvl2Page" should {
      "redirect to correct page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (artsCreationPerforming, routes.ArtsController.loadArtsCreationPerformingLvl3Page().url),
          (librariesArchivesMuseums, routes.ArtsController.loadLibrariesArchivesCulturalLvl3Page().url),
          (sportsAndRecreation, routes.ArtsController.loadSportsAmusementRecreationLvl3Page().url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArtsSportsRecreationLvl2Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitArtsSportsRecreationLvl2Page().url
              )
                .withFormUrlEncodedBody("artsSports2" -> value)
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
          controller.submitArtsSportsRecreationLvl2Page()(
            FakeRequest(POST, routes.ArtsController.submitArtsSportsRecreationLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in arts, sports and recreation"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadArtsCreationPerformingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArtsCreationPerformingLvl3Page()(
            FakeRequest(GET, routes.ArtsController.loadArtsCreationPerformingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 3
      }
    }

    "submitArtsCreationPerformingLvl3Page" should {
      "redirect to correct page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (artsCreation, routes.ArtsController.loadArtsCreationLvl4Page().url),
          (performingArtsSupport, routes.ArtsController.loadArtsPerformingSupportActivitiesLvl4Page().url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArtsCreationPerformingLvl3Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitArtsCreationPerformingLvl3Page().url
              )
                .withFormUrlEncodedBody("artsCreation3" -> value)
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
          controller.submitArtsCreationPerformingLvl3Page()(
            FakeRequest(POST, routes.ArtsController.submitArtsCreationPerformingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in arts creation and performing arts"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadLibrariesArchivesCulturalLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadLibrariesArchivesCulturalLvl3Page()(
            FakeRequest(GET, routes.ArtsController.loadLibrariesArchivesCulturalLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 4
      }
    }

    "submitLibrariesArchivesCulturalLvl3Page" should {
      "redirect to correct page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (botanicalGardensNatureReserves, routes.ArtsController.loadBotanicalZoologicalReservesLvl4Page().url),
          (libraryArchives, routes.ArtsController.loadLibrariesArchivesLvl4Page().url),
          (museumCollectionsMonuments, routes.ArtsController.loadMuseumsCollectionsMomumentsLvl4Page().url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitLibrariesArchivesCulturalLvl3Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitLibrariesArchivesCulturalLvl3Page().url
              )
                .withFormUrlEncodedBody("libraries3" -> value)
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
          controller.submitLibrariesArchivesCulturalLvl3Page()(
            FakeRequest(POST, routes.ArtsController.submitLibrariesArchivesCulturalLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of cultural organisation your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadSportsAmusementRecreationLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSportsAmusementRecreationLvl3Page()(
            FakeRequest(GET, routes.ArtsController.loadSportsAmusementRecreationLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitSportsAmusementRecreationLvl3Page" should {
      "redirect to correct page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (sports, routes.ArtsController.loadSportsLvl4Page().url),
          (amusementRecreation, routes.ArtsController.loadAmusementAndRecreationLvl4Page().url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSportsAmusementRecreationLvl3Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitSportsAmusementRecreationLvl3Page().url
              )
                .withFormUrlEncodedBody("sports3" -> value)
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
          controller.submitSportsAmusementRecreationLvl3Page()(
            FakeRequest(POST, routes.ArtsController.submitSportsAmusementRecreationLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in sports, amusement and recreation"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadArtsCreationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArtsCreationLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadArtsCreationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 3
      }
    }

    "submitArtsCreationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(literaryMusical, visualArtsCreation, otherArtsCreation)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArtsCreationLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitArtsCreationLvl4Page().url
              )
                .withFormUrlEncodedBody("artsCreation4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitArtsCreationLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitArtsCreationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in arts creation"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadArtsPerformingSupportActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArtsPerformingSupportActivitiesLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadArtsPerformingSupportActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitArtsPerformingSupportActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(artsFacilitiesOperation, otherPerformingArtsSupport)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArtsPerformingSupportActivitiesLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitArtsPerformingSupportActivitiesLvl4Page().url
              )
                .withFormUrlEncodedBody("artsPerforming4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitArtsPerformingSupportActivitiesLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitArtsPerformingSupportActivitiesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in arts support activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadLibrariesArchivesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadLibrariesArchivesLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadLibrariesArchivesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitLibrariesArchivesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(archives, libraries)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitLibrariesArchivesLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitLibrariesArchivesLvl4Page().url
              )
                .withFormUrlEncodedBody("libraries4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitLibrariesArchivesLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitLibrariesArchivesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of library or archive your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadMuseumsCollectionsMomumentsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMuseumsCollectionsMomumentsLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadMuseumsCollectionsMomumentsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitMuseumsCollectionsMomumentsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(historicalSitesMonuments, museumsCollections)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMuseumsCollectionsMomumentsLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitMuseumsCollectionsMomumentsLvl4Page().url
              )
                .withFormUrlEncodedBody("museums4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitMuseumsCollectionsMomumentsLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitMuseumsCollectionsMomumentsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of museum, collection or site your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadBotanicalZoologicalReservesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadBotanicalZoologicalReservesLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadBotanicalZoologicalReservesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))

        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitBotanicalZoologicalReservesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(botanicalZoologicalGardens, natureReserves)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitBotanicalZoologicalReservesLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitBotanicalZoologicalReservesLvl4Page().url
              )
                .withFormUrlEncodedBody("zoo4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitBotanicalZoologicalReservesLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitBotanicalZoologicalReservesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of garden or reserve your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadSportsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSportsLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadSportsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 4
      }
    }

    "submitSportsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(fitnessCentres, sportsClubs, sportsFacilitiesOperation, otherSportsActivities)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSportsLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitSportsLvl4Page().url
              )
                .withFormUrlEncodedBody("sports4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitSportsLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitSportsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of sports activity your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }

    "loadAmusementAndRecreationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAmusementAndRecreationLvl4Page()(
            FakeRequest(GET, routes.ArtsController.loadAmusementAndRecreationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))

        // All radio buttons share the same ID, check there are 2 spans
        val radioLabels = document.select("span[id=sector-label-artsSportsRecreation]")
        radioLabels.size() shouldBe 2
      }
    }

    "submitAmusementAndRecreationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Seq(amusementParks, otherRecreationActivities)
        radioButtons.foreach { value =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAmusementAndRecreationLvl4Page()(
              FakeRequest(
                POST,
                routes.ArtsController.submitAmusementAndRecreationLvl4Page().url
              )
                .withFormUrlEncodedBody("amusement4" -> value)
            )
          status(result) shouldBe SEE_OTHER
        }
      }

      "return BAD_REQUEST and show an error when no option is selected" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.submitAmusementAndRecreationLvl4Page()(
            FakeRequest(POST, routes.ArtsController.submitAmusementAndRecreationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in amusement and recreation"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        val errorLink = summary.selectFirst(".govuk-error-summary__list a")
        errorLink should not be null
        errorLink.text() shouldBe expectedErrorMsg
      }
    }
  }
}
