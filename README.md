
# eu-subsidy-compliance-frontend

This microservice serves the public digital UI to allow the companies to claim a custom subsidy on the goods coming from
GB or rest of the world into Northern Ireland that are at risk of moving into EU(via Ireland). It allows companies to register 
to the service and help claimants:
- record the non-customs subsidies they have claimed and received
- view, via a live financial dashboard, the customs and non-customs subsidies they have received
- ensure that the company (or 'undertaking') has not exceeded the cap for its respective sector

## Persistence

This service uses `mongodb` to persist user answers about Undertaking, Business Entity and Subsidies.
## Requirements

This service is written in [Scala](http://www.scala-lang.org/) using the
[Play framework](http://playframework.com/), so needs at least a JRE to run.

JRE/JDK 1.8 is recommended.

The service also depends on `mongodb`

## Running the service

All dependant services can be run via
```
sm2 --start ESC_ALL
```


To stop the microservice from running on service manager (e.g. to run your own version locally), you can run:

```
sm2 -stop ESC_FRONTEND 
```

### Starting a journey
A journey can be started by hitting the service root e.g.
```
GET /report-and-manage-your-allowance-for-customs-duty-waiver-claims
```
This will prompt the user to log in if there isn't an active session yet.


### Using localhost

To run this microservice locally on the configured port **'9093'**, you can run:

```
sbt run 
```

**NOTE:** Ensure that you are not running the microservice via service manager before starting your service locally (vice versa) or the service will fail to start


### Accessing the service

Access details can be found on
[DDCY Live Services Credentials sheet](https://docs.google.com/spreadsheets/d/1ecLTROmzZtv97jxM-5LgoujinGxmDoAuZauu2tFoAVU/edit?gid=1186990023#gid=1186990023)
for both staging and local url's or check the Tech Overview section in the
[service summary page ](https://confluence.tools.tax.service.gov.uk/display/ELSY/EUSC+Service+Summary)

## Changing the Sector Cap

To update industry sector caps, modify the configuration in the **application.conf** file. This centralizes the information, eliminating the need for a new release each time an update is required. Find the sector cap configuration as follows:
```
sectorCap {
  agriculture = ""
  aquaculture = ""
  transport = ""
  other = ""
}
```
Ensure consistency by updating industry sector caps in [eu-subsidy-compliance-stub](https://github.com/hmrc/eu-subsidy-compliance-stub) with the latest values.

### Updating the Sector Cap in other environments

Modify the app config file in each environment by accessing the relevant GitHub repository. For example, to amend QA's app config, visit: [app-config-qa](https://github.com/hmrc/app-config-qa).

After making and merging changes, restart the application in that environment to apply the updates.

## Feature flags

### euro-only-enabled
The standard subsidy journey allows users to submit payments in either pounds or euros. When the user chooses pounds, the ESC service displays the euro equivalent of the amount entered in pounds. To do this, the ESC service uses the Europa API (a public API) to get the latest exchange rates. We have no control over Europa API's service availability, so if/when the service is down for a considerable amount of time, we have a 'euro only' journey, allowing users (most users submit euros) to still be able to submit subsidies.
Set this value to true to enable the euro only journey.

## Testing the service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to
provide test coverage reports.

Use the following command to run the tests with coverage and generate a report.

```
sbt clean coverage test it:test coverageReport
```

The following command can also be run to format the code base and then execute all tests:

```
sbt precommit
```

## Formatting
This is governed by the **.scalafmt.conf** file.

## Monitoring

The following grafana and kibana dashboards are available for this service:
* [grafana](https://grafana.tools.production.tax.service.gov.uk/d/RwwxDLSnz/eu-subsidy-compliance-frontend)
* [kibana](https://kibana.tools.production.tax.service.gov.uk/app/kibana#/dashboard/eu-subsidy-compliance-frontend)

## Runbook

The runbook for this service can be found
[here](https://confluence.tools.tax.service.gov.uk/display/SC/Runbook+-+Subsidy+Compliance).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
