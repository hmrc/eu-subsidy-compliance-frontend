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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.ConfirmDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.{NaceLevel4, NaceLevel4Catalogue}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NACECheckDetailsController @Inject()(
                                            mcc: MessagesControllerComponents,
                                            store: Store,
                                            actionBuilders: ActionBuilders,
                                            naceCYAView: ConfirmDetailsPage,
                                            navigator: Navigator
                                          )(implicit ec: ExecutionContext, appConfig: AppConfig) extends BaseController(mcc) {

  import actionBuilders._

  private val confirmDetailsForm: Form[FormValues] = formWithSingleMandatoryField("confirmDetails")

  private def getSelectionPage(naceCode: String): String = naceCode match {
    case "D" => routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage.url
    case "35" => routes.AccomodationUtilitiesController.loadElectricityLvl3Page().url
    case s if s.startsWith("35.") && s.length == 4 => routes.AccomodationUtilitiesController.loadElectricityLvl3Page().url
    case s if s.startsWith("35.2") => routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page.url
    case s if s.startsWith("35.1") => routes.AccomodationUtilitiesController.loadElectricityLvl4Page.url
    case "G" => routes.RetailWholesaleController.loadRetailWholesaleLvl2Page.url
    case "46" => routes.RetailWholesaleController.loadWholesaleLvl3Page.url
    case s if s.startsWith("46.") && s.length == 4 => routes.RetailWholesaleController.loadWholesaleLvl3Page.url
    case s if s.startsWith("46.2") => routes.RetailWholesaleController.loadAgriculturalLvl4Page.url
    case _ => navigator.nextPage(naceCode, false).url
  }

  def getCheckDetails: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    implicit val messages: Messages = mcc.messagesApi.preferred(request)

    val selectedLevel4Code = request.queryString
      .collectFirst {
        case (key, values) if key.endsWith("4") && values.nonEmpty => values.head
      }
      .getOrElse("")

    if (selectedLevel4Code.nonEmpty) {
      val naceLevel4Code = selectedLevel4Code
      val naceLevel3Code = if (naceLevel4Code.length >= 4) naceLevel4Code.take(4) else naceLevel4Code
      val naceLevel2Code = if (naceLevel4Code.length >= 2) naceLevel4Code.take(2) else naceLevel4Code

      val naceLevel1Code = naceLevel2Code match {
        case "01" | "02" | "03" => "A"
        case "05"|"06"|"07"|"08"|"09" => "B"
        case "10"|"11"|"12"|"13"|"14"|"15"|"16"|"17"=> "C"
        case "18"|"19"|"20"|"21"|"22"|"23"|"24"|"25"=> "C"
        case "26"|"27"|"28"|"29"|"30"|"31"|"32"|"33"=> "C"
        case "35" => "D"
        case "36"| "37" |"38" |"39" => "E"
        case "41"| "42" |"43" => "F"
        case "46" | "47" => "G"
        case "49"| "50" |"51" |"52" |"53" => "H"
        case "55"| "56" => "I"
        case "58"| "59"| "60" => "J"
        case "61" | "62" | "63" => "K"
        case "64" | "65" | "66" => "L"
        case "68" => "M"
        case "69" | "70" | "71" | "72" | "73" | "74" | "75" => "N"
        case "77" | "78" | "79" | "80" | "81" | "82" => "O"
        case "84" => "P"
        case "85" => "Q"
        case "86" | "87" | "88" => "R"
        case "90"| "91"| "92" | "93" => "S"
        case "94" | "95" |"96" => "T"
        case "97"| "98" => "U"
        case "99" => "V"
        case _ => "D"
      }

      val level1Display = messages(s"NACE.$naceLevel1Code")
      val level2Display = messages(s"NACE.$naceLevel2Code")
      val level3Display = messages(s"NACE.$naceLevel3Code")

      val level4Heading = messages(s"NACE.$naceLevel4Code.heading")
      val level4Display = if (level4Heading.startsWith(naceLevel4Code)) {
        level4Heading.substring(naceLevel4Code.length + 1).trim
      } else {
        level4Heading
      }

      val changeSectorUrl = routes.UndertakingController.getSector.url
      val changeLevel1Url = getSelectionPage(naceLevel1Code)
      val changeLevel2Url = getSelectionPage(naceLevel2Code)
      val changeLevel3Url = getSelectionPage(naceLevel3Code)
      val changeLevel4Url = getSelectionPage(naceLevel4Code)

      val naceLevel4Notes = NaceLevel4Catalogue.fromMessages(naceLevel4Code)(messages)
        .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceLevel4Code"))

      val sector = Sector.other

      Ok(naceCYAView(
        confirmDetailsForm,
        sector,
        level1Display,
        level2Display,
        level3Display,
        level4Display,
        naceLevel1Code,
        naceLevel2Code,
        naceLevel3Code,
        naceLevel4Code,
        naceLevel4Notes,
        changeSectorUrl,
        changeLevel1Url,
        changeLevel2Url,
        changeLevel3Url,
        changeLevel4Url
      )(request, messages, appConfig)).toFuture

    } else {
      Redirect(routes.UndertakingController.getSector).toFuture
    }
  }

  def postCheckDetails: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    confirmDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Redirect(routes.AccomodationUtilitiesController.loadGasManufactureLvl4Page).toFuture
        },
        form => {
          if (form.value == "true") {
            Redirect(routes.UndertakingController.getAddBusiness).toFuture
          } else {
            Redirect(routes.UndertakingController.getSector).toFuture
          }
        }
      )
  }
}