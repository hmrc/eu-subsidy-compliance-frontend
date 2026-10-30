/*
 * Copyright 2026 HM Revenue & Customs
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

/*
 * Copyright 2026 HM Revenue & Customs
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

import cats.implicits._
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.UndertakingJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.UndertakingUpdated
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.Sector
import uk.gov.hmrc.eusubsidycompliancefrontend.navigation.Navigator
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EmailService, EscService}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html.{AmendUndertakingPage, UpdateConfirmationPage}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.{NaceCheckDetailsViewModel, NaceLevel4Catalogue}
import uk.gov.hmrc.eusubsidycompliancefrontend.views.models.NaceCheckDetailsViewModelBuilder

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UndertakingAmendController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  override val escService: EscService,
  emailService: EmailService,
  auditService: AuditService,
  amendUndertakingPage: AmendUndertakingPage,
  updateConfirmationPage: UpdateConfirmationPage,
  navigator: Navigator,
  naceCheckDetailsViewModelBuilder: NaceCheckDetailsViewModelBuilder
)(implicit
  val appConfig: AppConfig,
  override val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import actionBuilders._

  def getAmendUndertakingDetails: Action[AnyContent] = enrolled.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val messages: Messages = mcc.messagesApi.preferred(request)

      store.getOrCreate[UndertakingJourney](UndertakingJourney()).flatMap { journey =>
        for {
          updateToAmendMode <- store.update[UndertakingJourney](_.copy(mode = appConfig.AmendNaceMode))
          updatedJourney <- if (journey.isAmend) journey.toFuture else updateIsAmendState(value = true)
          verifiedEmail <- emailService.retrieveVerifiedEmailAddressByEORI(eori)
        } yield {
          val naceCode = updatedJourney.sector.value.toString.dropRight(1).drop(5)

          if (naceCode.nonEmpty && naceCode.length == 5) {
            val viewModel = naceCheckDetailsViewModelBuilder.buildViewModel(naceCode)(messages)

            val naceLevel4Notes = NaceLevel4Catalogue
              .fromMessages(naceCode)(messages)
              .getOrElse(throw new IllegalStateException(s"No notes found for Level 4 code $naceCode"))

            Ok(
              amendUndertakingPage(
                updatedJourney.sector.value.getOrElse(handleMissingSessionData("Undertaking sector")),
                verifiedEmail,
                viewModel,
                naceLevel4Notes,
                previous = routes.AccountController.getAccountPage.url
              )
            )
          } else {
            logger.warn(s"Invalid NACE code: $naceCode")
            Redirect(routes.UndertakingController.getSector)
          }
        }
      }
    }
  }

  private def updateIsAmendState(value: Boolean)(implicit eori: EORI): Future[UndertakingJourney] = {
    for {
      updateName <- store.update[UndertakingJourney](_.setUndertakingName(eori.value))
      updateIsAmend <- store.update[UndertakingJourney](_.copy(isAmend = value))
    } yield updateIsAmend
  }

  def postAmendUndertaking: Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      val result = for {
        updatedJourney <- updateIsAmendState(value = false).toContext
        undertakingSector <- updatedJourney.sector.value.toContext
        retrievedUndertaking <- escService.retrieveUndertaking(eori).toContext
        undertakingRef <- retrievedUndertaking.reference.toContext
        updatedUndertaking = retrievedUndertaking.copy(industrySector = undertakingSector)
        _ <- escService.updateUndertaking(updatedUndertaking).toContext
        _ = auditService.sendEvent(
          UndertakingUpdated(
            request.authorityId,
            eori,
            undertakingRef,
            updatedUndertaking.name,
            updatedUndertaking.industrySector
          )
        )
      } yield Ok(updateConfirmationPage(undertakingRef, eori))
      result.getOrElse(handleMissingSessionData("Undertaking Journey"))
    }
  }
}
