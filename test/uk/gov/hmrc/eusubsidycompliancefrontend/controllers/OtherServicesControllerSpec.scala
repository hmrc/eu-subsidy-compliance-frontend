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

class OtherServicesControllerSpec
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

  private val controller = instanceOf[OtherServicesController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val membershipOrganisations = "94"
    val personalServices = "96"
    val repairMaintenance = "95"
    val businessEmployersProfessional = "94.1"
    val tradeUnionsActivities = "94.20"
    val otherMembership = "94.9"
    val funeralActivities4 = "96.30"
    val beautyTreatment = "96.2"
    val cleaningTextileProducts4 = "96.10"
    val personalServicesIntermediation4 = "96.40"
    val otherPersonalServices = "96.9"
    val repairComputersCommunication = "95.10"
    val repairMotorVehiclesMotorcycles = "95.3"
    val repairHouseholdGoods = "95.2"
    val computerAndMotorcycleMaintenanceIntermediationServiceActivities4 = "95.40"
    val beautyCare = "96.22"
    val spaActivities = "96.23"
    val hairdressing = "96.21"
    val repairConsumerElectronics = "95.21"
    val repairFootwearLeather = "95.23"
    val repairFurnitureHome = "95.24"
    val repairHouseholdAppliances = "95.22"
    val watchesAndJewellery = "95.25"
    val otherHouseholdGoods = "95.29"
    val businessEmployersMembership = "94.11"
    val professionalMembership = "94.12"
    val repairMotorVehicles = "95.31"
    val repairMotorcycles = "95.32"
    val politicalOrganisations = "94.92"
    val religiousOrganisations = "94.91"
    val otherMembershipOrganisations = "94.99"
    val domesticPersonalServices = "96.91"
    val otherPersonalServices4 = "96.99"
  }

  import SectorCodes._

  "OtherServicesController" should {
    "loadArticlesPaperPaperboardLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherLvl2Page()(
            FakeRequest(GET, routes.OtherServicesController.loadOtherLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-membershipOrganisations", "Activities of membership organisations"),
          ("sector-label-personalServiceActivities", "Personal services"),
          (
            "sector-label-repairMaintenance",
            "Repair and maintenance of computers, personal and household goods, motor vehicles and motorcycles"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (membershipOrganisations, navigator.nextPage(membershipOrganisations, "").url),
          (personalServices, navigator.nextPage(personalServices, "").url),
          (repairMaintenance, navigator.nextPage(repairMaintenance, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherLvl2Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitOtherLvl2Page().url
              )
                .withFormUrlEncodedBody("other2" -> value)
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
          controller.submitOtherLvl2Page()(
            FakeRequest(POST, routes.OtherServicesController.submitOtherLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMembershipOrgActivitiesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMembershipOrgActivitiesLvl3Page()(
            FakeRequest(GET, routes.OtherServicesController.loadMembershipOrgActivitiesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-businessEmployersProfessional",
            "Business, employers and professional membership organisations"
          ),
          ("sector-label-tradeUnions", "Trade unions"),
          ("sector-label-otherMembership", "Other membership organisations")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMembershipOrgActivitiesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (businessEmployersProfessional, navigator.nextPage(businessEmployersProfessional, "").url),
          (tradeUnionsActivities, navigator.nextPage(tradeUnionsActivities, "").url),
          (otherMembership, navigator.nextPage(otherMembership, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMembershipOrgActivitiesLvl3Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitMembershipOrgActivitiesLvl3Page().url
              )
                .withFormUrlEncodedBody("membership3" -> value)
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
          controller.submitMembershipOrgActivitiesLvl3Page()(
            FakeRequest(POST, routes.OtherServicesController.submitMembershipOrgActivitiesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of membership organisation your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPersonalServicesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPersonalServicesLvl3Page()(
            FakeRequest(GET, routes.OtherServicesController.loadPersonalServicesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-funeralActivities", "Funeral and related activities"),
          ("sector-label-beautyTreatment", "Hairdressing, beauty treatment, day spa and similar activities"),
          ("sector-label-textileFurCleaning", "Washing and cleaning of textile and fur products"),
          ("sector-label-personalServicesIntermediation", "Intermediation services for personal services"),
          ("sector-label-otherPersonalServices", "Other personal services")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPersonalServicesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (funeralActivities4, navigator.nextPage(funeralActivities4, "").url),
          (beautyTreatment, navigator.nextPage(beautyTreatment, "").url),
          (cleaningTextileProducts4, navigator.nextPage(cleaningTextileProducts4, "").url),
          (personalServicesIntermediation4, navigator.nextPage(personalServicesIntermediation4, "").url),
          (otherPersonalServices, navigator.nextPage(otherPersonalServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPersonalServicesLvl3Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitPersonalServicesLvl3Page().url
              )
                .withFormUrlEncodedBody("personalServices3" -> value)
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
          controller.submitPersonalServicesLvl3Page()(
            FakeRequest(POST, routes.OtherServicesController.submitPersonalServicesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of personal services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadRepairsLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadRepairsLvl3Page()(
            FakeRequest(GET, routes.OtherServicesController.loadRepairsLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-repairComputersCommunication", "Computers and communication equipment"),
          ("sector-label-repairMotorVehiclesMotorcycles", "Motor vehicles and motorcycles"),
          ("sector-label-repairHouseholdGoods", "Personal and household goods"),
          (
            "sector-label-intermediationRepairMaintenance",
            "My undertaking provides intermediation services for repair and maintenance of computers, personal and household goods, motor vehicles and motorcycles"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitRepairsLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (repairComputersCommunication, navigator.nextPage(repairComputersCommunication, "").url),
          (repairMotorVehiclesMotorcycles, navigator.nextPage(repairMotorVehiclesMotorcycles, "").url),
          (repairHouseholdGoods, navigator.nextPage(repairHouseholdGoods, "").url),
          (
            computerAndMotorcycleMaintenanceIntermediationServiceActivities4,
            navigator.nextPage(computerAndMotorcycleMaintenanceIntermediationServiceActivities4, "").url
          )
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitRepairsLvl3Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitRepairsLvl3Page().url
              )
                .withFormUrlEncodedBody("repairs3" -> value)
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
          controller.submitRepairsLvl3Page()(
            FakeRequest(POST, routes.OtherServicesController.submitRepairsLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of goods your undertaking repairs or maintains"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHairdressingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHairdressingLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadHairdressingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-beautyCare", "Beauty care and other beauty treatments"),
          ("sector-label-spaActivities", "Day spa, sauna and steam bath activities"),
          ("sector-label-hairdressing", "Hairdressing and barbering")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHairdressingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (beautyCare, navigator.nextPage(beautyCare, "").url),
          (spaActivities, navigator.nextPage(spaActivities, "").url),
          (hairdressing, navigator.nextPage(hairdressing, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHairdressingLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitHairdressingLvl4Page().url
              )
                .withFormUrlEncodedBody("hairdressing4" -> value)
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
          controller.submitHairdressingLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitHairdressingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of beauty treatments or similar services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHouseholdRepairLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHouseholdRepairLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadHouseholdRepairLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-repairConsumerElectronics", "Consumer electronic"),
          ("sector-label-repairFootwearLeather", "Footwear and leather goods"),
          ("sector-label-repairFurnitureHome", "Furniture and home furnishings"),
          ("sector-label-repairHouseholdAppliances", "Household appliances and home and garden equipment"),
          ("sector-label-WatchesAndJewellery", "Watches, clocks and jewellery"),
          ("sector-label-otherHouseholdGoods", "Another type of personal and household goods")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHouseholdRepairLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (repairConsumerElectronics, navigator.nextPage(repairConsumerElectronics, "").url),
          (repairFootwearLeather, navigator.nextPage(repairFootwearLeather, "").url),
          (repairFurnitureHome, navigator.nextPage(repairFurnitureHome, "").url),
          (repairHouseholdAppliances, navigator.nextPage(repairHouseholdAppliances, "").url),
          (watchesAndJewellery, navigator.nextPage(watchesAndJewellery, "").url),
          (otherHouseholdGoods, navigator.nextPage(otherHouseholdGoods, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHouseholdRepairLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitHouseholdRepairLvl4Page().url
              )
                .withFormUrlEncodedBody("householdRepair4" -> value)
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
          controller.submitHouseholdRepairLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitHouseholdRepairLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of personal or household goods your undertaking repairs or maintains"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMembershipOrgsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMembershipOrgsLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadMembershipOrgsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-businessEmployersMembership", "Business and employers membership organisations"),
          ("sector-label-professionalMembership", "Professional membership organisations")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMembershipOrgsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (businessEmployersMembership, navigator.nextPage(businessEmployersMembership, "").url),
          (professionalMembership, navigator.nextPage(professionalMembership, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMembershipOrgsLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitMembershipOrgsLvl4Page().url
              )
                .withFormUrlEncodedBody("membership4" -> value)
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
          controller.submitMembershipOrgsLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitMembershipOrgsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of business membership organisation your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadMotorVehiclesRepairLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadMotorVehiclesRepairLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadMotorVehiclesRepairLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-repairMotorVehicles", "Motor vehicles"),
          ("sector-label-repairMotorcycles", "Motorcycles")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitMotorVehiclesRepairLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (repairMotorVehicles, navigator.nextPage(repairMotorVehicles, "").url),
          (repairMotorcycles, navigator.nextPage(repairMotorcycles, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitMotorVehiclesRepairLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitMotorVehiclesRepairLvl4Page().url
              )
                .withFormUrlEncodedBody("vehiclesRepair4" -> value)
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
          controller.submitMotorVehiclesRepairLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitMotorVehiclesRepairLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of vehicles your undertaking repairs or maintains"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherMembershipOrgsLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherMembershipOrgsLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadOtherMembershipOrgsLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-politicalOrganisations", "Political organisations"),
          ("sector-label-religiousOrganisations", "Religious organisations"),
          ("sector-label-otherMembershipOrganisations", "Another type of other membership organisation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherMembershipOrgsLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (politicalOrganisations, navigator.nextPage(politicalOrganisations, "").url),
          (religiousOrganisations, navigator.nextPage(religiousOrganisations, "").url),
          (otherMembershipOrganisations, navigator.nextPage(otherMembershipOrganisations, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherMembershipOrgsLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitOtherMembershipOrgsLvl4Page().url
              )
                .withFormUrlEncodedBody("otherMembership4" -> value)
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
          controller.submitOtherMembershipOrgsLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitOtherMembershipOrgsLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other membership organisation your undertaking operates"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherPersonalServicesLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherPersonalServicesLvl4Page()(
            FakeRequest(GET, routes.OtherServicesController.loadOtherPersonalServicesLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-domesticPersonalServices", "Domestic personal services"),
          ("sector-label-otherPersonalServices", "Any other personal service activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherPersonalServicesLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (domesticPersonalServices, navigator.nextPage(domesticPersonalServices, "").url),
          (otherPersonalServices4, navigator.nextPage(otherPersonalServices4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherPersonalServicesLvl4Page()(
              FakeRequest(
                POST,
                routes.OtherServicesController.submitOtherPersonalServicesLvl4Page().url
              )
                .withFormUrlEncodedBody("otherPersonal4" -> value)
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
          controller.submitOtherPersonalServicesLvl4Page()(
            FakeRequest(POST, routes.OtherServicesController.submitOtherPersonalServicesLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of other personal services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
