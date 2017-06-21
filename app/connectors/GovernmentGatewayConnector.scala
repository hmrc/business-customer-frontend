/*
 * Copyright 2017 HM Revenue & Customs
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
import config.{BusinessCustomerFrontendAuditConnector, WSHttp}
import metrics.{Metrics, MetricsEnum}
import models._
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  val appName: String = AppName.appName
  val metrics = Metrics
  val serviceUrl = baseUrl("government-gateway")
  val enrolUri = "enrol"
  val http: HttpGet with HttpPost = WSHttp
}

trait GovernmentGatewayConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def enrolUri: String

  def http: HttpGet with HttpPost

  def metrics: Metrics

  def enrol(enrolRequest: EnrolRequest, knownFacts: KnownFactsForService)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val jsonData = Json.toJson(enrolRequest)
    val postUrl = s"""$serviceUrl/$enrolUri"""

    val timerContext = metrics.startTimer(MetricsEnum.GG_AGENT_ENROL)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      timerContext.stop()
      auditEnrolCall(enrolRequest, response)
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.GG_AGENT_ENROL)
          response
        case BAD_REQUEST =>
          metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
            s"gg url:$postUrl, " +
            s"Bad Request Exception account Ref:${enrolRequest.knownFacts}, " +
            s"Service: ${enrolRequest.serviceName}}")
          throw new BadRequestException(response.body)
        case NOT_FOUND =>
          metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
            s"Not Found Exception account Ref:${enrolRequest.knownFacts}, " +
            s"Service: ${enrolRequest.serviceName}}")
          throw new NotFoundException(response.body)
        case SERVICE_UNAVAILABLE =>
          metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
            s"gg url:$postUrl, " +
            s"Service Unavailable Exception account Ref:${enrolRequest.knownFacts}, " +
            s"Service: ${enrolRequest.serviceName}}")
          throw new ServiceUnavailableException(response.body)
        case BAD_GATEWAY =>
          metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - BAD_GATEWAY " +
            s"gg url:$postUrl, " +
            s"Service: ${enrolRequest.serviceName}, " +
            s"Register Known Facts:${knownFacts.facts}, " +
            s"Enrol Known Facts:${enrolRequest.knownFacts}, " +
            s"Reponse Body: ${response.body}," +
            s"Reponse Status: ${response.status}")
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.GG_AGENT_ENROL)
          Logger.warn(s"[GovernmentGatewayConnector][enrol] - " +
            s"gg url:$postUrl, " +
            s"status:$status Exception account Ref:${enrolRequest.knownFacts}, " +
            s"Service: ${enrolRequest.serviceName}}" +
            s"Reponse Body: ${response.body}," +
            s"Reponse Status: ${response.status}")
          throw new InternalServerException(response.body)
      }
    }

  }

  private def auditEnrolCall(input: EnrolRequest, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "ggEnrolCall",
      detail = Map("txName" -> "ggEnrolCall",
        "friendlyName" -> s"${input.friendlyName}",
        "serviceName" -> s"${input.serviceName}",
        "portalId" -> s"${input.portalId}",
        "knownFacts" -> s"${input.knownFacts}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" ->  s"${eventType}"))
  }

}
