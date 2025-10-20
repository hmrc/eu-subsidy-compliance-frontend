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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, Sector}
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.nace.ConfirmDetailsPage
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.NaceLevel4Catalogue
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.NaceCheckDetailsViewModel
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney


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
  val NewRegChangeMode = appConfig.NewRegChangeMode

  private def getLevel1ChangeUrl(level1Code: String, level2Code: String): String = level1Code match {
    case "A" =>
      if (level2Code == "02") routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
      else routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
    case "C" | "F" | "G" | "H" | "J" | "M" | "N" | "O" =>
      routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
    case "B" | "D" | "E" | "I" | "K" | "L" | "P" | "Q" | "R" | "S" | "T" | "U" | "V" =>
      routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingOtherPage().url
    case _ =>
      routes.GeneralTradeGroupsController.loadGeneralTradeUndertakingPage().url
  }

  private def getLevel1_1ChangeUrl(level2Code: String): String = level2Code match {
    case "13" | "14" | "15" | "16" | "22" =>
      routes.GeneralTradeGroupsController.loadClothesTextilesHomewarePage().url
    case "26" | "27" | "28" | "33" =>
      routes.GeneralTradeGroupsController.loadComputersElectronicsMachineryPage().url
    case "10" | "11" | "12" =>
      routes.GeneralTradeGroupsController.loadFoodBeveragesTobaccoPage().url
    case "19" | "20" | "21" | "23" | "24" | "25" =>
      routes.GeneralTradeGroupsController.loadMetalsChemicalsMaterialsPage().url
    case "17" | "18" =>
      routes.GeneralTradeGroupsController.loadPaperPrintedProductsPage().url
    case "29" | "30" | "32" =>
      routes.GeneralTradeGroupsController.loadVehiclesTransportPage().url
    case _ =>
      routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
  }

  private def getLevel1_1Display(level2Code: String)(implicit messages: Messages): String = level2Code match {
    case "13" | "14" | "15" | "16" | "22" => messages("NACE.Manufacturing.Lvl2.1.Clothes")
    case "26" | "27" | "28" | "33" => messages("NACE.Manufacturing.Lvl2.1.Computers")
    case "10" | "11" | "12" => messages("NACE.Manufacturing.Lvl2.1.Food")
    case "19" | "20" | "21" | "23" | "24" | "25" => messages("NACE.Manufacturing.Lvl2.1.Metals")
    case "17" | "18" => messages("NACE.Manufacturing.Lvl2.1.Paper")
    case "29" | "30" | "32" => messages("NACE.Manufacturing.Lvl2.1.Vehicles")
    case _ => ""
  }

  def deriveLevel1Code(level2Code: String): String = level2Code match {
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

  private def toNavigatorCode(level1Code: String, level2Code: String): String = level1Code match {
    case "A" => level2Code match {
      case "01" | "02" => "02"
      case "03" => "03"
      case _ => level2Code
    }
    case _ => level2Code
  }

  private def buildViewModel(naceLevel4Code: String)(implicit messages: Messages): NaceCheckDetailsViewModel = {
    val naceLevel3Code = if (naceLevel4Code.length >= 4) naceLevel4Code.take(4) else naceLevel4Code
    val naceLevel2Code = if (naceLevel4Code.length >= 2) naceLevel4Code.take(2) else naceLevel4Code
    val naceLevel1Code = deriveLevel1Code(naceLevel2Code)

    val level1Display = {
      val rawDisplay = messages(s"NACE.$naceLevel1Code")
      if (rawDisplay.nonEmpty) {
        rawDisplay.charAt(0).toUpper.toString + rawDisplay.substring(1).toLowerCase
      } else {
        rawDisplay
      }
    }
    val level1_1Display = getLevel1_1Display(naceLevel2Code)
    val level2Display = messages(s"NACE.$naceLevel2Code")
    val level3Display = messages(s"NACE.$naceLevel3Code")

    val level4Heading = messages(s"NACE.$naceLevel4Code.heading")
    val level4Display = if (level4Heading.startsWith(naceLevel4Code)) {
      level4Heading.substring(naceLevel4Code.length + 1).trim
    } else {
      level4Heading
    }

    val sector = naceLevel2Code match {
      case "01" => Sector.agriculture
      case "03" => Sector.aquaculture
      case _ => Sector.other
    }

    val showLevel1 = naceLevel1Code match {
      case "A" => false
      case _ => true
    }

    val showLevel1_1 = naceLevel1Code == "C"

    val showLevel2 = {
      naceLevel1Code match {
        case "D" | "A"| "Q" | "M" => false
        case _ => true
      }
    }

    val showLevel3 = !naceLevel3Code.endsWith(".0")

    val showLevel4 = {
      val level4Last2Digits = naceLevel4Code.takeRight(2)
      val level3LastDigit = naceLevel3Code.takeRight(1)
      !(level4Last2Digits.endsWith("0") && level4Last2Digits.charAt(0).toString == level3LastDigit)
    }

    val changeSectorUrl = routes.UndertakingController.getSector.url
    val changeLevel1Url = getLevel1ChangeUrl(naceLevel1Code, naceLevel2Code)
    val changeLevel1_1Url = routes.GeneralTradeGroupsController.loadLvl2_1GroupsPage().url
    val navigatorLevel2Code = naceLevel2Code

    val changeLevel2Url = if (showLevel2) {
      naceLevel1Code match {
        case "C" => getLevel1_1ChangeUrl(naceLevel2Code)
        case "F" => navigator.nextPage(naceLevel1Code, "").url
        case _ => navigator.nextPage(navigatorLevel2Code, "").url
      }
    } else {
      navigator.nextPage(navigatorLevel2Code, "").url
    }

    val changeLevel3Url = navigator.nextPage(navigatorLevel2Code, "").url

    val changeLevel4Url = if (showLevel3) {
      navigator.nextPage(naceLevel3Code, "").url
    } else {
      navigator.nextPage(navigatorLevel2Code, "").url
    }

    NaceCheckDetailsViewModel(
      naceLevel1Display = level1Display,
      naceLevel1_1Display = level1_1Display,
      naceLevel2Display = level2Display,
      naceLevel3Display = level3Display,
      naceLevel4Display = level4Display,
      naceLevel1Code = naceLevel1Code,
      naceLevel2Code = naceLevel2Code,
      naceLevel3Code = naceLevel3Code,
      naceLevel4Code = naceLevel4Code,
      sector = sector,
      changeSectorUrl = changeSectorUrl,
      changeLevel1Url = changeLevel1Url,
      changeLevel1_1Url = changeLevel1_1Url,
      changeLevel2Url = changeLevel2Url,
      changeLevel3Url = changeLevel3Url,
      changeLevel4Url = changeLevel4Url,
      showLevel1 = showLevel1,
      showLevel1_1 = showLevel1_1,
      showLevel2 = showLevel2,
      showLevel3 = showLevel3,
      showLevel4 = showLevel4
    )
  }

  def getCheckDetails(usersLastAnswer: String) : Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val messages: Messages = mcc.messagesApi.preferred(request)
    store.update[UndertakingJourney](_.copy(mode = NewRegChangeMode))

    if (usersLastAnswer.nonEmpty) {
      val viewModel = buildViewModel(usersLastAnswer)(messages)
      val naceLevel4Notes = NaceLevel4Catalogue.fromMessages(usersLastAnswer)(messages)
        .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $usersLastAnswer"))

      println(
        s"""
           |Debug URLs for NACE code ${viewModel.naceLevel4Code}:
           |  Level 1 Code: ${viewModel.naceLevel1Code} - Display: ${viewModel.naceLevel1Display}
           |  Level 1.1 Display: ${viewModel.naceLevel1_1Display}
           |  Level 2 Code: ${viewModel.naceLevel2Code} - Display: ${viewModel.naceLevel2Display}
           |  Level 3 Code: ${viewModel.naceLevel3Code} - Display: ${viewModel.naceLevel3Display}
           |  Level 4 Code: ${viewModel.naceLevel4Code} - Display: ${viewModel.naceLevel4Display}
           |  changeSectorUrl: ${viewModel.changeSectorUrl}
           |  changeLevel1Url: ${viewModel.changeLevel1Url}
           |  changeLevel1_1Url: ${viewModel.changeLevel1_1Url}
           |  changeLevel2Url: ${viewModel.changeLevel2Url}
           |  changeLevel3Url: ${viewModel.changeLevel3Url}
           |  changeLevel4Url: ${viewModel.changeLevel4Url}
           |  showLevel1: ${viewModel.showLevel1}
           |  showLevel1_1: ${viewModel.showLevel1_1}
           |  showLevel2: ${viewModel.showLevel2}
           |  showLevel3: ${viewModel.showLevel3}
           |  showLevel4: ${viewModel.showLevel4}
           |"""
      )

      Ok(naceCYAView(confirmDetailsForm, viewModel, naceLevel4Notes)(request, messages, appConfig)).toFuture
    } else {
      Redirect(routes.UndertakingController.getSector).toFuture
    }
  }

  def postCheckDetails: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val messages: Messages = mcc.messagesApi.preferred(request)

    confirmDetailsForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          request.body.asFormUrlEncoded.flatMap(_.get("naceCode").flatMap(_.headOption)) match {
            case Some(naceLevel4Code) =>
              val viewModel = buildViewModel(naceLevel4Code)(messages)
              val naceLevel4Notes = NaceLevel4Catalogue.fromMessages(naceLevel4Code)(messages)
                .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceLevel4Code"))

              BadRequest(naceCYAView(formWithErrors, viewModel, naceLevel4Notes)(request, messages, appConfig)).toFuture
            case None =>
              Redirect(routes.UndertakingController.getSector).toFuture
          }
        },
        form => {
          store.update[UndertakingJourney](_.copy(mode = appConfig.NewRegMode))
          if (form.value == "true") {
            Redirect(routes.UndertakingController.getAddBusiness).toFuture
          } else {
            Redirect(routes.UndertakingController.getSector).toFuture
          }
        }
      )
  }
}