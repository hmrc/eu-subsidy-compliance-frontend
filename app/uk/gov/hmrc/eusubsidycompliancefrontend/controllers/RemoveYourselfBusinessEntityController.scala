/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.OptionT
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.{formWithSingleMandatoryField}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityRemovedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveYourselfBusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  timeProvider: TimeProvider,
  emailService: EmailService,
  auditService: AuditService,
  eoriPage: BusinessEntityEoriController,
  removeYourselfBEPage: BusinessEntityRemoveYourselfPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val removeYourselfBusinessForm = formWithSingleMandatoryField("removeYourselfBusinessEntity")

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("RemoveYourselfBusinessEntityController.getRemoveYourselfBusinessEntity")

    val previous = routes.AccountController.getAccountPage.url

    escService
      .retrieveUndertaking(eori)
      .toContext
      .fold {
        logger.info(
          s"Could not find undertaking for $eori, redirecting to AddBusinessEntityController.getAddBusinessEntity"
        )
        Redirect(routes.AddBusinessEntityController.getAddBusinessEntity())
      } { undertaking =>
        logger.info(
          s"Found undertaking for $eori, showing removeYourselfBEPage"
        )
        Ok(removeYourselfBEPage(removeYourselfBusinessForm, undertaking.getBusinessEntityByEORI(eori), previous))
      }
  }

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("RemoveYourselfBusinessEntityController.postRemoveYourselfBusinessEntity")

    def handleFormSubmission(undertaking: Undertaking, businessEntity: BusinessEntity): OptionT[Future, Result] = {
      removeYourselfBusinessForm
        .bindFromRequest()
        .fold(
          errors => {
            logger.warn("Failed validating postRemoveYourselfBusinessEntity, showing errors")
            BadRequest(
              removeYourselfBEPage(errors, businessEntity, routes.AccountController.getAccountPage.url)
            ).toContext
          }, {
            logger
              .info(s"Passed validating postRemoveYourselfBusinessEntity, handleValidFormSubmission for $undertaking")
            handleValidFormSubmission(undertaking, businessEntity)
          }
        )
    }

    def handleValidFormSubmission(undertaking: Undertaking, businessEntity: BusinessEntity)(
      form: FormValues
    ): OptionT[Future, Result] =
      if (form.value.isTrue) {
        for {
          _ <- escService.removeMember(undertaking.reference, businessEntity).toContext
          _ <- store.deleteAll.toContext
          effectiveDate = DateFormatter.govDisplayFormat(timeProvider.today.plusDays(1))
          _ <- emailService.sendEmail(eori, MemberRemoveSelfToBusinessEntity, undertaking, effectiveDate).toContext
          _ <- emailService
            .sendEmail(undertaking.getLeadEORI, eori, MemberRemoveSelfToLead, undertaking, effectiveDate)
            .toContext
          _ = auditService.sendEvent(
            BusinessEntityRemovedSelf(undertaking.reference, request.authorityId, undertaking.getLeadEORI, eori)
          )
        } yield Redirect(routes.SignOutController.signOut())
      } else Redirect(routes.AccountController.getAccountPage).toContext

    val result = for {
      undertaking <- escService.retrieveUndertaking(eori).toContext
      businessEntityToRemove <- undertaking.findBusinessEntity(eori).toContext
      r <- handleFormSubmission(undertaking, businessEntityToRemove)
    } yield r

    result.getOrElse(handleMissingSessionData("Undertaking"))
  }
}
