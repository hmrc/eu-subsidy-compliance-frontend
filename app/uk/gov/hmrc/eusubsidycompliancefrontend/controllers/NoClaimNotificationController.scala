/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscCDSActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.NonCustomsSubsidyNilReturn
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{FormValues, NilSubmissionDate, SubsidyUpdate}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, EscService, NilReturnJourney, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NoClaimNotificationController @Inject() (
  mcc: MessagesControllerComponents,
  escCDSActionBuilder: EscCDSActionBuilders,
  store: Store,
  override val escService: EscService,
  auditService: AuditService,
  timeProvider: TimeProvider,
  noClaimNotificationPage: NoClaimNotificationPage
)(implicit val appConfig: AppConfig, val executionContext: ExecutionContext)
    extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {
  import escCDSActionBuilder._

  def getNoClaimNotification: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      val previous = routes.AccountController.getAccountPage().url
      Ok(noClaimNotificationPage(noClaimForm, previous, undertaking.name)).toFuture
    }
  }

  def postNoClaimNotification: Action[AnyContent] = withCDSAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori = request.eoriNumber
      val previous = routes.AccountController.getAccountPage().url

      def handleValidNoClaim(form: FormValues): Future[Result] = {
        val nilSubmissionDate = timeProvider.today.plusDays(1)
        val result = for {
          reference <- undertaking.reference.toContext
          _ <- store.update[NilReturnJourney](e => e.copy(displayNotification = true)).toContext
          _ <- escService.createSubsidy(SubsidyUpdate(reference, NilSubmissionDate(nilSubmissionDate))).toContext
          _ = auditService
            .sendEvent(
              NonCustomsSubsidyNilReturn(request.authorityId, eori, reference, nilSubmissionDate)
            )
        } yield Redirect(routes.AccountController.getAccountPage())

        result.getOrElse(handleMissingSessionData("Undertaking ref"))
      }

      noClaimForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(noClaimNotificationPage(errors, previous, undertaking.name)).toFuture,
          handleValidNoClaim
        )
    }
  }

  private val noClaimForm: Form[FormValues] = Form(
    mapping("noClaimNotification" -> mandatory("noClaimNotification"))(FormValues.apply)(FormValues.unapply)
  )

}
