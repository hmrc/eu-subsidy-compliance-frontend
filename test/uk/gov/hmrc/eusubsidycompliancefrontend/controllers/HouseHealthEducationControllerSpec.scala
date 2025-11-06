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
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig

class HouseHealthEducationControllerSpec
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

  private val controller = instanceOf[HouseHealthEducationController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val domesticPersonnel = "97.00"
    val undifferentiatedGoodsAndServiceActivities = "98"
    val undifferentiatedGoods4 = "98.10"
    val undifferentiatedProduction = "98.20"
    val humanHealthActivities = "86"
    val residentialCareActivities = "87"
    val otherSocialWork = "88"
    val hospitalActivities = "86.10"
    val medicalDental = "86.2"
    val otherHumanHealth = "86.9"
    val dental = "86.23"
    val generalPractice = "86.21"
    val specialists = "86.22"
    val psychologists = "86.93"
    val diagnosticImaging = "86.91"
    val nursingMidwifery = "86.94"
    val ambulance = "86.92"
    val physiotherapy = "86.95"
    val traditionalMedicine = "86.96"
    val humanHealthActivitiesIntermediationServices = "86.97"
    val otherHumanHealthActivities4 = "86.99"
    val otherResidentialCareIntermediation = "87.91"
    val otherResidentialCare4 = "87.99"
    val childDayCare = "88.91"
    val otherSocialWorkNEC = "88.99"
    val disabledResidentialCareActivitiesMentalIllness4 = "87.30"
    val residentialCareActivitiesMentalIllness4 = "87.20"
    val residentialNursing4 = "87.10"
    val otherResidentialCare = "87.9"
    val generalSecondaryEducation = "85.31"
    val vocationalSecondaryEducation = "85.32"
    val postSecondaryEducationNonTertiary = "85.33"
    val culturalEducation = "85.52"
    val drivingSchool = "85.53"
    val sportsRecreationEducation = "85.51"
    val otherEducationNEC = "85.59"
    val prePrimaryEducation4 = "85.10"
    val primaryEducation4 = "85.20"
    val secondaryEducation = "85.3"
    val tertiaryEducation4 = "85.40"
    val otherEducation = "85.5"
    val educationalSupport = "85.6"
    val educationalSupportIntermediation = "85.61"
    val otherEducationalSupport = "85.69"
    val socialWorkDisabilities4 = "88.10"
    val otherSocialWorkWithoutAccommodation = "88.9"




  }

  import SectorCodes._

  "HouseHealthEducationController" should {
    "loadHouseholdsLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHouseholdsLvl2Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadHouseholdsLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-domesticPersonnel", "Activities of households as employers of domestic personnel"),
          ("sector-label-undifferentiatedProduction", "Undifferentiated goods and service-producing activities of private households for own use")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHouseholdsLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (domesticPersonnel, navigator.nextPage(domesticPersonnel, "").url),
          (undifferentiatedGoodsAndServiceActivities, navigator.nextPage(undifferentiatedGoodsAndServiceActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHouseholdsLvl2Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitHouseholdsLvl2Page().url
              )
                .withFormUrlEncodedBody("households2" -> value)
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
          controller.submitHouseholdsLvl2Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitHouseholdsLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in households as employers or as producers of goods or services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadUndifferentiatedProducingActivitiesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadUndifferentiatedProducingActivitiesLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadUndifferentiatedProducingActivitiesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-undifferentiatedGoods4", "Undifferentiated goods-producing activities of private households for own use"),
          ("sector-label-undifferentiatedServices4", "Undifferentiated service-producing activities of private households for own use")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitUndifferentiatedProducingActivitiesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (undifferentiatedGoods4, navigator.nextPage(undifferentiatedGoods4, "").url),
          (undifferentiatedProduction, navigator.nextPage(undifferentiatedProduction, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitUndifferentiatedProducingActivitiesLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitUndifferentiatedProducingActivitiesLvl4Page().url
              )
                .withFormUrlEncodedBody("undifferProducing4" -> value)
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
          controller.submitUndifferentiatedProducingActivitiesLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitUndifferentiatedProducingActivitiesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of private household activities your undertaking carries out"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHumanHealthLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHumanHealthLvl2Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadHumanHealthLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-humanHealthActivities", "Human health activities"),
          ("sector-label-residentialCareActivities", "Residential care activities"),
          ("sector-label-otherSocialWork", "Social work activities without accommodation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHumanHealthLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (humanHealthActivities, navigator.nextPage(humanHealthActivities, "").url),
          (residentialCareActivities, navigator.nextPage(residentialCareActivities, "").url),
          (otherSocialWork, navigator.nextPage(otherSocialWork, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHumanHealthLvl2Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitHumanHealthLvl2Page().url
              )
                .withFormUrlEncodedBody("humanHealth2" -> value)
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
          controller.submitHumanHealthLvl2Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitHumanHealthLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in human healthcare and social work"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHumanHealthLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHumanHealthLvl3Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadHumanHealthLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-hospital", "Hospital"),
          ("sector-label-medicalDental", "Medical and dental practice"),
          ("sector-label-otherHumanHealth", "Other human health activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHumanHealthLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (hospitalActivities, navigator.nextPage(hospitalActivities, "").url),
          (medicalDental, navigator.nextPage(medicalDental, "").url),
          (otherHumanHealth, navigator.nextPage(otherHumanHealth, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHumanHealthLvl3Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitHumanHealthLvl3Page().url
              )
                .withFormUrlEncodedBody("humanHealth3" -> value)
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
          controller.submitHumanHealthLvl3Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitHumanHealthLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of healthcare your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMedicalDentalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMedicalDentalLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadMedicalDentalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-dental", "Dental practice care"),
          ("sector-label-generalPractice", "General medical practice activities"),
          ("sector-label-specialists", "Medical specialists activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMedicalDentalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (dental, navigator.nextPage(dental, "").url),
          (generalPractice, navigator.nextPage(generalPractice, "").url),
          (specialists, navigator.nextPage(specialists, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMedicalDentalLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitMedicalDentalLvl4Page().url
              )
                .withFormUrlEncodedBody("medical4" -> value)
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
          controller.submitMedicalDentalLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitMedicalDentalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in medical and dental practice and related activities"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherHumanHealthLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherHumanHealthLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadOtherHumanHealthLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-psychologists", "Activities of psychologists and psychotherapists (except medical doctors)"),
          ("sector-label-diagnosticImaging", "Diagnostic imaging services and medical laboratory activities"),
          ("sector-label-nursingMidwifery", "Nursing and midwifery"),
          ("sector-label-ambulance", "Patient transportation by ambulance"),
          ("sector-label-physiotherapy", "Physiotherapy"),
          ("sector-label-traditionalMedicine", "Traditional, complementary and alternative medicine"),
          ("sector-label-humanHealthActivitiesIntermediationServices", "Intermediation services for medical, dental and other human health services"),
          ("sector-label-OtherHumanHealthActivities4", "Any other human health activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherHumanHealthLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (psychologists, navigator.nextPage(psychologists, "").url),
          (diagnosticImaging, navigator.nextPage(diagnosticImaging, "").url),
          (nursingMidwifery, navigator.nextPage(nursingMidwifery, "").url),
          (ambulance, navigator.nextPage(ambulance, "").url),
          (physiotherapy, navigator.nextPage(physiotherapy, "").url),
          (traditionalMedicine, navigator.nextPage(traditionalMedicine, "").url),
          (humanHealthActivitiesIntermediationServices, navigator.nextPage(humanHealthActivitiesIntermediationServices, "").url),
          (otherHumanHealthActivities4, navigator.nextPage(otherHumanHealthActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherHumanHealthLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitOtherHumanHealthLvl4Page().url
              )
                .withFormUrlEncodedBody("otherHealth4" -> value)
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
          controller.submitOtherHumanHealthLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitOtherHumanHealthLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other human health activities or services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherResidentialCareLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherResidentialCareLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadOtherResidentialCareLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-otherResidentialCareIntermediation", "Intermediation services for residential care activities"),
          ("sector-label-OtherResidentialCare", "Any other residential care activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherResidentialCareLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (otherResidentialCareIntermediation, navigator.nextPage(otherResidentialCareIntermediation, "").url),
          (otherResidentialCare4, navigator.nextPage(otherResidentialCare4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherResidentialCareLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitOtherResidentialCareLvl4Page().url
              )
                .withFormUrlEncodedBody("otherResidential4" -> value)
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
          controller.submitOtherResidentialCareLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitOtherResidentialCareLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of residential care your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherSocialWorkLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherSocialWorkLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadOtherSocialWorkLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-childDayCare", "Child daycare"),
          ("sector-label-otherSocialWorkNEC", "Any other social work activities without accommodation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherSocialWorkLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (childDayCare, navigator.nextPage(childDayCare, "").url),
          (otherSocialWorkNEC, navigator.nextPage(otherSocialWorkNEC, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherSocialWorkLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitOtherSocialWorkLvl4Page().url
              )
                .withFormUrlEncodedBody("otherSocial4" -> value)
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
          controller.submitOtherSocialWorkLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitOtherSocialWorkLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other social work activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadResidentialCareLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadResidentialCareLvl3Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadResidentialCareLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-disabledResidentialCareActivitiesMentalIllness", "Residential care activities for older persons or persons with physical disabilities"),
          ("sector-label-residentialCareActivitiesMentalIllness", "Residential care activities for persons living with or having a diagnosis of a mental illness or substance abuse"),
          ("sector-label-residentialNursing", "Residential nursing care"),
          ("sector-label-otherResidentialCare", "Other residential care activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitResidentialCareLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (disabledResidentialCareActivitiesMentalIllness4, navigator.nextPage(disabledResidentialCareActivitiesMentalIllness4, "").url),
          (residentialCareActivitiesMentalIllness4, navigator.nextPage(residentialCareActivitiesMentalIllness4, "").url),
          (residentialNursing4, navigator.nextPage(residentialNursing4, "").url),
          (otherResidentialCare, navigator.nextPage(otherResidentialCare, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitResidentialCareLvl3Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitResidentialCareLvl3Page().url
              )
                .withFormUrlEncodedBody("resiCare3" -> value)
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
          controller.submitResidentialCareLvl3Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitResidentialCareLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other residential care activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadSecondaryEducationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSecondaryEducationLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadSecondaryEducationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-generalSecondaryEducation", "General secondary education"),
          ("sector-label-vocationalSecondaryEducation", "Vocational secondary education"),
          ("sector-label-postSecondaryEducationNonTertiary", "Post-secondary non-tertiary education")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSecondaryEducationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (generalSecondaryEducation, navigator.nextPage(generalSecondaryEducation, "").url),
          (vocationalSecondaryEducation, navigator.nextPage(vocationalSecondaryEducation, "").url),
          (postSecondaryEducationNonTertiary, navigator.nextPage(postSecondaryEducationNonTertiary, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSecondaryEducationLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitSecondaryEducationLvl4Page().url
              )
                .withFormUrlEncodedBody("secondaryEducation4" -> value)
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
          controller.submitSecondaryEducationLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitSecondaryEducationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of secondary or post-secondary education your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherEducationLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherEducationLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadOtherEducationLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-culturalEducation", "Cultural education"),
          ("sector-label-drivingSchool", "Driving school"),
          ("sector-label-sportsRecreationEducation", "Sports and recreation education"),
          ("sector-label-otherEducationNEC", "Another type of education")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherEducationLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (culturalEducation, navigator.nextPage(culturalEducation, "").url),
          (drivingSchool, navigator.nextPage(drivingSchool, "").url),
          (sportsRecreationEducation, navigator.nextPage(sportsRecreationEducation, "").url),
          (otherEducationNEC, navigator.nextPage(otherEducationNEC, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherEducationLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitOtherEducationLvl4Page().url
              )
                .withFormUrlEncodedBody("otherEducation4" -> value)
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
          controller.submitOtherEducationLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitOtherEducationLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other education your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadEducationLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadEducationLvl3Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadEducationLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-prePrimaryEducation", "Pre-primary education"),
          ("sector-label-primaryEducation", "Primary education"),
          ("sector-label-secondaryEducation", "Secondary and post-secondary non-tertiary education"),
          ("sector-label-tertiaryEducation", "Tertiary education"),
          ("sector-label-otherEducation", "Other education"),
          ("sector-label-educationalSupport", "Educational support")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitEducationLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (prePrimaryEducation4, navigator.nextPage(prePrimaryEducation4, "").url),
          (primaryEducation4, navigator.nextPage(primaryEducation4, "").url),
          (secondaryEducation, navigator.nextPage(secondaryEducation, "").url),
          (tertiaryEducation4, navigator.nextPage(tertiaryEducation4, "").url),
          (otherEducation, navigator.nextPage(otherEducation, "").url),
          (educationalSupport, navigator.nextPage(educationalSupport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitEducationLvl3Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitEducationLvl3Page().url
              )
                .withFormUrlEncodedBody("education3" -> value)
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
          controller.submitEducationLvl3Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitEducationLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in education"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadEducationalSupportLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadEducationalSupportLvl4Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadEducationalSupportLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-educationalSupportIntermediation", "Intermediation service activities for courses and tutors"),
          ("sector-label-otherEducationalSupport", "Other educational support")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitEducationalSupportLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (educationalSupportIntermediation, navigator.nextPage(educationalSupportIntermediation, "").url),
          (otherEducationalSupport, navigator.nextPage(otherEducationalSupport, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitEducationalSupportLvl4Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitEducationalSupportLvl4Page().url
              )
                .withFormUrlEncodedBody("educationSupport4" -> value)
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
          controller.submitEducationalSupportLvl4Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitEducationalSupportLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of educational support your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadSocialWorkLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSocialWorkLvl3Page()(
            FakeRequest(GET, routes.HouseHealthEducationController.loadSocialWorkLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-socialWorkDisabilities", "Social work activities without accommodation for older persons or persons with disabilities"),
          ("sector-label-otherSocialWorkWithoutAccommodation", "Other social work activities without accommodation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSocialWorkLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (socialWorkDisabilities4, navigator.nextPage(socialWorkDisabilities4, "").url),
          (otherSocialWorkWithoutAccommodation, navigator.nextPage(otherSocialWorkWithoutAccommodation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSocialWorkLvl3Page()(
              FakeRequest(
                POST,
                routes.HouseHealthEducationController.submitSocialWorkLvl3Page().url
              )
                .withFormUrlEncodedBody("socialWork3" -> value)
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
          controller.submitSocialWorkLvl3Page()(
            FakeRequest(POST, routes.HouseHealthEducationController.submitSocialWorkLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of social work activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
  }
}
