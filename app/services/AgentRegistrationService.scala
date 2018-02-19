/*
 * Copyright 2018 HM Revenue & Customs
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

package services

import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import connectors.{BusinessCustomerConnector, DataCacheConnector, GovernmentGatewayConnector}
import models._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.{Logger, Play}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, RunMode}
import utils.GovernmentGatewayConstants

import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

trait AgentRegistrationService extends RunMode with Auditable {

  def governmentGatewayConnector: GovernmentGatewayConnector

  def dataCacheConnector: DataCacheConnector

  def businessCustomerConnector: BusinessCustomerConnector

  def enrolAgent(serviceName: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails)
      case _ =>
        Logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
    }
  }

  def isAgentEnrolmentAllowed(serviceName: String) :Boolean = {
    getServiceAgentEnrolmentType(serviceName).isDefined
  }

  private def enrolAgent(serviceName: String, businessDetails: ReviewDetails)
                        (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val enrolReq = createEnrolRequest(serviceName, businessDetails)
    val knownFacts = createKnownFacts(businessDetails)
    for {
      _ <- businessCustomerConnector.addKnownFacts(knownFacts)
      enrolResponse <- governmentGatewayConnector.enrol(enrolReq, knownFacts)
    } yield {
      auditEnrolAgent(businessDetails, enrolResponse, enrolReq)
      enrolResponse
    }
  }

  private def getServiceAgentEnrolmentType(serviceName: String) : Option[String] = {
    Play.configuration.getString(s"microservice.services.${serviceName.toLowerCase}.agentEnrolmentService")
  }

  private def createEnrolRequest(serviceName: String, businessDetails: ReviewDetails)(implicit bcContext: BusinessCustomerContext): EnrolRequest = {
    getServiceAgentEnrolmentType(serviceName) match {
      case Some(enrolServiceName) =>
        val knownFactsList = List(businessDetails.agentReferenceNumber, Some(""), Some(""), Some(businessDetails.safeId)).flatten
        EnrolRequest(portalId = GovernmentGatewayConstants.PortalIdentifier,
          serviceName = enrolServiceName,
          friendlyName = GovernmentGatewayConstants.FriendlyName,
          knownFacts = knownFactsList)
      case _ =>
        Logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = $serviceName")
        throw new RuntimeException(Messages("bc.agent-service.error.no-agent-enrolment-service-name", serviceName, serviceName.toLowerCase))
    }
  }

  private def createKnownFacts(businessDetails: ReviewDetails)(implicit bcContext: BusinessCustomerContext) = {
    val agentRefNo = businessDetails.agentReferenceNumber.getOrElse {
      Logger.warn(s"[AgentRegistrationService][createKnownFacts] - No Agent Reference Number Found")
      throw new RuntimeException(Messages("bc.agent-service.error.no-agent-reference", "[AgentRegistrationService][createKnownFacts]"))
    }
    val knownFacts = List(
      KnownFact(GovernmentGatewayConstants.KnownFactsAgentRefNo, agentRefNo),
      KnownFact(GovernmentGatewayConstants.KnownFactsSafeId, businessDetails.safeId)
    )
    KnownFactsForService(knownFacts)
  }

  private def auditEnrolAgent(businessDetails: ReviewDetails, enrolResponse: HttpResponse, enrolReq: EnrolRequest)(implicit hc: HeaderCarrier) = {
    val status = enrolResponse.status match {
      case OK => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent("enrolAgent", detail = Map(
      "txName" -> "enrolAgent",
      "agentReferenceNumber" -> businessDetails.agentReferenceNumber.getOrElse(""),
      "safeId" -> businessDetails.safeId,
      "service" -> enrolReq.serviceName,
      "status" -> status
    ))
  }
}

object AgentRegistrationService extends AgentRegistrationService {
  val governmentGatewayConnector = GovernmentGatewayConnector
  val dataCacheConnector = DataCacheConnector
  val businessCustomerConnector = BusinessCustomerConnector
  val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  val appName: String = AppName.appName
}
