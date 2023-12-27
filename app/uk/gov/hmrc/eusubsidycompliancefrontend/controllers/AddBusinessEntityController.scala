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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.forms.FormHelpers.formWithSingleMandatoryField
import uk.gov.hmrc.eusubsidycompliancefrontend.journeys.BusinessEntityJourney
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.FormValues
import uk.gov.hmrc.eusubsidycompliancefrontend.persistence.Store
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  override val store: Store,
  override val escService: EscService,
  timeProvider: TimeProvider,
  emailService: EmailService,
  auditService: AuditService,
  addBusinessPage: AddBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport
    with ControllerFormHelpers {

  import actionBuilders._

  private val addBusinessForm = formWithSingleMandatoryField("addBusiness")

  def startJourney(
    businessAdded: Option[Boolean] = None,
    businessRemoved: Option[Boolean] = None,
    newlyAddedEoriOpt: Option[String] = None,
    removedAddedEoriOpt: Option[String] = None
  ): Action[AnyContent] =
    verifiedEori.async { implicit request =>
      withLeadUndertaking { _ =>
        startNewJourney { _ =>
          Redirect(
            routes.AddBusinessEntityController
              .getAddBusinessEntity(businessAdded, businessRemoved, newlyAddedEoriOpt, removedAddedEoriOpt)
              .url
          )
        }
      }
    }

  def getAddBusinessEntity(
    businessAdded: Option[Boolean] = None,
    businessRemoved: Option[Boolean] = None,
    newlyAddedEoriOpt: Option[String] = None,
    removedAddedEoriOpt: Option[String] = None
  ): Action[AnyContent] = verifiedEori.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber
      logger.info("AddBusinessEntityController.getAddBusinessEntity")
      store
        .getOrCreate[BusinessEntityJourney](BusinessEntityJourney())
        .map { journey =>
          val form =
            journey.addBusiness.value
              .fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))

          logger.info("AddBusinessEntityController showing undertakingBusinessEntity")
          Ok(
            addBusinessPage(
              form = form,
              businessEntity = undertaking.undertakingBusinessEntity,
              businessAdded = businessAdded.getOrElse(false),
              businessRemoved = businessRemoved.getOrElse(false),
              newlyAddedEoriOpt = newlyAddedEoriOpt,
              removedAddedEoriOpt = removedAddedEoriOpt
            )
          )
        }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = verifiedEori.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    logger.info("AddBusinessEntityController.postAddBusinessEntity")
    def handleValidAnswer(form: FormValues) =
      if (form.value.isTrue) store.update[BusinessEntityJourney](_.setAddBusiness(form.value.toBoolean)).flatMap(_.next)
      else Redirect(routes.AccountController.getAccountPage).toFuture

    withLeadUndertaking { undertaking =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors => BadRequest(addBusinessPage(errors, undertaking.undertakingBusinessEntity)).toFuture,
          handleValidAnswer
        )
    }
  }

  protected def startNewJourney(
    f: BusinessEntityJourney => Result,
    businessAdded: Option[Boolean] = None,
    businessRemoved: Option[Boolean] = None
  )(implicit r: AuthenticatedEnrolledRequest[AnyContent]): Future[Result] = {
    implicit val eori: EORI = r.eoriNumber
    store
      .put[BusinessEntityJourney](BusinessEntityJourney())
      .toContext
      .map(journey => f(journey))
      .getOrElse(Redirect(routes.AddBusinessEntityController.getAddBusinessEntity(businessAdded, businessRemoved).url))

  }
}
