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

package connectors

import audit.Auditable
import config.ApplicationConfig

import javax.inject.Inject
import models._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.GovernmentGatewayConstants

import scala.concurrent.{ExecutionContext, Future}

class NewBusinessCustomerConnector @Inject()(config: ApplicationConfig,
                                             val audit: Auditable,
                                             val http: DefaultHttpClient) extends RawResponseReads with Logging {

  val baseUri = "business-customer"
  val registerUri = "register"
  val knownFactsUri = "known-facts"
  val updateRegistrationDetailsURI = "update"

  def addKnownFacts(knownFacts: Verifiers, arn: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = authContext.authLink
    val postUrl = s"""${config.businessCustomer}/$authLink/$baseUri/${GovernmentGatewayConstants.KnownFactsAgentServiceName}/$knownFactsUri/$arn"""
    val jsonData = Json.toJson(knownFacts)

    http.POST[JsValue, HttpResponse](postUrl, jsonData, Seq.empty)
  }

  def auditRegisterCall(input: BusinessRegistrationRequest, response: HttpResponse, service: String, isNonUKClientRegisteredByAgent: Boolean = false)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    val transactionName = input.address.countryCode.toUpperCase match {
      case "GB" => "etmpRegisterUKCall"
      case _ => if (isNonUKClientRegisteredByAgent) "etmpClientRegisteredByAgent" else "etmpRegisterNonUKCall"
    }

    audit.sendDataEvent(
      transactionName = transactionName,
      detail = Map(
        "txName" -> transactionName,
        "service" -> s"$service",
        "address" -> s"${input.address}",
        "contactDetails" -> s"${input.contactDetails}",
        "identification" -> s"${input.identification}",
        "isAGroup" -> s"${input.isAGroup}",
        "isAnAgent" -> s"${input.isAnAgent}",
        "organisation" -> s"${input.organisation}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" ->  s"$eventType"
      )
    )

    audit.sendDataEvent(
      transactionName = if (input.address.postalCode.isDefined) "manualAddressSubmitted" else "internationalAddressSubmitted",
      detail = Map(
        "submittedLine1" -> input.address.addressLine1.toString,
        "submittedLine2" -> input.address.addressLine2.toString,
        "submittedLine3" -> input.address.addressLine3.getOrElse(""),
        "submittedLine4" -> input.address.addressLine4.getOrElse(""),
        "submittedPostcode" -> input.address.postalCode.getOrElse(""),
        "submittedCountry" -> input.address.countryCode
      )
    )
  }

  def register(registerData: BusinessRegistrationRequest, service: String, isNonUKClientRegisteredByAgent: Boolean = false)
              (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[BusinessRegistrationResponse] = {

    val authLink = authContext.authLink
    val postUrl = s"""${config.businessCustomer}/$authLink/$baseUri/$registerUri"""
    val jsonData = Json.toJson(registerData)

    http.POST(postUrl, jsonData, Seq.empty) map { response =>
      auditRegisterCall(registerData, response, service, isNonUKClientRegisteredByAgent)
      response.status match {
        case OK => response.json.as[BusinessRegistrationResponse]
        case NOT_FOUND =>
          logger.warn(s"[BusinessCustomerConnector][register] - Not Found Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"Not Found - Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          logger.warn(s"[BusinessCustomerConnector][register] - Service Unavailable Exception ${registerData.organisation.organisationName}")
          throw new ServiceUnavailableException(s"Service Unavailable - Exception ${response.body}")
        case status =>
          logger.warn(s"[BusinessCustomerConnector][register] - $status Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"Unknown Status $status - Exception ${response.body}")
      }
    }
  }

  def updateRegistrationDetails(safeId: String, updateRegistrationDetails: UpdateRegistrationDetailsRequest)
                               (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = authContext.authLink
    val postUrl = s"""${config.businessCustomer}/$authLink/$baseUri/$updateRegistrationDetailsURI/$safeId"""
    val jsonData = Json.toJson(updateRegistrationDetails)
    http.POST(postUrl, jsonData, Seq.empty) map { response =>
      response.status match {
        case OK => response
        case NOT_FOUND =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - Not Found Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new InternalServerException(s"Not Found - Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - Service Unavailable ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new ServiceUnavailableException(s"Service Unavailable - Exception ${response.body}")
        case status =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - $status Exception ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new InternalServerException(s"Unknown Status $status - Exception ${response.body}")
      }
    }
  }

}
