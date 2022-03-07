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
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.{EORI, UndertakingName}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.{BusinessEntity, FormValues, Undertaking}
import uk.gov.hmrc.eusubsidycompliancefrontend.services.Journey.Uri
import uk.gov.hmrc.eusubsidycompliancefrontend.services.{AuditService, BusinessEntityJourney, EscService, JourneyTraverseService, SendEmailHelperService, Store}
import uk.gov.hmrc.eusubsidycompliancefrontend.util.TimeProvider
import uk.gov.hmrc.eusubsidycompliancefrontend.views.formatters.DateFormatter
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessEntityController @Inject() (
  mcc: MessagesControllerComponents,
  escActionBuilders: EscActionBuilders,
  store: Store,
  escService: EscService,
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
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import escActionBuilders._
  val eoriPrefix = "GB"
  val AddMemberEmailToBusinessEntity = "addMemberEmailToBE"
  val AddMemberEmailToLead = "addMemberEmailToLead"
  val RemoveMemberEmailToBusinessEntity = "removeMemberEmailToBE"
  val RemoveMemberEmailToLead = "removeMemberEmailToLead"
  val RemoveThemselfEmailToBusinessEntity = "removeThemselfEmailToBE"
  val RemoveThemselfEmailToLead = "removeThemselfEmailToLead"

  def getAddBusinessEntity: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    for {
      businessEntityJourneyOpt <- store.get[BusinessEntityJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      _ <- store.put[Undertaking](
        undertakingOpt.getOrElse(throw new IllegalStateException("missing undertaking on hod"))
      )
    } yield (businessEntityJourneyOpt, undertakingOpt) match {
      case (Some(journey), Some(undertaking)) =>
        val form =
          journey.addBusiness.value.fold(addBusinessForm)(bool => addBusinessForm.fill(FormValues(bool.toString)))
        Ok(
          addBusinessPage(
            form,
            undertaking.name,
            undertaking.undertakingBusinessEntity
          )
        )
      case _ => handleMissingSessionData("Business Entity Journey and Undertaking")
    }
  }

  def postAddBusinessEntity: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswer(form: FormValues) = {
      val enteredValue = form.value
      if (enteredValue === "true") {
        store
          .update[BusinessEntityJourney] { businessEntityOpt =>
            businessEntityOpt.map(be => be.copy(addBusiness = be.addBusiness.copy(value = Some(form.value.toBoolean))))
          }
          .flatMap(_.next)
      } else {
        Future.successful(Redirect(routes.AccountController.getAccountPage()))
      }

    }
    store.get[Undertaking].flatMap { undertaking =>
      val name: UndertakingName =
        undertaking.map(_.name).getOrElse(throw new IllegalStateException("missing undertaking name"))
      val businessEntities: List[BusinessEntity] = undertaking.map(_.undertakingBusinessEntity).getOrElse(List.empty)
      addBusinessForm
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(addBusinessPage(errors, name, businessEntities))),
          form => handleValidAnswer(form)
        )

    }
  }

  def getEori: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    journeyTraverseService.getPrevious[BusinessEntityJourney].flatMap { previous =>
      store.get[BusinessEntityJourney].flatMap {
        case Some(journey) =>
          val form = journey.eori.value.fold(eoriForm)(eori => eoriForm.fill(FormValues(eori.toString)))
          Future.successful(Ok(eoriPage(form, previous)))
        case _ => handleMissingSessionData("Business Entity Journey")
      }
    }
  }

  def postEori: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidEori(form: FormValues, previous: Uri) =
      for {
        retrievedUndertaking <- escService.retrieveUndertaking(EORI(form.value))
      } yield retrievedUndertaking match {
        case Some(_) =>
          Future(
            BadRequest(
              eoriPage(eoriForm.withError("businessEntityEori", "businessEntityEori.eoriInUse").fill(form), previous)
            )
          )
        case _ =>
          store
            .update[BusinessEntityJourney] { businessEntityOpt =>
              businessEntityOpt.map { businessEntity =>
                businessEntity.copy(
                  eori = businessEntity.eori.copy(value = Some(EORI(form.value))),
                  oldEORI = businessEntity.eori.value
                )
              }
            }
            .flatMap(_.next)
      }
    journeyTraverseService.getPrevious[BusinessEntityJourney].flatMap { previous =>
      eoriForm
        .bindFromRequest()
        .fold(
          errors => Future.successful(BadRequest(eoriPage(errors, previous))),
          form => handleValidEori(form, previous).flatten
        )
    }
  }

  def getCheckYourAnswers: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store.get[BusinessEntityJourney].flatMap {
      case Some(journey) =>
        val eori = journey.eori.value.getOrElse(handleMissingSessionData("EORI"))
        Future.successful(Ok(businessEntityCyaPage(eori)))

      case _ => handleMissingSessionData("CheckYourAnswers journey")
    }
  }

  def postCheckYourAnswers: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleValidAnswers() = for {
      undertaking <- store.get[Undertaking].map(_.getOrElse(handleMissingSessionData("undertaking ")))
      undertakingRef = undertaking.reference.getOrElse(handleMissingSessionData("undertaking ref"))
      businessEntityJourney <- store
        .get[BusinessEntityJourney]
        .map(_.getOrElse(handleMissingSessionData("BusinessEntity Journey")))
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
      _ =
        if (businessEntityJourney.isAmend)
          auditService.sendEvent(AuditEvent.BusinessEntityUpdated(request.authorityId, eori, eoriBE))
        else auditService.sendEvent(AuditEvent.BusinessEntityAdded(request.authorityId, eori, eoriBE))
      redirect <- getNext(businessEntityJourney)(eori)
    } yield redirect

    cyaForm
      .bindFromRequest()
      .fold(
        errors => throw new IllegalStateException(s"value hard-coded, form hacking? $errors"),
        _ => handleValidAnswers()
//        // TODO try to get an undertaking for the eori of the added business, and only proceed if there isn't one
//        // TODO UX are figuring out the correct behaviour here so will come back to this
//        for {
//          retrievedUndertaking <- connector.retrieveUndertaking(EORI("GB123456789016"))
//          b <- store.get[BusinessEntityJourney]
//          journey = b.fold(throw new IllegalStateException("journey should be defined")) {
//            identity
//          }
//          cd = Some(
//            ContactDetails(
//              journey.contact.value.getOrElse(throw new IllegalStateException("contact should be defined")).phone,
//              journey.contact.value.getOrElse(throw new IllegalStateException("contact should be defined")).mobile
//            )
//          )
//        } yield {
//          retrievedUndertaking match {
//            case Some(_) => {
//              ??? //Future(BadRequest(businessEntityCyaPage(cyaForm.withError("businessEntityEori", "businessEntityEori.eoriInUse").fill(form))))
//            }
//          }
//        }
      )
  }

  def editBusinessEntity(eoriEntered: String): Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori111: EORI = request.eoriNumber

    for {
      undertakingOpt <- escService.retrieveUndertaking(eori111)
      _ <- store.put(BusinessEntityJourney.businessEntityJourneyForEori(undertakingOpt, EORI(eoriEntered)))
    } yield Ok(businessEntityCyaPage(eoriEntered))
  }

  def getRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    for {
      undertakingOpt <- escService.retrieveUndertaking(EORI(eoriEntered))
    } yield undertakingOpt match {
      case Some(undertaking) =>
        val removeBE = undertaking.getBusinessEntityByEORI(EORI(eoriEntered))
        Ok(removeBusinessPage(removeBusinessForm, removeBE))

      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def getRemoveYourselfBusinessEntity: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori = request.eoriNumber
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

  def postRemoveBusinessEntity(eoriEntered: String): Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
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
                    _ = auditService
                      .sendEvent(AuditEvent.BusinessEntityRemoved(request.authorityId, eori, EORI(eoriEntered)))
                  } yield Redirect(routes.BusinessEntityController.getAddBusinessEntity())
                case _ => Future(Redirect(routes.BusinessEntityController.getAddBusinessEntity()))
              }
            }
          )

      case _ => handleMissingSessionData("Undertaking journey")
    }
  }

  def postRemoveYourselfBusinessEntity: Action[AnyContent] = authenticatedLeadUser.async { implicit request =>
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
                      .sendEvent(AuditEvent.BusinessEntityRemovedSelf(request.authorityId, leadEORI, loggedInEORI))
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
