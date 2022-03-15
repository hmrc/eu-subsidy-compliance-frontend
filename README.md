
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

All dependant services can run via
```
sm --start ESC_ALL
```
By default, this service runs on port `9093`. To bring up only this service , use
```
sbt run
```

### Starting a journey
A journey can be started via the start endpoint
```
GET /eu-subsidy-complaince/
```
This will prompt the user to log in if there isn't an active session yet.

## Testing the service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to
provide test coverage reports.

Use the following command to run the tests with coverage and generate a report.

```
sbt clean coverage test it:test coverageReport
```

## Monitoring

The following grafana and kibana dashboards are availble for this service
* [grafana](https://grafana.tools.production.tax.service.gov.uk/d/RwwxDLSnz/eu-subsidy-compliance-frontend)
* [kibana](https://kibana.tools.production.tax.service.gov.uk/app/kibana#/dashboard/eu-subsidy-compliance-frontend)

## Runbook

The runbook for this service can be found
[here](https://confluence.tools.tax.service.gov.uk/display/SC/Runbook+-+Subsidy+Compliance).

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").