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

package controllers

import java.util.UUID

import config.BusinessCustomerSessionCache
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Address, EnrolResponse, Identifier, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse, SessionKeys }


class ReviewDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentRegistrationService = mock[AgentRegistrationService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")

  val directMatchReviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = true, Some("agent123"))

  val nonDirectMatchReviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

  val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>9001</ErrorNumber><Message>The service HMRC-AGENT-AGENT requires unique identifiers</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

  def testReviewDetailsController(reviewDetails: ReviewDetails) = {
    val mockDataCacheConnector = new DataCacheConnector {
      val sessionCache = BusinessCustomerSessionCache
      override val sourceId: String = "Test"

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(Some(reviewDetails))
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
      override val authConnector = mockAuthConnector
      override val agentRegistrationService = mockAgentRegistrationService
      override val controllerId = "test"
      override val backLinkCacheConnector = mockBackLinkCache
    }
  }

  def testReviewDetailsControllerNotFound = {
    val mockDataCacheConnector = new DataCacheConnector {
      override val sessionCache = BusinessCustomerSessionCache
      override val sourceId: String = "Test"

      var reads: Int = 0

      override def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier) = {
        reads = reads + 1
        Future.successful(None)
      }
    }
    new ReviewDetailsController {
      override def dataCacheConnector = mockDataCacheConnector
      override val authConnector = mockAuthConnector
      override val agentRegistrationService = mockAgentRegistrationService
      override val controllerId = "test"
      override val backLinkCacheConnector = mockBackLinkCache
    }
  }

  override def beforeEach = {
    reset(mockAgentRegistrationService)
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "ReviewDetailsController" must {

    "use the correct data cache connector" in {
      controllers.ReviewDetailsController.dataCacheConnector must be(DataCacheConnector)
    }

    "unauthorised users" must {
      "respond with a redirect" in {
        businessDetailsWithUnAuthorisedUser { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        businessDetailsWithUnAuthorisedUser { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }
    "throw an exception if we have no review details" in {
      businessDetailsWithAuthorisedUserNotFound { result =>
        val thrown = the[RuntimeException] thrownBy contentAsString(result)
        thrown.getMessage must include("We could not find your details. Check and try again.")

      }
    }

    "return Review Details view for a user, when user can't be directly found with login credentials" in {
      businessDetailsWithAuthorisedUser(nonDirectMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Check this is the business you want to register")
      }
    }

    "return Review Details view for a user, when we directly found this user" in {
      businessDetailsWithAuthorisedUser(directMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Check this is the business you want to register")
      }
    }

    "return Review Details view for an agent, when agent can't be directly found with login credentials" in {
      businessDetailsWithAuthorisedAgent(nonDirectMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your agency's details")
      }
    }

    "return Review Details view for an agent, when we directly found this agent" in {
      businessDetailsWithAuthorisedAgent(directMatchReviewDetails) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Confirm your agency's details")
      }
    }

    "return Review Details view for an agent, when we directly found this agent with editable address" in {
      businessDetailsWithAuthorisedAgent(directMatchReviewDetails.copy(isBusinessDetailsEditable = true)) { result =>
        val document = Jsoup.parse(contentAsString(result))
        document.select("h1").text must be("Check your agency details")
      }
    }

    "read existing business details data from cache (without updating data)" in {
      val testDetailsController = businessDetailsWithAuthorisedUser(nonDirectMatchReviewDetails) { result =>
        status(result) must be(OK)
      }
      testDetailsController.dataCacheConnector.reads must be(1)
    }

  }


  "continue " must {

    "unauthorised users" must {
      "respond with a redirect" in {
        continueWithUnAuthorisedUser(service) { result =>
          status(result) must be(SEE_OTHER)
        }
      }

      "be redirected to the unauthorised page" in {
        continueWithUnAuthorisedUser(service) { result =>
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }
    }



    "Authorised Users" must {

      "return service start page correctly for ATED Users" in {

        continueWithAuthorisedUser(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/registered-business-address")
        }
      }

      "return service start page correctly for AWRS Users" in {

        continueWithAuthorisedUser("AWRS") {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/alcohol-wholesale-scheme")
        }
      }

      "redirect to the correspondence address page for capital-gains-tax-service" in {
        continueWithAuthorisedUser("capital-gains-tax") {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/capital-gains-tax/subscription/company/correspondence-address-confirm")
        }
      }

      "redirect to the confirmation for agents address page for capital-gains-tax-subscription service" in {
        continueWithAuthorisedUser("capital-gains-tax-agents") {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/capital-gains-tax/subscription/agent/registered/subscribe")
        }
      }

      "return agent registration page correctly for Agents" in {
        when(mockAgentRegistrationService.isAgentEnrolmentAllowed(Matchers.eq(service))).thenReturn(true)
        continueWithAuthorisedAgent(service) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/ated-subscription/agent-confirmation")
        }
      }

      "return OK and redirect to error page, if different agent try to register with same details" in {
        when(mockAgentRegistrationService.isAgentEnrolmentAllowed(Matchers.eq(service))).thenReturn(true)
        continueWithDuplicategent(service) {
          result =>
            val document = Jsoup.parse(contentAsString(result))
            status(result) must be(OK)
            document.getElementById("content").text() must be("Somebody has already registered from your agency If you require access you need to add administrators to your account in Government Gateway. Get help with this page. Get help with this page.")
        }
      }

      "throw an exception if status is not OK or BAD_GATEWAY" in {
        when(mockAgentRegistrationService.isAgentEnrolmentAllowed(Matchers.eq(service))).thenReturn(true)
        continueWithAAuthAgent("ATED") {
          result =>
            val thrown = the[RuntimeException] thrownBy redirectLocation(result).get
            thrown.getMessage must include("We could not find your details. Check and try again.")
        }
      }

      "respond with NotFound when invalid service is in uri" in {
        when(mockAgentRegistrationService.isAgentEnrolmentAllowed(Matchers.eq("unknownServiceTest"))).thenReturn(false)
        continueWithAuthorisedUser("unknownServiceTest") {
          result =>
            status(result) must be (NOT_FOUND)
        }
      }
    }
  }

  private def fakeRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  private def continueWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val enrolSuccessResponse = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithDuplicategent(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, Some(badGatewayResponse))))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  private def continueWithAAuthAgent(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockAgentRegistrationService.enrolAgent(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).continue(service).apply(fakeRequestWithSession(userId))
    test(result)
  }

  def businessDetailsWithAuthorisedAgent(reviewDetails: ReviewDetails)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val testDetailsController = testReviewDetailsController(reviewDetails)
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithAuthorisedUser(reviewDetails: ReviewDetails)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val testDetailsController = testReviewDetailsController(reviewDetails)
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithAuthorisedUserNotFound(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val testDetailsController = testReviewDetailsControllerNotFound
    val result = testDetailsController.businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
    testDetailsController
  }

  def businessDetailsWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = testReviewDetailsController(nonDirectMatchReviewDetails).businessDetails(service).apply(fakeRequestWithSession(userId))

    test(result)
  }

}
