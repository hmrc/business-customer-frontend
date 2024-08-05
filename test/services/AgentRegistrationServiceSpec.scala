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

package services

import audit.Auditable
import builders.AuthBuilder
import config.ApplicationConfig
import connectors._
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AgentRegistrationServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfter with Injecting {


  val mockTaxEnrolmentConnector = mock[TaxEnrolmentsConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockNewBusinessCustomerConnector = mock[NewBusinessCustomerConnector]
  val mockAuthClientConnector = mock[AuthConnector]
  val mockAuditable = mock[Auditable]

  val appConfig = inject[ApplicationConfig]
  implicit val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  implicit val ec: ExecutionContext = mcc.executionContext

  object TestAgentRegistrationService extends AgentRegistrationService(
    mockTaxEnrolmentConnector,
    mockDataCacheConnector,
    mockAuditable,
    appConfig,
    mockNewBusinessCustomerConnector
  )

  "AgentRegistrationService" must {

    "isAgentEnrolmentAllowed is true if the configuration is setup" in {
      TestAgentRegistrationService.isAgentEnrolmentAllowed("ATED") must be(true)
    }

    "isAgentEnrolmentAllowed is false if the configuration is setup" in {
      TestAgentRegistrationService.isAgentEnrolmentAllowed("AWRS") must be(false)
      TestAgentRegistrationService.isAgentEnrolmentAllowed("AMLS") must be(false)
    }

    "enrolAgent throw exception if we have no agent ref no" in {
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = None)

      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("No unique authorisation number found")
    }

    "for sole traders, enrolAgent return the status OK if it worked" in {
      val returnedReviewDetails = ReviewDetails(businessName = "Bus Name", businessType = Some("Sole Trader"),
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        utr = Some("1111111111"),
        agentReferenceNumber = Some("agent123"))

      implicit val user = AuthBuilder.createSaUser()

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK,  "")))
      when(mockTaxEnrolmentConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, "")))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      await(result).status must be(CREATED)
    }

    "for agents other than sole traders, enrolAgent return the status OK if it worked" in {
      val returnedReviewDetails = ReviewDetails(businessName = "Bus Name", businessType = Some("Corporate Body"),
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        utr = Some("1111111111"),
        agentReferenceNumber = Some("agent123"))

      implicit val user = AuthBuilder.createAgentAuthContext("userid", "username")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockTaxEnrolmentConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, "")))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      await(result).status must be(CREATED)
    }

    "enrolAgent throws an exception if NO postcode is found for UK Agents" in {
      val returnedReviewDetails = ReviewDetails(businessName = "Bus Name", businessType = Some("Corporate Body"),
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), None, "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        utr = Some("1111111111"),
        agentReferenceNumber = Some("agent123"))

      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockTaxEnrolmentConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, "")))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("No Registered UK Postcode found for the agent!")
    }

    "enrolAgent throws an exception if no groupId is found" in {
      val returnedReviewDetails = ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        agentReferenceNumber = Some("agent123"))

      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), None)))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockTaxEnrolmentConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(CREATED, "")))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Failed to enrol -  no details found for the agent (not a valid GG user")
    }

    "enrolAgent return the status anything if it does not work" in {
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = Some("agent123"))

      implicit val user = AuthBuilder.createAgentAuthContext("userid", "username")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockTaxEnrolmentConnector.enrol(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, "")))

      val result = TestAgentRegistrationService.enrolAgent("ATED")
      await(result).status must be(BAD_GATEWAY)
    }

    "enrolAgent throw an exception if we have no details" in {

      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      val result = TestAgentRegistrationService.enrolAgent("ATED")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("We could not find your details. Check and try again.")
    }

    "enrolAgent throw an exception if we have no service config" in {
      val returnedReviewDetails = new ReviewDetails(businessName = "Bus Name", businessType = None,
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country"),
        sapNumber = "sap123",
        safeId = "safe123",
        isAGroup = false,
        directMatch = false,
        agentReferenceNumber = Some("agent123"))

      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createAgentAuthContext("userid", "username")

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))
      when(mockAuthClientConnector.authorise[Any](any(), any())(any(), any())).thenReturn(Future.successful(new ~(Credentials("ggcredId", "ggCredType"), Some("42424200-0000-0000-0000-000000000000"))))
      when(mockNewBusinessCustomerConnector.addKnownFacts(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, "")))
      val result = TestAgentRegistrationService.enrolAgent("INVALID_SERVICE_NAME")
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must startWith("Agent enrolment service name does not exist for")
    }
  }
}
