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
import models.{MatchBusinessData, StandardAuthRetrievals}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.model.EventTypes

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BusinessMatchingConnector @Inject()(val audit: Auditable,
                                          val http: HttpClientV2,
                                          val conf: ApplicationConfig) extends RawResponseReads with Logging {

  val baseUri = "business-matching"
  val lookupUri = "business-lookup"

  def lookup(lookupData: MatchBusinessData, userType: String, service: String)
            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {
    val authLink = authContext.authLink
    val url = s"""${conf.businessMatching}/$authLink/$baseUri/$lookupUri/${lookupData.utr}/$userType"""
    http.post(url"${url}").withBody(Json.toJson(lookupData)).execute map { response =>
      auditMatchCall(lookupData, userType, response, service)
      response.status match {
        case OK | NOT_FOUND =>
          Try{
            Json.parse(response.body)
          } match {
            case Success(s) => s
            case Failure(_) => truncateContactDetails(response.body)
          }
        case SERVICE_UNAVAILABLE =>
          logger.warn(s"[BusinessMatchingConnector][lookup] - Service unavailableException ${lookupData.utr}")
          throw new ServiceUnavailableException("Service unavailable")
        case BAD_REQUEST =>
          logger.warn(s"[BusinessMatchingConnector][lookup] - Bad Request Exception ${lookupData.utr}")
          throw new BadRequestException("Bad Request")
        case INTERNAL_SERVER_ERROR =>
          logger.warn(s"[BusinessMatchingConnector][lookup] - Service Internal server error ${lookupData.utr}")
          throw new InternalServerException("Internal server error")
        case status =>
          logger.warn(s"[BusinessMatchingConnector][lookup] - $status Exception ${lookupData.utr}")
          throw new RuntimeException("Unknown response")
      }
    }
  }

  private def truncateContactDetails(responseJson: String): JsValue = {
    val replacedX1 = responseJson.replaceAll("[\r\n\t]", "")
    val removedContactDetails = replacedX1.substring(0, replacedX1.indexOf("contactDetails"))
    val correctedJsonString = removedContactDetails.substring(0, removedContactDetails.lastIndexOf(","))
    val validJson = correctedJsonString + "}"
    Json.parse(validJson)
  }

  private def auditMatchCall(input: MatchBusinessData, userType: String, response: HttpResponse, service: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Any = {
    val eventType = response.status match {
      case OK | NOT_FOUND => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    audit.sendDataEvent(
      transactionName = "etmpMatchCall",
      detail = Map(
        "txName" -> "etmpMatchCall",
        "userType" -> s"$userType",
        "service" -> s"$service",
        "utr" -> input.utr,
        "requiresNameMatch" -> s"${input.requiresNameMatch}",
        "isAnAgent" -> s"${input.isAnAgent}",
        "individual" -> s"${input.individual}",
        "organisation" -> s"${input.organisation}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" ->  s"$eventType"
      )
    )

    def getAddressPiece(piece: Option[JsValue]):String = piece.map(_.toString).getOrElse("")

    if (eventType == EventTypes.Succeeded) {
      val data = Try {
        Json.parse(response.body)
      } match {
        case Success(s) => s
        case Failure(_) => truncateContactDetails(response.body)
      }
      (data \\ "address").headOption.map { _ =>
        audit.sendDataEvent(
          transactionName = "postcodeAddressSubmitted",
          detail = Map(
            "submittedLine1" -> (data \\ "addressLine1").head.as[String],
            "submittedLine2" -> (data \\ "addressLine2").head.as[String],
            "submittedLine3" -> getAddressPiece((data \\ "addressLine3").headOption),
            "submittedLine4" -> getAddressPiece((data \\ "addressLine4").headOption),
            "submittedPostcode" -> getAddressPiece((data \\ "postalCode").headOption),
            "submittedCountry" -> (data \\ "countryCode").head.as[String]
          )
        )
      }
    }
  }
}
