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
import builders.AuthBuilder
import config.ApplicationConfig
import models._
import org.apache.pekko.actor.setup.Setup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.connectors.ConnectorTest
import uk.gov.hmrc.http._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class NewBusinessCustomerConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with ConnectorTest with BeforeAndAfterEach with Injecting {

  val mockAuditable = mock[Auditable]
  val appConfig = inject[ApplicationConfig]

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  object TestNewBusinessCustomerConnector extends NewBusinessCustomerConnector(
    appConfig,
    mockAuditable,
    mockHttpClient
  ) {
    override val baseUri = "business-customer"
    override val registerUri = "register"
    override val updateRegistrationDetailsURI = "update"
    override val knownFactsUri = "known-facts"
  }

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
  }

  implicit val user: StandardAuthRetrievals = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val service = "ATED"

  "NewBusinessCustomerConnector" must {
    val businessOrgData = EtmpOrganisation(organisationName = "testName")
    val etmpIdentification = EtmpIdentification(idNumber = "id1", issuingInstitution = "HRMC", issuingCountryCode = "UK")
    val businessAddress = EtmpAddress("line1", "line2", None, None, Some("AA1 1AA"), "GB")
    val nonUkBusinessAddress = EtmpAddress("line1", "line2", None, None, None, "FR")

    val knownFacts = Verifiers(List(Verifier("type", "value")))

    "addKnownFacts" must {
      "for successful knownFacts, return Response as HttpResponse" in new Setup {
        val successResponse = Json.toJson(knownFacts)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          //.thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result = TestNewBusinessCustomerConnector.addKnownFacts(knownFacts, "JARN123456")
        await(result).status must be(OK)
        await(result).json must be(successResponse)
      }

      "for knownfacts Internal Server error, allow this through" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.addKnownFacts(knownFacts, "JARN123456")
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
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
         // .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result = TestNewBusinessCustomerConnector.register(businessRequestData, service)
        await(result) must be(businessResponseData)
      }

      "for successful save with non-uk address, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))


        val result = TestNewBusinessCustomerConnector.register(businessRequestDataNonUK, service)
        await(result) must be(businessResponseData)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in {
        val businessResponseData = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse = Json.toJson(businessResponseData)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result = TestNewBusinessCustomerConnector.register(businessRequestDataNonUK, service, isNonUKClientRegisteredByAgent = true)
        await(result) must be(businessResponseData)
      }

      "for Service Unavailable, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service Unavailable")
      }

      "for Not Found, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for Unknown Error, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.register(businessRequestData, service)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown Status 999")
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
      val successResponse = HttpResponse(OK, """{"processingDate": "2014-12-17T09:30:47Z"}""")
      "for successful save, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(successResponse))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(successResponse))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        await(result) must be(successResponse)
      }

      "for successful save with non-uk address, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
       //   .thenReturn(Future.successful(successResponse))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(successResponse))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in {

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(successResponse))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(successResponse))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for Service Unavailable, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service Unavailable")
      }

      "for Not Found, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for Unknown Error, throw an exception" in {
        val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        //when(mockHttpClient.POST[BusinessRegistration, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        val result = TestNewBusinessCustomerConnector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown Status 999")
      }
    }
  }

}
