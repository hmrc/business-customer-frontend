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

package services

import audit.Auditable
import config.ApplicationConfig
import connectors.{NewBusinessCustomerConnector, TaxEnrolmentsConnector}

import javax.inject.Inject
import models._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.model.EventTypes
import utils.BusinessCustomerConstants.SoleTrader
import utils.GovernmentGatewayConstants

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class AgentRegistrationService @Inject() (val taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                          val dataCacheConnector: DataCacheService,
                                          val audit: Auditable,
                                          implicit val config: ApplicationConfig,
                                          val businessCustomerConnector: NewBusinessCustomerConnector)
    extends Logging {

  def enrolAgent(serviceName: String)(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) => enrolAgent(serviceName, businessDetails)
      case _ =>
        logger.warn(s"[AgentRegistrationService][enrolAgent] - No Service details found in DataCache for")
        throw new RuntimeException("We could not find your details. Check and try again.")
    }
  }

  def isAgentEnrolmentAllowed(serviceName: String): Boolean = {
    getServiceAgentEnrolmentType(serviceName).isDefined
  }

  private def enrolAgent(serviceName: String, businessDetails: ReviewDetails)(implicit
      authContext: StandardAuthRetrievals,
      hc: HeaderCarrier,
      ec: ExecutionContext): Future[HttpResponse] = {

    def failedEnrolment = throw new RuntimeException("Failed to enrol -  no details found for the agent (not a valid GG user)")

    val arn                 = getArn(businessDetails)
    val knownFacts          = createEnrolmentVerifiers(businessDetails, arn)
    val (ggCredId, groupId) = authContext.credId.getOrElse(failedEnrolment) -> authContext.groupId.getOrElse(failedEnrolment)

    for {
      _             <- businessCustomerConnector.addKnownFacts(knownFacts, arn)
      enrolResponse <- taxEnrolmentsConnector.enrol(createEnrolRequest(serviceName, knownFacts, ggCredId), groupId, arn)
    } yield {
      auditEnrolAgent(businessDetails, enrolResponse, createEnrolRequest(serviceName, knownFacts, ggCredId))
      enrolResponse
    }
  }

  private def getServiceAgentEnrolmentType(serviceName: String): Option[String] = {
    Try {
      config.conf.getString(s"microservice.services.${serviceName.toLowerCase}.agentEnrolmentService")
    } match {
      case Success(s) => Some(s)
      case _          => None
    }
  }

  private def createEnrolRequest(serviceName: String, knownFacts: Verifiers, ggCredId: String)(implicit
      authContext: StandardAuthRetrievals): NewEnrolRequest = {
    getServiceAgentEnrolmentType(serviceName) match {
      case Some(_) =>
        NewEnrolRequest(
          userId = ggCredId,
          friendlyName = GovernmentGatewayConstants.FriendlyName,
          `type` = GovernmentGatewayConstants.enrolmentType,
          verifiers = knownFacts.verifiers)
      case _ =>
        logger.warn(s"[AgentRegistrationService][createEnrolRequest] - No Agent Enrolment name found in config found = $serviceName")
        throw new RuntimeException(
          s"Agent enrolment service name does not exist for : $serviceName." +
            s" This should be in the conf file against 'services.${serviceName.toLowerCase}.agentEnrolmentService'")
    }
  }

  private def createEnrolmentVerifiers(businessDetails: ReviewDetails, arn: String)(implicit authContext: StandardAuthRetrievals): Verifiers = {
    val verifiers = businessDetails.utr match {
      case Some(utr) =>
        val ukPostCodeVerifier = Verifier(
          GovernmentGatewayConstants.KnownFactsUKPostCode,
          businessDetails.businessAddress.postcode.getOrElse(throw new RuntimeException("No Registered UK Postcode found for the agent!"))
        )
        businessDetails.businessType match {
          case Some(SoleTrader) =>
            ukPostCodeVerifier :: List(Verifier(GovernmentGatewayConstants.KnownFactsUniqueTaxRef, utr))
          case _ =>
            ukPostCodeVerifier :: List(Verifier(GovernmentGatewayConstants.KnownFactsCompanyTaxRef, utr))
        }
      case _ => List(Verifier(GovernmentGatewayConstants.KnownFactsAgentRef, arn))
      // NOTE: Non-UK agents DO NOT have UTRs and we don't capture any Postcode/Intl Postcode
    }
    Verifiers(verifiers)
  }

  private def auditEnrolAgent(businessDetails: ReviewDetails, enrolResponse: HttpResponse, enrolReq: NewEnrolRequest)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): Unit = {
    val status = enrolResponse.status match {
      case CREATED => EventTypes.Succeeded
      case _       => EventTypes.Failed
    }
    audit.sendDataEvent(
      "enrolAgent",
      detail = Map(
        "txName"               -> "enrolAgent",
        "agentReferenceNumber" -> businessDetails.agentReferenceNumber.getOrElse(""),
        "service"              -> GovernmentGatewayConstants.KnownFactsAgentServiceName,
        "status"               -> status
      )
    )
  }

  private def getArn(businessDetails: ReviewDetails): String = {
    businessDetails.agentReferenceNumber.getOrElse {
      throw new RuntimeException("[AgentRegistrationService][createEnrolmentVerifiers] - No unique authorisation number found")
    }
  }

}
