/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.model.EventTypes
import utils.GovernmentGatewayConstants

import scala.concurrent.{ExecutionContext, Future}

class BusinessCustomerConnector @Inject()(val http: HttpClientV2,
                                          val audit: Auditable,
                                          implicit val config: ApplicationConfig) extends RawResponseReads with Logging {

  val baseUri = "business-customer"
  val registerUri = "register"
  val knownFactsUri = "known-facts"
  val updateRegistrationDetailsURI = "update"

  def addKnownFacts(knownFacts: KnownFactsForService)(implicit authContext: StandardAuthRetrievals,
                                                      hc: HeaderCarrier,
                                                      ec: ExecutionContext): Future[HttpResponse] = {
    val authLink = authContext.authLink
    val postUrl = s"""${config.businessCustomer}/$authLink/$baseUri/${GovernmentGatewayConstants.KnownFactsAgentServiceName}/$knownFactsUri"""
    val jsonData = Json.toJson(knownFacts)
    http.post(url"${postUrl}").withBody(jsonData).execute
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
        "service" -> s"$service", "address" -> s"${input.address}",
        "contactDetails" -> s"${input.contactDetails}", "identification" -> s"${input.identification}",
        "isAGroup" -> s"${input.isAGroup}", "isAnAgent" -> s"${input.isAnAgent}",
        "organisation" -> s"${input.organisation}", "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}", "status" ->  s"$eventType"
      )
    )

    def getAddressPiece(piece: Option[String]):String = piece.getOrElse("")

    audit.sendDataEvent(
      transactionName = if (input.address.postalCode.isDefined) "manualAddressSubmitted" else "internationalAddressSubmitted",
      detail = Map(
        "submittedLine1" -> input.address.addressLine1, "submittedLine2" -> input.address.addressLine2,
        "submittedLine3" -> getAddressPiece(input.address.addressLine3), "submittedLine4" -> getAddressPiece(input.address.addressLine4),
        "submittedPostcode" -> getAddressPiece(input.address.postalCode), "submittedCountry" -> input.address.countryCode
      )
    )
  }


  def register(registerData: BusinessRegistrationRequest, service: String, isNonUKClientRegisteredByAgent: Boolean = false)
              (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[BusinessRegistrationResponse] = {
    val authLink = authContext.authLink
    val postUrl = s"""${config.businessCustomer}/$authLink/$baseUri/$registerUri"""
    val jsonData = Json.toJson(registerData)
    http.post(url"${postUrl}").withBody(jsonData).execute map { response =>
      auditRegisterCall(registerData, response, service, isNonUKClientRegisteredByAgent)
      response.status match {
        case OK => response.json.as[BusinessRegistrationResponse]
        case NOT_FOUND =>
          logger.warn(s"[BusinessCustomerConnector][register] - Not Found Exception ${registerData.organisation.organisationName}")
          throw new InternalServerException(s"Not Found - Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          logger.warn(s"[BusinessCustomerConnector][register] - Service Unavailable Exception ${registerData.organisation.organisationName}")
          throw new ServiceUnavailableException(s"Service unavailable  Exception ${response.body}")
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
    http.post(url"${postUrl}").withBody(jsonData).execute map { response =>
      response.status match {
        case OK => response
        case NOT_FOUND =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - Not Found ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new InternalServerException(s"Not Found - Exception ${response.body}")
        case SERVICE_UNAVAILABLE =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - Service Unavailable ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new ServiceUnavailableException(s"Service Unavailable - Exception ${response.body}")
        case status =>
          logger.warn(
            s"[BusinessCustomerConnector][updateRegistrationDetails] - $status ${updateRegistrationDetails.organisation.map(_.organisationName)}"
          )
          throw new InternalServerException(s"Unknown Status $status - Exception ${response.body}")
      }
    }
  }

}
