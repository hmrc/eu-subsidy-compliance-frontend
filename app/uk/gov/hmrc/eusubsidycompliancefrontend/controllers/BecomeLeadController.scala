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

import cats.implicits.catsSyntaxOptionId
import play.api.data.Form
import play.api.data.Forms.{email, mapping}
import play.api.mvc._
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.ActionBuilders
import uk.gov.hmrc.eusubsidycompliancefrontend.actions.requests.AuthenticatedEnrolledRequest
import uk.gov.hmrc.eusubsidycompliancefrontend.config.AppConfig
import uk.gov.hmrc.eusubsidycompliancefrontend.models._
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent
import uk.gov.hmrc.eusubsidycompliancefrontend.models.audit.AuditEvent.BusinessEntityPromotedSelf
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.EmailTemplate.{PromotedSelfToNewLead, RemovedAsLeadToFormerLead}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.email.{EmailType, RetrieveEmailResponse}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.types.EORI
import uk.gov.hmrc.eusubsidycompliancefrontend.services._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.FutureSyntax.FutureOps
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.OptionTSyntax._
import uk.gov.hmrc.eusubsidycompliancefrontend.syntax.StringSyntax.StringOps
import uk.gov.hmrc.eusubsidycompliancefrontend.views.html._
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BecomeLeadController @Inject() (
  mcc: MessagesControllerComponents,
  actionBuilders: ActionBuilders,
  store: Store,
  escService: EscService,
  emailService: EmailService,
  emailVerificationService: EmailVerificationService,
  auditService: AuditService,
  becomeAdminPage: BecomeAdminPage,
  becomeAdminAcceptResponsibilitiesPage: BecomeAdminAcceptResponsibilitiesPage,
  becomeAdminConfirmationPage: BecomeAdminConfirmationPage,
  confirmEmailPage: ConfirmEmailPage,
  inputEmailPage: InputEmailPage,

)(implicit
  val appConfig: AppConfig,
  executionContext: ExecutionContext
) extends BaseController(mcc) {

  import actionBuilders._

  private val becomeAdminForm = formWithSingleMandatoryField("becomeAdmin")

  private val optionalEmailForm = Form(
    mapping(
      "using-stored-email" -> mandatory("using-stored-email"),
      "email" -> mandatoryIfEqual("using-stored-email", "false", email)
    )(OptionalEmailFormInput.apply)(OptionalEmailFormInput.unapply)
  )

  private val emailForm = Form(
    mapping(
      "email" -> email
    )(FormValues.apply)(FormValues.unapply)
  )

  // TODO - refactor the code here and consider using getUndertaking to remove some of the boilerplate
  def getAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    println(s"getAcceptResponsibilities: called for eori $eori")

    def handleRequest(
      becomeLeadJourneyOpt: Option[BecomeLeadJourney],
      undertakingOpt: Option[Undertaking]
    )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] = {
      (becomeLeadJourneyOpt, undertakingOpt) match {
        case (Some(journey), Some(_)) =>
          Ok(becomeAdminAcceptResponsibilitiesPage(eori)).toFuture
        case (None, Some(_)) => // initialise the empty Journey model
          store.put(BecomeLeadJourney()).map { _ =>
            Ok(becomeAdminAcceptResponsibilitiesPage(eori))
          }
        case _ =>
          // TODO - we should never be able to get there since there should always be an undertaking. :/
          throw new IllegalStateException("missing undertaking")
      }
    }

    // TODO - review the logic here
    for {
      journey <- store.get[BecomeLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      result <- handleRequest(journey, undertakingOpt)
    } yield result

  }

  def postAcceptResponsibilities: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    store
      .update[BecomeLeadJourney](j => j.copy(acceptResponsibilities = j.acceptResponsibilities.copy(value = Some(true))))
      .flatMap(_ => Future(Redirect(routes.BecomeLeadController.getConfirmEmail())))
  }

  def getConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    val previous = routes.BecomeLeadController.getAcceptResponsibilities().url

    store.get[BecomeLeadJourney].flatMap { _ =>
      emailVerificationService.getEmailVerification(request.eoriNumber).flatMap {
        case Some(verifiedEmail) =>
          Ok(confirmEmailPage(optionalEmailForm, routes.BecomeLeadController.postConfirmEmail(), EmailAddress(verifiedEmail.email), previous)).toFuture
        case None => emailService.retrieveEmailByEORI(request.eoriNumber) map { response =>
          response.emailType match {
            // TODO - is the get on emailAddress safe?
            case EmailType.VerifiedEmail => Ok(confirmEmailPage(optionalEmailForm, routes.BecomeLeadController.postConfirmEmail(), response.emailAddress.get, previous))
            case _ => Ok(inputEmailPage(emailForm, previous))
          }
        }
      }
    }
  }

  // TODO - look at EmailVerificationService hard coded redirects and revise to take parameters
  def postConfirmEmail: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber
    val verifiedEmail = for {
      stored <- emailVerificationService.getEmailVerification(request.eoriNumber)
      cds <- emailService.retrieveEmailByEORI(request.eoriNumber)
      result = if (stored.isDefined) stored.get.email.some else cds match {
        case RetrieveEmailResponse(EmailType.VerifiedEmail, Some(value)) => value.value.some
        case _ => Option.empty
      }
    } yield result

    val previous = routes.BecomeLeadController.getConfirmEmail().url
    val next = routes.BecomeLeadController.getBecomeLeadEori().url

    verifiedEmail flatMap {
      case Some(email) =>
        optionalEmailForm
          .bindFromRequest()
          .fold(
            errors => BadRequest(confirmEmailPage(errors, routes.BecomeLeadController.postConfirmEmail(), EmailAddress(email), previous)).toFuture,
            {
              // TODO - clarify the meaning of these cases
              case OptionalEmailFormInput("true", None) =>
                for {
                  _ <- emailVerificationService.addVerificationRequest(request.eoriNumber, email)
                  _ <- emailVerificationService.verifyEori(request.eoriNumber)
                  _ <- store.update[UndertakingJourney](_.setVerifiedEmail(email))
                } yield Redirect(next)
              case OptionalEmailFormInput("false", Some(email)) => {
                for {
                  verificationId <- emailVerificationService.addVerificationRequest(request.eoriNumber, email)
                  verifyEmailUrl = routes.BecomeLeadController.getVerifyEmail(verificationId).url
                  verificationResponse <- emailVerificationService.verifyEmail(verifyEmailUrl, previous)(request.authorityId, email)
                } yield emailVerificationService.emailVerificationRedirect(routes.BecomeLeadController.getConfirmEmail())(verificationResponse)
              }
              // TODO - does this default case make any sense
              case _ => Redirect(routes.EligibilityController.getNotEligible()).toFuture
            }
          )
      case _ => emailForm.bindFromRequest().fold(
        errors => BadRequest(inputEmailPage(errors, routes.EligibilityController.getDoYouClaim().url)).toFuture,
        form => {
          // TODO - seems to be a duplication of some of the code above - refactor
          for {
            verificationId <- emailVerificationService.addVerificationRequest(request.eoriNumber, form.value)
            verifyEmailUrl = routes.BecomeLeadController.getVerifyEmail(verificationId).url
            verificationResponse <- emailVerificationService.verifyEmail(verifyEmailUrl, previous)(request.authorityId, form.value)
          } yield emailVerificationService.emailVerificationRedirect(routes.BecomeLeadController.getConfirmEmail())(verificationResponse)
        }
      )
    }
  }

  // TODO - review this code
  def getVerifyEmail(verificationId: String): Action[AnyContent] = enrolled.async { implicit request =>
    println(s"BecomeLeadController - getVerifyEmail called")
    implicit val eori: EORI = request.eoriNumber
    store.get[BecomeLeadJourney].flatMap {
      case Some(journey) =>
        for {
          e <- emailVerificationService.approveVerificationRequest(request.eoriNumber, verificationId)
          wasSuccessful = e.getMatchedCount > 0
          redirect <- if (wasSuccessful) {
            for {
              stored <- emailVerificationService.getEmailVerification(request.eoriNumber)
              _ <- store.update[UndertakingJourney](_.setVerifiedEmail(stored.get.email))
              redirect <-
                if (wasSuccessful) {
                  println(s"Verification was successful - moving on to next step in journey")
                  journey.next
                  Redirect(routes.BecomeLeadController.getBecomeLeadEori().url).toFuture
                }
                else Redirect(routes.BecomeLeadController.getConfirmEmail().url).toFuture
            } yield redirect
          } else Redirect(routes.BecomeLeadController.getBecomeLeadEori().url).toFuture
        } yield redirect
      case None => handleMissingSessionData("Become Lead Journey")
    }
  }

  def getBecomeLeadEori: Action[AnyContent] = verifiedEmail.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleRequest(
      becomeLeadJourneyOpt: Option[BecomeLeadJourney],
      undertakingOpt: Option[Undertaking]
    )(implicit request: AuthenticatedEnrolledRequest[_], eori: EORI): Future[Result] = {
      (becomeLeadJourneyOpt, undertakingOpt) match {
        case (Some(journey), Some(_)) =>

          val form = journey.becomeLeadEori.value.fold(becomeAdminForm)(e => becomeAdminForm.fill(FormValues(e.toString)))
          Ok(becomeAdminPage(form)).toFuture

        case (None, Some(_)) => // initialise the empty Journey model
          store.put(BecomeLeadJourney()).map { _ =>
            Ok(becomeAdminPage(becomeAdminForm))

          }

        case _ => throw new IllegalStateException("missing undertaking name")
      }
    }

    for {
      journey <- store.get[BecomeLeadJourney]
      undertakingOpt <- escService.retrieveUndertaking(eori)
      result <- handleRequest(journey, undertakingOpt)
    } yield result
  }


  def postBecomeLeadEori: Action[AnyContent] = enrolled.async { implicit request =>
    implicit val eori: EORI = request.eoriNumber

    def handleFormSubmission(form: FormValues) =
      if (form.value.isTrue) promoteBusinessEntity()
      else Redirect(routes.AccountController.getAccountPage()).toFuture

    def promoteBusinessEntity() =
      store.get[BecomeLeadJourney].toContext
        .foldF(Redirect(routes.BecomeLeadController.getBecomeLeadEori()).toFuture) { journey =>
          val result = for {
            undertaking <- escService.getUndertaking(eori).toContext
            ref = undertaking.reference
            newLead = undertaking.getBusinessEntityByEORI(eori).copy(leadEORI = true)
            formerLead = undertaking.getLeadBusinessEntity.copy(leadEORI = false)
            // Promote new lead
            _ <- escService.addMember(ref, newLead).toContext
            _ <- emailService.sendEmail(eori, PromotedSelfToNewLead, undertaking).toContext
            // Demote former lead
            _ <- escService.addMember(ref, formerLead).toContext
            _ <- emailService.sendEmail(formerLead.businessEntityIdentifier, RemovedAsLeadToFormerLead, undertaking).toContext
            // Flush any stale journey state
            _ <- store.delete[UndertakingJourney].toContext
            _ <- store.delete[BusinessEntityJourney].toContext
            // Send audit event
            _ = auditService.sendEvent[BusinessEntityPromotedSelf](
              AuditEvent.BusinessEntityPromotedSelf(
                ref,
                request.authorityId,
                formerLead.businessEntityIdentifier,
                newLead.businessEntityIdentifier
              )
            )
          } yield Redirect(routes.BecomeLeadController.getPromotionConfirmation())

          result.getOrElse(throw new IllegalStateException("Unexpected error promoting business entity to lead"))
        }

    becomeAdminForm
      .bindFromRequest()
      .fold(
        formWithErrors => BadRequest(becomeAdminPage(formWithErrors)).toFuture,
        handleFormSubmission
      )
  }


  def getPromotionConfirmation: Action[AnyContent] = verifiedEmail.async { implicit request =>
    Ok(becomeAdminConfirmationPage()).toFuture
  }

}
