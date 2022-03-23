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

import cats.implicits.{catsSyntaxEq, catsSyntaxOptionId}
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.EscActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  override val store: Store,
  override val escService: EscService,
  journeyTraverseService: JourneyTraverseService,
  timeProvider: TimeProvider,
  sendEmailHelperService: SendEmailHelperService,
  auditService: AuditService,
  addBusinessPage: AddBusinessPage,
  eoriPage: BusinessEntityEoriPage,
  removeYourselfBEPage: BusinessEntityRemoveYourselfPage,
  businessEntityCyaPage: BusinessEntityCYAPage,
  removeBusinessPage: RemoveBusinessPage
)(implicit
  val appConfig: AppConfig,
  val executionContext: ExecutionContext
) extends BaseController(mcc)
    with LeadOnlyUndertakingSupport {

  import escActionBuilders._

  private val eoriPrefix = "GB"

  private val AddMemberEmailToBusinessEntity = "addMemberEmailToBE"
  private val AddMemberEmailToLead = "addMemberEmailToLead"
  private val RemoveMemberEmailToBusinessEntity = "removeMemberEmailToBE"
  private val RemoveMemberEmailToLead = "removeMemberEmailToLead"
  private val RemoveThemselfEmailToBusinessEntity = "removeThemselfEmailToBE"
  private val RemoveThemselfEmailToLead = "removeThemselfEmailToLead"

  def getAddBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { undertaking =>
      implicit val eori: EORI = request.eoriNumber

      val result = for {
        journey <- store.get[BusinessEntityJourney].toContext
        _ <- store.put[Undertaking](undertaking).toContext
      } yield journey

      result.fold(handleMissingSessionData("Business Entity Journey")) { journey =>
        val form = journey.addBusiness.value
          .fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))

        Ok(
          addBusinessPage(
            form,
            undertaking.name,
            undertaking.undertakingBusinessEntity
          )
        )
      }
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswer(form: FormValues) = {
      val enteredValue = form.value
      if (enteredValue === "true")
        store.update[BusinessEntityJourney](updateAddBusiness(form)).flatMap(_.next)
      else Redirect(routes.AccountController.getAccountPage()).toFuture
    }

    withLeadUndertaking { undertaking =>
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors =>
            BadRequest(addBusinessPage(errors, undertaking.name, undertaking.undertakingBusinessEntity)).toFuture,
          handleValidAnswer
        )
    }
  }

  def getEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    withLeadUndertaking { _ =>
      implicit val eori: EORI = request.eoriNumber
      journeyTraverseService.getPrevious[BusinessEntityJourney].flatMap { previous =>
        store.get[BusinessEntityJourney].flatMap {
          case Some(journey) =>
            val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori)))
            Ok(eoriPage(form, previous)).toFuture
          case _ => handleMissingSessionData("Business Entity Journey")
        }
      }
    }
  }

  def postEori: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val businessEntityEori = "businessEntityEori"

    def getErrorResponse(errorMessageKey: String, previous: String, form: FormValues): Future[Result] =
      BadRequest(eoriPage(eoriForm.withError(businessEntityEori, errorMessageKey).fill(form), previous)).toFuture

    def handleValidEori(form: FormValues, previous: Uri): Future[Result] =
      escService.retrieveUndertakingWithErrorResponse(EORI(form.value)).flatMap {

        case Right(Some(_)) => getErrorResponse("businessEntityEori.eoriInUse", previous, form)
        case Left(_) => getErrorResponse(s"error.$businessEntityEori.required", previous, form)
        case Right(None) => store.update[BusinessEntityJourney](updateEori(form)).flatMap(_.next)
      }

    withLeadUndertaking { _ =>
      journeyTraverseService.getPrevious[BusinessEntityJourney].flatMap { previous =>
        eoriForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(eoriPage(errors, previous)).toFuture,
            form => handleValidEori(form, previous)
          )
      }
    }
  }

  def getCheckYourAnswers: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    withLeadUndertaking { _ =>
      store.get[BusinessEntityJourney].flatMap {
        case Some(journey) =>
          val eori = journey.eori.value.getOrElse(handleMissingSessionData("EORI"))
          Ok(businessEntityCyaPage(eori, journey.previous)).toFuture
        case _ => handleMissingSessionData("CheckYourAnswers journey")
      }
    }
  }

  def postCheckYourAnswers: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswers(undertaking: Undertaking) = for {
      businessEntityJourney <- store
        .get[BusinessEntityJourney]
        .map(_.getOrElse(handleMissingSessionData("BusinessEntity Journey")))
      undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("undertaking ref"))
      eoriBE = businessEntityJourney.eori.value.getOrElse(handleMissingSessionData("BE EORI"))
      businessEntity = BusinessEntity(eoriBE, leadEORI = false) // resetting the journey as it's final CYA page
      _ <- {
        if (businessEntityJourney.isAmend) {
          escService.removeMember(
            undertakingRef,
            businessEntity.copy(businessEntityIdentifier = businessEntityJourney.oldEORI.get)
          )
        }
        escService.addMember(undertakingRef, businessEntity)
      }
      _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
        eoriBE,
        None,
        AddMemberEmailToBusinessEntity,
        undertaking,
        undertakingRef,
        None
      )
      _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
        eori,
        eoriBE.some,
        AddMemberEmailToLead,
        undertaking,
        undertakingRef,
        None
      )
      // Clear the cached undertaking so it's retrieved on the next access
      _ <- store.delete[Undertaking]
      _ =
        if (businessEntityJourney.isAmend)
          auditService.sendEvent(AuditEvent.BusinessEntityUpdated(undertakingRef, request.authorityId, eori, eoriBE))
        else auditService.sendEvent(AuditEvent.BusinessEntityAdded(undertakingRef, request.authorityId, eori, eoriBE))
      redirect <- getNext(businessEntityJourney)(eori)
    } yield redirect

    withLeadUndertaking { undertaking =>
      cyaForm
        .bindFromRequest()
        .fold(
          errors => throw new IllegalStateException(s"value hard-coded, form hacking? $errors"),
          _ => handleValidAnswers(undertaking)
          // TODO try to get an undertaking for the eori of the added business, and only proceed if there isn't one
          // TODO UX are figuring out the correct behaviour here so will come back to this
        )
    }
  }

  def editBusinessEntity(eoriEntered: String): Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[BusinessEntityJourney].flatMap { previous =>
      withLeadUndertaking { undertaking =>
        store
          .put(BusinessEntityJourney.businessEntityJourneyForEori(undertaking.some, EORI(eoriEntered)))
          .map(_ => Ok(businessEntityCyaPage(eoriEntered, previous)))
      }
    }

  }

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = withAuthenticatedUser.async {
    implicit request =>
      withLeadUndertaking { _ =>
        escService.retrieveUndertaking(EORI(eoriEntered)).map {
          case Some(undertaking) =>
            val removeBE = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))
            Ok(removeBusinessPage(removeBusinessForm, removeBE))
          case _ => handleMissingSessionData("Undertaking journey")
        }
      }
  }

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    for {
      undertakingOpt <- escService.retrieveUndertaking(eori)
    } yield undertakingOpt match {
      case Some(undertaking) =>
        val removeBE = undertaking.getBusinessEntityByEORI(eori)
        Ok(removeYourselfBEPage(removeYourselfBusinessForm, removeBE, previous, undertaking.name))

      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = withAuthenticatedUser.async {
    implicit request =>
      implicit val eori: EORI = request.eoriNumber
      withLeadUndertaking { _ =>
        escService.retrieveUndertaking(EORI(eoriEntered)).flatMap {
          case Some(undertaking) =>
            val undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("undertaking reference"))
            val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))

            removeBusinessForm
              .bindFromRequest()
              .fold(
                errors => Future.successful(BadRequest(removeBusinessPage(errors, removeBE))),
                success = form => {
                  form.value match {
                    case "true" =>
                      val removalEffectiveDateString = DateFormatter.govDisplayFormat(timeProvider.today)
                      for {
                        _ <- escService.removeMember(undertakingRef, removeBE)
                        _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
                          EORI(eoriEntered),
                          None,
                          RemoveMemberEmailToBusinessEntity,
                          undertaking,
                          undertakingRef,
                          removalEffectiveDateString.some
                        )
                        _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
                          eori,
                          EORI(eoriEntered).some,
                          RemoveMemberEmailToLead,
                          undertaking,
                          undertakingRef,
                          removalEffectiveDateString.some
                        )
                        // Clear the cached undertaking so it's retrieved on the next access
                        _ <- store.delete[Undertaking]
                        _ = auditService
                          .sendEvent(
                            AuditEvent
                              .BusinessEntityRemoved(undertakingRef, request.authorityId, eori, EORI(eoriEntered))
                          )
                      } yield Redirect(routes.BusinessEntityController.getAddBusinessEntity())
                    case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
                  }
                }
              )
          case _ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()).toFuture
        }
      }
  }

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = withAuthenticatedUser.async { implicit request =>
    val loggedInEORI = request.eoriNumber
    val previous = routes.AccountController.getAccountPage().url
    escService.retrieveUndertaking(loggedInEORI).flatMap {
      case Some(undertaking) =>
        val undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("undertaking reference"))
        val removeBE: BusinessEntity = undertaking.getBusinessEntityByEORI(loggedInEORI)
        removeYourselfBusinessForm
          .bindFromRequest()
          .fold(
            errors => Future.successful(BadRequest(removeYourselfBEPage(errors, removeBE, previous, undertaking.name))),
            form =>
              form.value match {
                case "true" =>
                  val removalEffectiveDateString = DateFormatter.govDisplayFormat(timeProvider.today)
                  val leadEORI = undertaking.getLeadEORI
                  for {
                    _ <- escService.removeMember(undertakingRef, removeBE)
                    _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
                      loggedInEORI,
                      None,
                      RemoveThemselfEmailToBusinessEntity,
                      undertaking,
                      undertakingRef,
                      removalEffectiveDateString.some
                    )
                    _ <- sendEmailHelperService.retrieveEmailAddressAndSendEmail(
                      leadEORI,
                      loggedInEORI.some,
                      RemoveThemselfEmailToLead,
                      undertaking,
                      undertakingRef,
                      removalEffectiveDateString.some
                    )
                    _ = auditService
                      .sendEvent(
                        AuditEvent
                          .BusinessEntityRemovedSelf(undertakingRef, request.authorityId, leadEORI, loggedInEORI)
                      )
                  } yield Redirect(routes.SignOutController.signOut())
                case _ => Future(Redirect(routes.AccountController.getAccountPage()))
              }
          )

      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  private def getNext(businessEntityJourney: BusinessEntityJourney)(implicit EORI: EORI): Future[Result] =
    businessEntityJourney.isLeadSelectJourney match {
      case Some(true) =>
        store
          .put[BusinessEntityJourney](BusinessEntityJourney(isLeadSelectJourney = true.some))
          .map(_ => Redirect(routes.SelectNewLeadController.getSelectNewLead()))
      case _ =>
        store
          .put[BusinessEntityJourney](BusinessEntityJourney())
          .map(_ => Redirect(routes.BusinessEntityController.getAddBusinessEntity()))
    }

  private def updateBusinessEntityJourney(beOpt: Option[BusinessEntityJourney])(
    f: BusinessEntityJourney => BusinessEntityJourney
  ) = beOpt.map(f)

  def updateEori(f: FormValues)(beOpt: Option[BusinessEntityJourney]) = updateBusinessEntityJourney(beOpt) {
    beJourney =>
      beJourney.copy(eori = beJourney.eori.copy(value = EORI(f.value).some), oldEORI = beJourney.eori.value)
  }

  def updateAddBusiness(f: FormValues)(beOpt: Option[BusinessEntityJourney]) =
    updateBusinessEntityJourney(beOpt) { beJourney =>
      beJourney.copy(addBusiness = beJourney.addBusiness.copy(value = f.value.toBoolean.some))
    }

  lazy val addBusinessForm: Form[FormValues] = Form(
    mapping("addBusiness" -> mandatory("addBusiness"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val removeBusinessForm: Form[FormValues] = Form(
    mapping("removeBusiness" -> mandatory("removeBusiness"))(FormValues.apply)(FormValues.unapply)
  )

  lazy val removeYourselfBusinessForm: Form[FormValues] = Form(
    mapping("removeYourselfBusinessEntity" -> mandatory("removeYourselfBusinessEntity"))(FormValues.apply)(
      FormValues.unapply
    )
  )

  lazy val eoriForm: Form[FormValues] = Form(
    mapping("businessEntityEori" -> mandatory("businessEntityEori"))(eoriEntered =>
      FormValues(s"$eoriPrefix$eoriEntered")
    )(eori => eori.value.drop(2).some)
      .verifying(
        "businessEntityEori.error.incorrect-length",
        eori => eori.value.length == 14 || eori.value.length == 17
      )
      .verifying("businessEntityEori.regex.error", eori => eori.value.matches(EORI.regex))
  )

  lazy val cyaForm: Form[FormValues] = Form(mapping("cya" -> mandatory("cya"))(FormValues.apply)(FormValues.unapply))

}
