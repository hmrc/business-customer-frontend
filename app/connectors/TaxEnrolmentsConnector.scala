/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Mode.Mode
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import utils.GovernmentGatewayConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TaxEnrolmentsConnector extends TaxEnrolmentsConnector {
  val appName: String = AppName(Play.current.configuration).appName
  val audit: Audit = new Audit(appName, BusinessCustomerFrontendAuditConnector)
  val metrics = Metrics
  val serviceUrl = baseUrl("tax-enrolments")
  val enrolmentUrl = s"$serviceUrl/tax-enrolments"
  val http: CoreGet with CorePost = WSHttp
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait TaxEnrolmentsConnector extends ServicesConfig with RawResponseReads with Auditable {

  def serviceUrl: String

  def enrolmentUrl: String

  def http: CoreGet with CorePost

  def metrics: Metrics

  def enrol(enrolRequest: NewEnrolRequest, groupId: String, arn: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val enrolmentKey = s"${GovernmentGatewayConstants.KnownFactsAgentServiceName}~${GovernmentGatewayConstants.KnownFactsAgentRefNo}~$arn"
    val postUrl = s"""$enrolmentUrl/groups/$groupId/enrolments/$enrolmentKey"""
    val jsonData = Json.toJson(enrolRequest)

    val timerContext = metrics.startTimer(MetricsEnum.EMAC_AGENT_ENROL)
    http.POST[JsValue, HttpResponse](postUrl, jsonData) map { response =>
      timerContext.stop()
      auditEnrolCall(postUrl, enrolRequest, response)
      processResponse(response, postUrl, enrolRequest)
    }
  }

  private def processResponse(response: HttpResponse, postUrl: String, enrolRequest: NewEnrolRequest) = {
    response.status match {
      case CREATED =>
        metrics.incrementSuccessCounter(MetricsEnum.EMAC_AGENT_ENROL)
        response
      case _ =>
        metrics.incrementFailedCounter(MetricsEnum.EMAC_AGENT_ENROL)
        Logger.warn(s"[TaxEnrolmentsConnector][enrol] - " +
          s"emac url: $postUrl, " +
          s"service: ${GovernmentGatewayConstants.KnownFactsAgentServiceName}, " +
          s"verfiers sent: ${enrolRequest.verifiers}, " +
          s"response: ${response.body}")
        response
    }
  }

  private def auditEnrolCall(postUrl: String, input: NewEnrolRequest, response: HttpResponse)(implicit hc: HeaderCarrier) = {
    val eventType = response.status match {
      case CREATED => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent(transactionName = "emacEnrolCallES08",
      detail = Map("txName" -> "emacAllocateEnrolmentToGroup",
        "friendlyName" -> s"${input.friendlyName}",
        "serviceName" -> s"${GovernmentGatewayConstants.KnownFactsAgentServiceName}",
        "postUrl" -> s"$postUrl",
        "requestBody" -> s"${Json.prettyPrint(Json.toJson(input))}",
        "verifiers" -> s"${input.verifiers}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"))
  }

}
