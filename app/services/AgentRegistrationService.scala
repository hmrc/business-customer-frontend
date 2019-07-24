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

package services

import audit.Auditable
import config.{AuthClientConnector, BusinessCustomerFrontendAuditConnector}
import connectors.{DataCacheConnector, NewBusinessCustomerConnector, TaxEnrolmentsConnector}
import models._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, _}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, RunMode}
import utils.{BCUtils, GovernmentGatewayConstants}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AgentRegistrationService extends RunMode with Auditable with AuthorisedFunctions {

  def taxEnrolmentsConnector: TaxEnrolmentsConnector

  def dataCacheConnector: DataCacheConnector

  def businessCustomerConnector: NewBusinessCustomerConnector

  def enrolAgent(serviceName: String)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails)
      case _ =>
        Logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
    }
  }

  def isAgentEnrolmentAllowed(serviceName: String): Boolean = {
    getServiceAgentEnrolmentType(serviceName).isDefined
  }

  private def enrolAgent(serviceName: String, businessDetails: ReviewDetails)
                        (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[HttpResponse] = {
    val arn = getArn(businessDetails)
    val knownFacts = createEnrolmentVerifiers(businessDetails, arn)
    for {
      (groupId, ggCredId) <- getUserAuthDetails
      _ <- businessCustomerConnector.addKnownFacts(knownFacts, arn)
      enrolResponse <- taxEnrolmentsConnector.enrol(createEnrolRequest(serviceName, knownFacts, ggCredId), groupId, arn)
    } yield {
      auditEnrolAgent(businessDetails, enrolResponse, createEnrolRequest(serviceName, knownFacts, ggCredId))
      enrolResponse
    }
  }

  private def getServiceAgentEnrolmentType(serviceName: String): Option[String] = {
    Play.configuration.getString(s"microservice.services.${serviceName.toLowerCase}.agentEnrolmentService")
  }

  private def createEnrolRequest(serviceName: String, knownFacts: Verifiers, ggCredId: String)(implicit bcContext: BusinessCustomerContext): NewEnrolRequest = {
    getServiceAgentEnrolmentType(serviceName) match {
      case Some(enrolServiceName) =>
        NewEnrolRequest(userId = ggCredId,
          friendlyName = GovernmentGatewayConstants.FriendlyName,
          `type` = GovernmentGatewayConstants.enrolmentType,
          verifiers = knownFacts.verifiers)
      case _ =>
        Logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = $serviceName")
        throw new RuntimeException(Messages("bc.agent-service.error.no-agent-enrolment-service-name", serviceName, serviceName.toLowerCase))
    }
  }

  private def createEnrolmentVerifiers(businessDetails: ReviewDetails, arn: String)(implicit bcContext: BusinessCustomerContext): Verifiers = {
    val verifiers = businessDetails.utr match {
      case Some(utr) =>
        val ukPostCodeVerifier = Verifier(GovernmentGatewayConstants.KnownFactsUKPostCode,
          businessDetails.businessAddress.postcode.getOrElse(throw new RuntimeException("No Registered UK Postcode found for the agent!")))
        businessDetails.businessType match {
          case Some("Sole Trader") =>
            ukPostCodeVerifier :: List(Verifier(GovernmentGatewayConstants.KnownFactsUniqueTaxRef, utr))
          case _ =>
            ukPostCodeVerifier :: List(Verifier(GovernmentGatewayConstants.KnownFactsCompanyTaxRef, utr))
        }
      case _ => List(Verifier(GovernmentGatewayConstants.KnownFactsAgentRef, arn)) //NOTE: Non-UK agents DO NOT have UTRs and we don't capture any Postcode/Intl Postcode
    }
    Verifiers(verifiers)
  }

  private def getUserAuthDetails(implicit hc: HeaderCarrier): Future[(String, String)] = {
    authorised().retrieve(credentials and groupIdentifier) {
      case Credentials(ggCredId, _) ~ Some(groupId) => Future.successful(BCUtils.validateGroupId(groupId), ggCredId)
      case _ => throw new RuntimeException("Failed to enrol -  no details found for the agent (not a valid GG user)")
    }
  }

  private def auditEnrolAgent(businessDetails: ReviewDetails, enrolResponse: HttpResponse, enrolReq: NewEnrolRequest)(implicit hc: HeaderCarrier) = {
    val status = enrolResponse.status match {
      case CREATED => EventTypes.Succeeded
      case _ => EventTypes.Failed
    }
    sendDataEvent("enrolAgent", detail = Map(
      "txName" -> "enrolAgent",
      "agentReferenceNumber" -> businessDetails.agentReferenceNumber.getOrElse(""),
      "service" -> GovernmentGatewayConstants.KnownFactsAgentServiceName,
      "status" -> status
    ))
  }

  private def getArn(businessDetails: ReviewDetails): String = {
    businessDetails.agentReferenceNumber.getOrElse {
      throw new RuntimeException(Messages("bc.agent-service.error.no-agent-reference", "[AgentRegistrationService][createEnrolmentVerifiers]"))
    }
  }
}

object AgentRegistrationService extends AgentRegistrationService {
  val appName: String = AppName(Play.current.configuration).appName
  val taxEnrolmentsConnector = TaxEnrolmentsConnector
  val dataCacheConnector = DataCacheConnector
  val businessCustomerConnector = NewBusinessCustomerConnector
  val audit: Audit = new Audit(appName, BusinessCustomerFrontendAuditConnector)
  val authConnector: AuthConnector = AuthClientConnector
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
