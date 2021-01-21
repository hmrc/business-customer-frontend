/*
 * Copyright 2021 HM Revenue & Customs
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
import metrics.{MetricsEnum, MetricsService}
import models._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.GovernmentGatewayConstants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsConnector @Inject()(val metrics: MetricsService,
                                       implicit val config: ApplicationConfig,
                                       val audit: Auditable,
                                       val http: DefaultHttpClient) extends RawResponseReads with Logging {

  val enrolmentUrl = s"${config.taxEnrolments}/tax-enrolments"

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
        logger.warn(
          s"[TaxEnrolmentsConnector][enrol] - " +
          s"emac url: $postUrl, " +
          s"service: ${GovernmentGatewayConstants.KnownFactsAgentServiceName}, " +
          s"verfiers sent: ${enrolRequest.verifiers}, " +
          s"response: ${response.body}"
        )
        response
    }
  }

  private def auditEnrolCall(postUrl: String, input: NewEnrolRequest, response: HttpResponse)(implicit hc: HeaderCarrier): Unit = {
    val eventType = response.status match {
      case CREATED => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    audit.sendDataEvent(
      transactionName = "emacEnrolCallES08",
      detail = Map(
        "txName" -> "emacAllocateEnrolmentToGroup",
        "friendlyName" -> s"${input.friendlyName}",
        "serviceName" -> s"${GovernmentGatewayConstants.KnownFactsAgentServiceName}",
        "postUrl" -> s"$postUrl",
        "requestBody" -> s"${Json.prettyPrint(Json.toJson(input))}",
        "verifiers" -> s"${input.verifiers}",
        "responseStatus" -> s"${response.status}",
        "responseBody" -> s"${response.body}",
        "status" -> s"$eventType"
      )
    )
  }

}
