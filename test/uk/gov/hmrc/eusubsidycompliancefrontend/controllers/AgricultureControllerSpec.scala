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
import org.scalatest.prop.Tables.Table
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.test.util.FakeTimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider

class AgricultureControllerSpec
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

  private val controller = instanceOf[AgricultureController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val cementPlasterArticles = "23.6"
    val cropAnimalProduction = "01"
    val growingNonPerennialCrops = "01.1"
    val cerealsLeguminousCrops = "01.11"
    val fibreCrops = "01.16"
    val rice = "01.12"
    val sugarCane = "01.14"
    val tobacco = "01.15"
    val vegetables = "01.13"
    val otherNonPerennialCrops = "01.19"
    val growingPerennialCrops = "01.2"
    val beverageCrops = "01.27"
    val citrusFruits = "01.23"
    val grapes = "01.21"
    val oleaginousFruits = "01.26"
    val stoneFruits = "01.24"
    val spicesPharmaceuticalCrops = "01.28"
    val tropicalFruits = "01.22"
    val otherTree = "01.25"
    val otherPerennialCrops = "01.29"
    val plantPropagation = "01.3"
    val plantPropagation4 = "01.30"
    val animalProduction = "01.4"
    val dairyCattle = "01.41"
    val otherCattle = "01.42"
    val camels = "01.44"
    val horses = "01.43"
    val poultry = "01.47"
    val sheep = "01.45"
    val swine = "01.46"
    val otherAnimals = "01.48"
    val mixedFarming = "01.5"
    val mixedFarming4 = "01.50"
    val supportActivities = "01.6"
    val postHarvestActivities = "01.63"
    val supportActivitiesAnimal = "01.62"
    val supportActivitiesCrop = "01.61"
    val huntingTrapping = "01.7"
    val huntingTrapping4 = "01.70"

  }

  import SectorCodes._

  "AgricultureController" should {
    "loadAgricultureLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAgricultureLvl3Page()(
            FakeRequest(GET, routes.AgricultureController.loadAgricultureLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-animalProduction", "Animal production"),
          ("sector-label-growingNonPerennialCrops", "Growing non-perennial crops"),
          ("sector-label-growingPerennialCrops", "Growing perennial crops"),
          ("sector-label-huntingTrapping", "Hunting, trapping and related service activities"),
          ("sector-label-mixedFarming", "Mixed farming"),
          ("sector-label-plantPropagation", "Plant propagation"),
          ("sector-label-supportActivities", "Support activities to agriculture and post-harvest crop activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }

    "submitAgricultureLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (cropAnimalProduction, navigator.nextPage(cropAnimalProduction, "").url),
          (growingPerennialCrops, navigator.nextPage(growingPerennialCrops, "").url),
          (plantPropagation, navigator.nextPage(plantPropagation, "").url),
          (animalProduction, navigator.nextPage(animalProduction, "").url),
          (mixedFarming, navigator.nextPage(mixedFarming, "").url),
          (supportActivities, navigator.nextPage(supportActivities, "").url),
          (huntingTrapping, navigator.nextPage(huntingTrapping, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAgricultureLvl3Page()(
              FakeRequest(
                POST,
                routes.AgricultureController.submitAgricultureLvl3Page().url
              )
                .withFormUrlEncodedBody("agriculture3" -> value)
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
          controller.submitAgricultureLvl3Page()(
            FakeRequest(POST, routes.AgricultureController.submitAgricultureLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in agriculture"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
