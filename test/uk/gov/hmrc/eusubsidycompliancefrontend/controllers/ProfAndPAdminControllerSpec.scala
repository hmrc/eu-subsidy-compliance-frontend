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

class ProfAndPAdminControllerSpec
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

  private val controller = instanceOf[ProfAndPAdminController]
  private val navigator = instanceOf[Navigator]

  private object SectorCodes {
    val administrationGeneral = "84.1"
    val compulsorySocialSecurity4 = "84.30"
    val communityServices = "84.2"
    val generalPublicAdmin = "84.11"
    val healthEducationRegulation = "84.12"
    val businessRegulation = "84.13"
    val defence = "84.22"
    val fireService = "84.25"
    val foreignAffairs = "84.21"
    val justiceJudicial = "84.23"
    val publicOrderSafety = "84.24"
    val advertising = "73.1"
    val marketResearch4 = "73.20"
    val publicRelations4 = "73.30"
    val advertisingActivities = "73.11"
    val mediaRepresentation = "73.12"
    val architecturalAndTechnicalActivities = "71.1"
    val technicalTesting4 = "71.20"
    val architecturalEngineering = "71.11"
    val engineeringConsultancy = "71.12"
    val headOfficesActivities4 = "70.10"
    val managementConsultancyActivities4 = "70.20"
    val accountingTaxConsultancy = "69.20"
    val legalActivities4 = "69.10"
    val photographicActivities = "74.20"
    val specialisedDesign = "74.1"
    val translationActivities4 = "74.30"
    val otherProfessionalScientificActivities = "74.9"
    val patentBrokering = "74.91"
    val otherProfessionalScientificActivities4 = "74.99"
    val advertisingMarketResearch = "73"
    val architecturalAndTechnical = "71"
    val headOfficesAndManagementConsultancy = "70"
    val legalAndAccounting = "69"
    val scientificResearchAndDevelopment = "72"
    val veterinaryActivities4 = "75.00"
    val otherProfessionalScientific = "74"
    val naturalScientificResearchAndDevelopment4 = "72.10"
    val socialScientificResearchAndDevelopment4 = "72.20"
    val graphicDesign = "74.12"
    val industrialFashionDesign = "74.11"
    val interiorDesign = "74.13"
    val otherDesign = "74.14"
  }

  import SectorCodes._

  "ProfAndPAdminController" should {
    "loadPublicAdminDefenceLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPublicAdminDefenceLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadPublicAdminDefenceLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-administrationGeneral",
            "Administration of the State and the economic, social and environmental policies of the community"
          ),
          ("sector-label-compulsorySocialSecurity", "Compulsory social security activities"),
          ("sector-label-communityServices", "Provision of services to the community as a whole")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPublicAdminDefenceLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (administrationGeneral, navigator.nextPage(administrationGeneral, "").url),
          (compulsorySocialSecurity4, navigator.nextPage(compulsorySocialSecurity4, "").url),
          (communityServices, navigator.nextPage(communityServices, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPublicAdminDefenceLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitPublicAdminDefenceLvl3Page().url
              )
                .withFormUrlEncodedBody("publicAdmin3" -> value)
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
          controller.submitPublicAdminDefenceLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitPublicAdminDefenceLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in public administration"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadPublicAdminLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadPublicAdminLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadPublicAdminLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-generalPublicAdmin", "General public administration"),
          (
            "sector-label-healthEducationRegulation",
            "Regulation of health care, education, cultural services and other social services"
          ),
          (
            "sector-label-businessRegulation",
            "Regulation of and contribution to more efficient operation of businesses"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitPublicAdminLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (generalPublicAdmin, navigator.nextPage(generalPublicAdmin, "").url),
          (healthEducationRegulation, navigator.nextPage(healthEducationRegulation, "").url),
          (businessRegulation, navigator.nextPage(businessRegulation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitPublicAdminLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitPublicAdminLvl4Page().url
              )
                .withFormUrlEncodedBody("publicAdmin4" -> value)
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
          controller.submitPublicAdminLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitPublicAdminLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in public administration"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadServiceProvisionLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadServiceProvisionLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadServiceProvisionLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-defence", "Defence"),
          ("sector-label-fireService", "Fire service activities"),
          ("sector-label-foreignAffairs", "Foreign affairs"),
          ("sector-label-justiceJudicial", "Justice and judicial activities"),
          ("sector-label-publicOrderSafety", "Public order and safety")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitServiceProvisionLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (defence, navigator.nextPage(defence, "").url),
          (fireService, navigator.nextPage(fireService, "").url),
          (foreignAffairs, navigator.nextPage(foreignAffairs, "").url),
          (justiceJudicial, navigator.nextPage(justiceJudicial, "").url),
          (publicOrderSafety, navigator.nextPage(publicOrderSafety, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitServiceProvisionLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitServiceProvisionLvl4Page().url
              )
                .withFormUrlEncodedBody("serviceProvision4" -> value)
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
          controller.submitServiceProvisionLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitServiceProvisionLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in provision of services to the community"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAdvertisingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAdvertisingLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadAdvertisingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-advertising", "Advertising"),
          ("sector-label-marketResearch", "Market research and public opinion polling"),
          ("sector-label-publicRelations", "Public relations and communication")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAdvertisingLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (advertising, navigator.nextPage(advertising, "").url),
          (marketResearch4, navigator.nextPage(marketResearch4, "").url),
          (publicRelations4, navigator.nextPage(publicRelations4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAdvertisingLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitAdvertisingLvl3Page().url
              )
                .withFormUrlEncodedBody("advertising3" -> value)
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
          controller.submitAdvertisingLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitAdvertisingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking''s main business activity in advertising, market research and public relations"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadAdvertisingLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadAdvertisingLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadAdvertisingLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-advertisingActivities", "Activities of advertising agencies"),
          ("sector-label-mediaRepresentation", "Media representation")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitAdvertisingLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (advertisingActivities, navigator.nextPage(advertisingActivities, "").url),
          (mediaRepresentation, navigator.nextPage(mediaRepresentation, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitAdvertisingLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitAdvertisingLvl4Page().url
              )
                .withFormUrlEncodedBody("advertising4" -> value)
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
          controller.submitAdvertisingLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitAdvertisingLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in advertising"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadArchitecturalLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArchitecturalLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadArchitecturalLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          (
            "sector-label-architecturalAndTechnicalActivities",
            "Architectural and engineering activities and related technical consultancy"
          ),
          ("sector-label-technicalTesting", "Technical testing and analysis")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitArchitecturalLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (architecturalAndTechnicalActivities, navigator.nextPage(architecturalAndTechnicalActivities, "").url),
          (technicalTesting4, navigator.nextPage(technicalTesting4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArchitecturalLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitArchitecturalLvl3Page().url
              )
                .withFormUrlEncodedBody("architecture3" -> value)
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
          controller.submitArchitecturalLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitArchitecturalLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in architecture and engineering, technical testing and analysis"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadArchitecturalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadArchitecturalLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadArchitecturalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-architecturalEngineering", "Architectural activities"),
          ("sector-label-engineeringConsultancy", "Engineering activities and related technical consultancy")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitArchitecturalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (architecturalEngineering, navigator.nextPage(architecturalEngineering, "").url),
          (engineeringConsultancy, navigator.nextPage(engineeringConsultancy, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitArchitecturalLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitArchitecturalLvl4Page().url
              )
                .withFormUrlEncodedBody("architecture4" -> value)
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
          controller.submitArchitecturalLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitArchitecturalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in architecture, engineering and technical consultancy"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadHeadOfficesLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadHeadOfficesLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadHeadOfficesLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-headOffice", "Activities of head offices"),
          ("sector-label-managementConsultancy", "Business and other management consultancy activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitHeadOfficesLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (headOfficesActivities4, navigator.nextPage(headOfficesActivities4, "").url),
          (managementConsultancyActivities4, navigator.nextPage(managementConsultancyActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitHeadOfficesLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitHeadOfficesLvl3Page().url
              )
                .withFormUrlEncodedBody("headOffice3" -> value)
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
          controller.submitHeadOfficesLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitHeadOfficesLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select your undertaking’s main business activity in head offices and management consultancy"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadLegalAndAccountingLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadLegalAndAccountingLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadLegalAndAccountingLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-accounting", "Accounting, bookkeeping, auditing and tax consultancy"),
          ("sector-label-legal", "Legal activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitLegalAndAccountingLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (accountingTaxConsultancy, navigator.nextPage(accountingTaxConsultancy, "").url),
          (legalActivities4, navigator.nextPage(legalActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitLegalAndAccountingLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitLegalAndAccountingLvl3Page().url
              )
                .withFormUrlEncodedBody("legal3" -> value)
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
          controller.submitLegalAndAccountingLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitLegalAndAccountingLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select your undertaking’s main business activity in legal and accounting services"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherProfessionalLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherProfessionalLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadOtherProfessionalLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-photography", "Photography"),
          ("sector-label-specialisedDesign", "Specialised design activities"),
          ("sector-label-translationInterpretation", "Translation and interpretation"),
          (
            "sector-label-otherProfessionalScientificActivities",
            "Other professional, scientific and technical activities"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherProfessionalLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (photographicActivities, navigator.nextPage(photographicActivities, "").url),
          (specialisedDesign, navigator.nextPage(specialisedDesign, "").url),
          (translationActivities4, navigator.nextPage(translationActivities4, "").url),
          (otherProfessionalScientificActivities, navigator.nextPage(otherProfessionalScientificActivities, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherProfessionalLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitOtherProfessionalLvl3Page().url
              )
                .withFormUrlEncodedBody("otherProf3" -> value)
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
          controller.submitOtherProfessionalLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitOtherProfessionalLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of professional, scientific and technical activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadOtherProfessionalLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadOtherProfessionalLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadOtherProfessionalLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-patentBrokering", "Patent brokering and marketing service activities"),
          (
            "sector-label-otherProfessionalScientificActivities4",
            "All other professional, scientific and technical activities"
          )
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitOtherProfessionalLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (patentBrokering, navigator.nextPage(patentBrokering, "").url),
          (otherProfessionalScientificActivities4, navigator.nextPage(otherProfessionalScientificActivities4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitOtherProfessionalLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitOtherProfessionalLvl4Page().url
              )
                .withFormUrlEncodedBody("otherProf4" -> value)
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
          controller.submitOtherProfessionalLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitOtherProfessionalLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of other professional, scientific and technical activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadProfessionalLvl2Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadProfessionalLvl2Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadProfessionalLvl2Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-advertisingMarketResearch", "Advertising, market research and public relations"),
          ("sector-label-architecturalAndTechnical", "Architectural and engineering, technical testing and analysis"),
          ("sector-label-headOfficesAndManagementConsultancy", "Head offices and management consultancy"),
          ("sector-label-legalAndAccounting", "Legal and accounting"),
          ("sector-label-ScientificResearchAndDevelopment", "Scientific research and development"),
          ("sector-label-veterinary", "Veterinary activities"),
          ("sector-label-otherProfessionalScientific", "Other professional, scientific and technical activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitProfessionalLvl2Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (advertisingMarketResearch, navigator.nextPage(advertisingMarketResearch, "").url),
          (architecturalAndTechnical, navigator.nextPage(architecturalAndTechnical, "").url),
          (headOfficesAndManagementConsultancy, navigator.nextPage(headOfficesAndManagementConsultancy, "").url),
          (legalAndAccounting, navigator.nextPage(legalAndAccounting, "").url),
          (scientificResearchAndDevelopment, navigator.nextPage(scientificResearchAndDevelopment, "").url),
          (veterinaryActivities4, navigator.nextPage(veterinaryActivities4, "").url),
          (otherProfessionalScientific, navigator.nextPage(otherProfessionalScientific, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitProfessionalLvl2Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitProfessionalLvl2Page().url
              )
                .withFormUrlEncodedBody("prof2" -> value)
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
          controller.submitProfessionalLvl2Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitProfessionalLvl2Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg =
          "Select the type of professional, scientific and technical services your undertaking provides"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadScientificRAndDLvl3Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadScientificRAndDLvl3Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadScientificRAndDLvl3Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-naturalScientificResearchAndDevelopment", "Natural sciences and engineering"),
          ("sector-label-socialScientificResearchAndDevelopment", "Social sciences and humanities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitScientificRAndDLvl3Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (
            naturalScientificResearchAndDevelopment4,
            navigator.nextPage(naturalScientificResearchAndDevelopment4, "").url
          ),
          (socialScientificResearchAndDevelopment4, navigator.nextPage(socialScientificResearchAndDevelopment4, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitScientificRAndDLvl3Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitScientificRAndDLvl3Page().url
              )
                .withFormUrlEncodedBody("rAndD3" -> value)
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
          controller.submitScientificRAndDLvl3Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitScientificRAndDLvl3Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the area your undertaking conducts scientific research and development in"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }
    "loadSpecialisedDesignLvl4Page" should {
      "return OK and render expected radio options" in {
        inSequence {
          mockAuthWithEnrolment()
        }
        val result =
          controller.loadSpecialisedDesignLvl4Page()(
            FakeRequest(GET, routes.ProfAndPAdminController.loadSpecialisedDesignLvl4Page().url)
          )
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        val radios = Table(
          ("id", "text"),
          ("sector-label-graphicDesign", "Graphic design and visual communication"),
          ("sector-label-industrialFashionDesign", "Industrial product and fashion design"),
          ("sector-label-interiorDesign", "Interior design"),
          ("sector-label-otherDesign", "Other specialised design activities")
        )
        forAll(radios) { (id, expected) =>
          val element = document.getElementById(id)
          element should not be null
          element.text() shouldBe expected
        }
      }
    }
    "submitSpecialisedDesignLvl4Page" should {
      "redirect to confirm details page on valid form submission" in {
        val radioButtons = Table(
          ("formValue", "expectedUrl"),
          (graphicDesign, navigator.nextPage(graphicDesign, "").url),
          (industrialFashionDesign, navigator.nextPage(industrialFashionDesign, "").url),
          (interiorDesign, navigator.nextPage(interiorDesign, "").url),
          (otherDesign, navigator.nextPage(otherDesign, "").url)
        )
        forAll(radioButtons) { (value: String, expectedUrl: String) =>
          inSequence {
            mockAuthWithEnrolment()
          }
          val result =
            controller.submitSpecialisedDesignLvl4Page()(
              FakeRequest(
                POST,
                routes.ProfAndPAdminController.submitSpecialisedDesignLvl4Page().url
              )
                .withFormUrlEncodedBody("specialDesign4" -> value)
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
          controller.submitSpecialisedDesignLvl4Page()(
            FakeRequest(POST, routes.ProfAndPAdminController.submitSpecialisedDesignLvl4Page().url)
          )
        status(result) shouldBe BAD_REQUEST
        val document = Jsoup.parse(contentAsString(result))
        val expectedErrorMsg = "Select the type of specialised design activities your undertaking does"
        val summary = document.selectFirst(".govuk-error-summary")
        summary should not be null
        summary.selectFirst(".govuk-error-summary__title").text() shouldBe "There is a problem"
        summary.text() should include(expectedErrorMsg)
      }
    }

  }
}
