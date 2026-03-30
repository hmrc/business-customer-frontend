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

import builders.AuthBuilder
import config.ApplicationConfig
import models._
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.DefaultAuditConnector

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BusinessCustomerConnectorSpec extends PlaySpec with GuiceOneServerPerSuite  {

  val service = "ATED"
  implicit val authData: StandardAuthRetrievals = AuthBuilder.createSaUser()

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockAuditConnector: DefaultAuditConnector = app.injector.instanceOf[DefaultAuditConnector]

  class Setup extends ConnectorTest {
    val connector = new BusinessCustomerConnector(
      mockHttpClient,
      mockAuditable,
      mockAppConfig
    )
  }

  "BusinessCustomerConnector" must {
    val businessOrgData = EtmpOrganisation(organisationName = "testName")
    val etmpIdentification = EtmpIdentification(idNumber = "id1", issuingInstitution = "HRMC", issuingCountryCode = "UK")
    val businessAddress = EtmpAddress("line1", "line2", None, None, Some("AA1 1AA"), "GB")
    val nonUkBusinessAddress = EtmpAddress("line1", "line2", None, None, None, "FR")


    "addKnownFacts" must {
      "for successful knownFacts, return Response as HttpResponse" in new Setup {
        val knownFacts: KnownFactsForService = KnownFactsForService(List(KnownFact("type", "value")))
        val successResponse: JsValue = Json.toJson(knownFacts)
        val inputBody: JsValue = Json.toJson(KnownFactsForService(List(KnownFact("type", "value"))))

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[HttpResponse] = connector.addKnownFacts(knownFacts)
        await(result).status must be(OK)
        await(result).json must be(successResponse)
      }

      "for knownfacts Internal Server error, allow this through" in new Setup {
        val knownFacts: KnownFactsForService = KnownFactsForService(List(KnownFact("type", "value")))
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")
        val inputBody: JsValue = Json.toJson(Json.toJson(KnownFactsForService(List(KnownFact("type", "value")))))

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, matchFailureResponse.toString)))

        val result: Future[HttpResponse] = connector.addKnownFacts(knownFacts)
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

      "for successful save, return Response as Json" in new Setup {
        val businessResponseData: BusinessRegistrationResponse = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse: JsValue = Json.toJson(businessResponseData)
        val inputBody: JsValue = Json.toJson(businessRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestData, service)
        await(result) must be(businessResponseData)
      }

      "for successful save with non-uk address, return Response as Json" in new Setup {
        val businessResponseData: BusinessRegistrationResponse = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse: JsValue = Json.toJson(businessResponseData)
        val inputBody: JsValue = Json.toJson(businessRequestDataNonUK)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestDataNonUK, service)
        await(result) must be(businessResponseData)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in new Setup {
        val businessResponseData: BusinessRegistrationResponse = BusinessRegistrationResponse(processingDate = "2015-01-01", sapNumber = "SAP123123", safeId = "SAFE123123",
          agentReferenceNumber = Some("AREF123123"))
        val successResponse: JsValue = Json.toJson(businessResponseData)
        val inputBody: JsValue = Json.toJson(businessRequestDataNonUK)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, successResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestDataNonUK, service, isNonUKClientRegisteredByAgent = true)
        await(result) must be(businessResponseData)
      }

      "for Service Unavailable, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")
        val inputBody: JsValue = Json.toJson(businessRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestData, service)
        val thrown: ServiceUnavailableException = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service unavailable")
      }

      "for Not Found, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = {
          Json.parse("""{"error": "Sorry. Business details not found."}""")
        }

        val inputBody: JsValue = Json.toJson(businessRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestData, service)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("not found")
      }

      "for Unknown Error, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")
        val inputBody: JsValue = Json.toJson(businessRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        val result: Future[BusinessRegistrationResponse] = connector.register(businessRequestData, service)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
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

      "for successful save, return Response as Json" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(successResponse))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestData)
        await(result) must be(successResponse)
      }

      "for successful save with non-uk address, return Response as Json" in new Setup {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestDataNonUk)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(successResponse))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for successful registration of NON-UK based client by an agent, return Response as Json" in new Setup {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestDataNonUk)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(successResponse))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestDataNonUk)
        await(result) must be(successResponse)
      }

      "for Service Unavailable, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, matchFailureResponse.toString)))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown: ServiceUnavailableException = the[ServiceUnavailableException] thrownBy await(result)
        thrown.getMessage must include("Service Unavailable")
      }

      "for Not Found, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Not Found")
      }

      "for Unknown Error, throw an exception" in new Setup {
        val matchFailureResponse: JsValue = Json.parse( """{"error": "Sorry. Business details not found."}""")
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        val inputBody: JsValue = Json.toJson(updateRequestData)

        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(999, matchFailureResponse.toString)))

        val result: Future[HttpResponse] = connector.updateRegistrationDetails(safeId, updateRequestData)
        val thrown: InternalServerException = the[InternalServerException] thrownBy await(result)
        thrown.getMessage must include("Unknown Status 999")
      }
    }
  }
}
