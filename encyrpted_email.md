# Current MongoDB email storage design

We use mongodb for things like journey state. We are referring to the email mongodb as a cache, but it is not really 
a cache as it can become divergent by design.

## Where do we initially get the emails from in an undertaking?

![create-undertaking-workflow.png](create-undertaking-workflow.png)

Our mongodb is likely missing the email address. In that case, we defer to CDS to get the email in the "create undertaking" journey.
We assume the email has been verified in the CDS cache. Though that cache could have an unverified email set as the update email
cache endpoint on their service could be used by another service without going through a verification process.

We always add the email to our MongoDb email store whether it exists or not so self repairs.

## CDS workflow
![cds.png](cds.png)

CDS defers to Sub09 for initial cache population, but after that the data stored potentially branches off.

## Sending email 
![sending-email.png](sending-email.png)

If we do not have it in our cache, we defer to CDS. We do not add that value to our cache.

## Do we need to store the email in our cache?

Unfortunately, if we want an email to be verified, then yes. We are assuming that CDS/Sub09 has emails that have been
verified but this may not be the case. The CDS update cache process does not update its original authoritative source, the
update endpoint also may be used by services that have not done verification (trust issues?).

In an ideal world, an EORI would have one email assigned to it, and we could just refer to that source. A verification
process would manage and guard that cache. Currently, application may have its own email value set per EORI.

## How long do we want to keep the entry in MongoDB?

In the current design, if we lose the cached value, everything breaks as has been demonstrated last week.
We only populate the cache in the "create undertaking" journey. On many journeys, we check for the existence 
of a verified email in our MongoDB email cache. If we do not have a cache entry, we currently blow up with no way to
rectify. We are changing the design to fire off the email verification process in those cases, so losing the cache entry
will no longer be catastrophic.

### With the improved self-rectifying verified email process, what would the issue/s be if we lost the cache entry? 

The main issue is user experience. The user would have to redo the verification. This comes down to the lack of authoritative  
source for a verified email tied to an EORI we can self repair from. Can we design a user experience where we only keep
it say yearly if we have the reverification mechanism?

## How consistent are we in checking the cache before checking CDS

We always check our MongoDB email storage. As our storage diverges from CDS it is not a cache. As mentioned, CDS can also
diverge.

## PII concerns? Do we need GDRP?

There are issues with the zone we are keeping the data in (it is in the frontend), and also it is not encrypted. Gerald Benischke
recommends we move it to the backend and encrypt it. 

## question for CDS - if we get a response from CDS - is it verified? can we raise this with CDS please

We could raise it, but as their endpoint is not forcefully tied to verification and relies on the consumers to verify before 
 updating, it becomes an issue of faith. Did Sub09 verify its value where CDS got its value from?

## If we encrypt the emails - impact to the performance?

Not noticeably, we would have to be under very high load to notice adding such low level of cpu overhead. 

## What and whose support is needed for encryption?

Encryption is done by the JSON serializer/deserializer which we use to create and read the cached entry. The data migration
task is where the fun will happen. 

##  Are we going to test the encryption in QA before Prod? How?

Of course, and Staging. We can get a copy of the mongodb for QA and Staging but not live. We need to get the dry run perfect
before doing the process in live. We can also keep both stores for a while and use the non encrypted to sanity check the encrypted.
After we have X period of no issues, we can remove the non-encrypted logic. We cannot debug production, so we want to guarantee
smoothness before turning off.

![email-lookup-encrypted-interim.png](email-lookup-encrypted-interim.png)

## can we add logs to the code to highlight how often we use our cache

The cache really is storage as it diverges. We should probably refrain from using the term cache as it indicates there
is an authoritative source we can rely on. As mentioned, we cannot see logs that are not warnings/errors in production.
We can log warnings where we do not hit our storage. Though in some cases, this is expected.

### Storage is missed when creating an undertaking 
This is expected on a first journey. The journey populates our storage. We also confirm with the user if that value is 
correct, so they can set another which fires off the verification process. The email in CDS being verified has some 
assumption tied to it.


### 
