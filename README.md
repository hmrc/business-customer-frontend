
business-customer-frontend
==========================

[![Build Status](https://travis-ci.org/hmrc/business-customer-frontend.svg?branch=master)](https://travis-ci.org/hmrc/business-customer-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/business-customer-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/business-customer-frontend/_latestVersion)

This frontend microservice is used to register a UK/Non-UK client (SA/Organisation) or agent to ETMP (creating business partner record) and enrol in Government Gateway. This renders common set of pages with texts based on the service using it. 

The services which uses business-customer-frontend are:
 * ATED - Annual Tax on Enveloped Dwellings
 * AWRS - The Alcohol Wholesaler Registration Scheme
 * AMLS - Anti Money Laundering Supervision
 

Usage
-----

Add your service name in the list inside application.conf 

```text
microservice {
  services {

    names = [ated, amls, awrs]
```

and, make appropriate changes as necessary

```text
 awrs {
      serviceRedirectUrl: "http://localhost:9913/alcohol-wholesale-scheme"
    }
    amls {
      serviceRedirectUrl: "http://localhost:9222/anti-money-laundering"
    }
```

where,

| Property | Description |
|------|-------------------|
| serviceRedirectUrl | page uri that agent or client would land when it leaves the service |

Other important files to be modified/used:

```text
messages
BCUtils.scala
*Spec.scala (as per use)
```

Please run test and test coverage before raising a PR.

APIs used
---------

| Property | Description |
|------|-------------------|
|```/clear-cache/:service```| Used to clear the cache from another service, usually used after user has subscribed to their particular service |
|```/fetch-review-details/:service```| Used to retrieve review details returned from ETMP after registering to ETMP |

Note - Remember to use the partials header carrier for frontend to frontend calls

Requirements
------------

This service is written in [Scala] and [Play], so needs the latest [JRE] to run.


Authentication
------------

Authentication is via [Government Gateway]


Acronyms
--------

In the context of this service we use the following acronyms:

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [URL]: Uniform Resource Locator

### All tests and checks

> `sbt runAllChecks`

This is an sbt command alias specific to this project. It will run

- clean
- compile
- unit tests
- and produce a coverage report.

You can view the coverage report in the browser by pasting the generated url.

#### Installing sbt plugin to check for library updates.
To check for dependency updates locally you will need to create this file locally ~/.sbt/1.0/plugins/sbt-updates.sbt
and paste - addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3") - into the file.
Then run:

> `sbt dependencyUpdates `

To view library update suggestions - this does not cover sbt plugins.
It is not advised to install the plugin for the project.


License
-------

This code is open source software licensed under the [Apache 2.0 License].

[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html

[Government Gateway]: http://www.gateway.gov.uk/

[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[JSON]: http://json.org/
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
