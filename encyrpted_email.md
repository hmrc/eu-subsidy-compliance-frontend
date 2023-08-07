# Current MongoDB email design

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


