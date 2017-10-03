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

import java.util.UUID

import builders.{AuthBuilder, TestAudit}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.audit.model.Audit

import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp: CoreGet with CorePost = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  object TestBusinessCustomerConnector extends BusinessCustomerConnector {
    override val http: CoreGet with CorePost = mockWSHttp
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
    override val serviceUrl = ""
    override val baseUri = "business-customer"
    override val registerUri = "register"
    override val updateRegistrationDetailsURI = "update"
    override val knownFactsUri = "known-facts"
  }

  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val service = "ATED"

  "BusinessCustomerConnector" must {
    val businessOrgData = EtmpOrganisation(organisationName = "testName")
    val etmpIdentification = EtmpIdentification(idNumber = "id1", issuingInstitution = "HRMC", issuingCountryCode = "UK")
    val businessAddress = EtmpAddress("line1", "line2", None, None, Some("AA1 1AA"), "GB")
    val nonUkBusinessAddress = EtmpAddress("line1", "line2", None, None, None, "FR")


    "addKnownFacts" must {
      "for successful knownFacts, return Response as HttpResponse" in {
        val knownFacts = KnownFactsForService(List(KnownFact("type", "value")))
        val successResponse = Json.toJson(knownFacts)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.addKnownFacts(knownFacts)
        await(result).status must be(OK)
        await(result).json must be(successResponse)
      }

      "for knownfacts Internal Server error, allow this through" in {
        val knownFacts = KnownFactsForService(List(KnownFact("type", "value")))
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.addKnownFacts(knownFacts)
        await(result).status must be(INTERNAL_SERVER_ERROR)
        await(result).json must be(matchFailureResponse)
      }
    }


    "register" must {
      val businessRequestData = BusinessRegistrationRequest(
        acknowledgementReference = "SESS:123123123",
        organisation = businessOrgData,
        address = businessAddress,
        isAnAgent = false,
        isAGroup = false,
        identification = Some(etmpIdentification),
        contactDetails = EtmpContactDetails()
      )

      val businessRequestDataNonUK = BusinessRegistrationRequest(
        acknowledgementReference = "SESS:123123123",
        organisation = businessOrgData,
        address = nonUkBusinessAddress,
        isAnAgent = false,
        isAGroup = false,
        identification = Some(etmpIdentification),
        contactDetails = EtmpContactDetails()
      )

      "for successful save, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestData, service)
        await(result) must be(businessResponseData)
      }

      "for successful save with non-uk address, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestDataNonUK, service)
        await(result) must be(businessResponseData)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestDataNonUK, service, isNonUKClientRegisteredByAgent = true)
        await(result) must be(businessResponseData)
      }

      "for Service Unavailable, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service unavailable")
      }

      "for Not Found, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for Unknown Error, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(999, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown response status: 999")
      }
    }

    "update" must {
      val updateRequestData = UpdateRegistrationDetailsRequest(
        acknowledgementReference = "SESS:123123123",
        isAnIndividual = false,
        individual = None,
        organisation = Some(businessOrgData),
        address = businessAddress,
        contactDetails = EtmpContactDetails(),
        isAnAgent = false,
        isAGroup = false,
        identification = Some(etmpIdentification)
      )

      val updateRequestDataNonUk = UpdateRegistrationDetailsRequest(
        acknowledgementReference = "SESS:123123123",
        isAnIndividual = false,
        individual = None,
        organisation = Some(businessOrgData),
        address = nonUkBusinessAddress,
        isAnAgent = false,
        isAGroup = false,
        identification = Some(etmpIdentification),
        contactDetails = EtmpContactDetails()
      )

      val safeId = "SAFE123123"
      val successResponse = HttpResponse(OK, Some(Json.parse( """{"processingDate": "2014-12-17T09:30:47Z"}""")))
      "for successful save, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successResponse))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        await(result) must be(successResponse)
      }

      "for successful save with non-uk address, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successResponse))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(successResponse))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for Service Unavailable, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service unavailable")
      }

      "for Not Found, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for Unknown Error, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[BusinessRegistration, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(999, Some(matchFailureResponse))))

        val result = TestBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown response status: 999")
      }
    }
  }

}
