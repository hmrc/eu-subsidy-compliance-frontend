# Email cache journey

## Sending an email

We attempt to get the email address from the memcache. This looks for only verified emails. If one is not found, we call
CDS via GET
```s
/customs-data-store/eori/$eori/verified-email
```

And use that. This does bring up the point that we can have mismatches if there is an issue with the cache update mechanism.

## Cached entry usages

### Entrance points in the controllers

We use the cached entry to verify we have a verified email for the user on the entrance of the majority of 
controller routes. We do not use the value, just raise a "No verified email address found" fatal error 
if one is not found. CDS probably would not be very happy with us unless we cached the entry due to the load we would
cause on them.


### Email value usage (why we break GPDR)

#### EmailVerificationSupport.findVerifiedEmail 
This attempts to get it from the cache, if not there, it self-corrects later adding to cache in **handleConfirmEmailPost**
if the user says they want to use the stored one. It self-corrects always, even if it is in the cache.

```scala
private def findVerifiedEmail(implicit eori: EORI, hc: HeaderCarrier): Future[Option[String]] =
  emailVerificationService
          .getCachedEmailVerification(eori)
          .toContext
          .foldF(default = retrieveCdsEmail)(_.email.some.toFuture)

```

This is a trait bound to 

* **BecomeLeadController**
* **UndertakingController**

##### Show confirm email page

And use to show email confirmation page. Possibly just use CDS direct as this page is not heavy use?

* BecomeLeadController.getConfirmEmail
* UndertakingController.getConfirmEmail

Both call

```scala
  protected def handleConfirmEmailGet[A : ClassTag](
    previous: Call,
    formAction: Call
  )(implicit
    request: AuthenticatedEnrolledRequest[AnyContent],
    messages: Messages,
    appConfig: AppConfig,
    format: Format[A]
  ): Future[Result] = {

    implicit val eori: EORI = request.eoriNumber

    withJourneyOrRedirect[A](previous) { _ =>
      findVerifiedEmail.toContext
        .fold(default = Ok(inputEmailPage(emailForm, previous.url))) { e =>
          Ok(confirmEmailPage(optionalEmailForm, formAction, EmailAddress(e), previous.url))
        }
    }
  }

```

And DO not do a have a verified email check. Just an enrollment check.


##### handleConfirmEmailPost
<https://github.com/hmrc/eu-subsidy-compliance-frontend/blob/64601fba701d8da267e26604b4f1864ca4bfc719/app/uk/gov/hmrc/eusubsidycompliancefrontend/controllers/EmailVerificationSupport.scala#L99>

**If we say we are using the stored email address, then the following happens: -**
* Set the verification in our cache in a verified state. This actually course corrects if we don't have it in 
  the cache and as it gets it from CDS. Stored can mean CDS or our Mongo.
* Add the email address to the undertaking journey, spoke to Gerald about this.
  The design uses the template Method pattern and for BecomeLeadController does nothing. It just
  has to fit the opinionated design of the inheritance
```scala
  override protected def addVerifiedEmailToJourney(email: String)(implicit eori: EORI): Future[Unit] =
      ().toFuture
```
* Add to our Mongo Cache for future use
```scala
if (form.usingStoredEmail.isTrue)
        for {
          _ <- emailVerificationService.addVerifiedEmailToCache(eori, email)
          _ <- addVerifiedEmailToJourney(email)
        } 
```
**If we say we want to not use the stored email and use something else for the EORI**
* We add the new email to our cache for the EORI, this will set it to an unverified state.
* We send a verification request to the email-verification service (potentially this leaves things broken 
  until email verification is complete)


### Sending email

We always attempt to use our cache when sending emails, if it is not there then we defer to CDS. This always 
relies on the EORI, so potentially we just use CDS all the time and not store the email. Just have an EORI and verified
entry in the cache, maybe a sha-256 email, so we can confirm if something is what we expect but is not reversible.

The stories
<https://jira.tools.tax.service.gov.uk/browse/ESC-623>
<https://jira.tools.tax.service.gov.uk/browse/ESC-647>

Deal with creating the email caching (code is linked to ticket) but just mention CDS and no mention of why
the email is stored. We cannot call CDS needlessly on most entrance points as we check for just whether the EORI
has a verified email, but showing the email.


### Where is the verified email value is actually used.

* Showing the email on the getAmendUndertakingDetails. This only uses the cache.
* Sending email with fallback to CDS
* Add the email to the Become Lead/Undertaking journey. Possibly just get from CDS?

CDS https://github.com/hmrc/customs-data-store has a cache as well, if not in cache it goes to SUB9


## Potential course of action

* We stop returning the email always from the cache. We can just have something that returns whether there 
  is a valid entry. This caters for 90%+ or calls.
* For places that require the actual email, we can do the reverse.
  1. Try CDS
  2. Defer to our cache if the CDS endpoint does not have the value. We should log a warning with the EORI if we do defer, 
     this will allow us to spot if there are any gaps in their data. If there are no gaps then we can trust CDS. We can
     only see warnings for live.