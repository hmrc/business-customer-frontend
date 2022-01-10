/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{MatchBusinessData, StandardAuthRetrievals}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import java.util.UUID
import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockHttp: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAudit: DefaultAuditConnector = mock[DefaultAuditConnector]
  val mockAuditable: Auditable = mock[Auditable]
  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  implicit val user: StandardAuthRetrievals = AuthBuilder.createUserAuthContext("userId", "userName")

  class Setup {
    val connector: BusinessMatchingConnector = new BusinessMatchingConnector (
      mockAuditable,
      mockHttp,
      mockAppConfig
    )
  }

  object TestBusinessMatchingConnector extends BusinessMatchingConnector(
    mockAuditable,
    mockHttp,
    mockAppConfig
  ) {
    override val lookupUri = "lookupUri"
    override val baseUri = "baseUri"
  }

  val userType = "sa"
  val service = "ATED"

  override def beforeEach: Unit = {
    reset(mockHttp)
  }

  "BusinessMatchingConnector" must {

    val matchSuccessResponse = Json.parse(
      """
        |{
        |  "businessName":"ACME",
        |  "businessType":"Unincorporated body",
        |  "businessAddress":"address line 1\naddress line 2\naddress line 3\naddress line 4\nAA1 1AA",
        |  "businessTelephone":"201234567890",
        |  "businessEmail":"contact@acme.com",
        |  "address": {
        |    "addressLine1" : "XYZ  ESTATE",
        |    "addressLine2" : "XYZ DRIVE",
        |    "addressLine3" : "XYZ",
        |    "postalCode" : "AA2 2AA",
        |    "countryCode" : "GB"
        |  }
        |}
      """.stripMargin)

    val matchSuccessResponseInvalidJson =
      """{
        |  "sapNumber" : "111111111111",
        |  "safeId" : "XV111111111111",
        |  "isEditable" : false,
        |  "isAnAgent" : false,
        |  "isAnIndividual" : false,
        |  "organisation": {
        |    "organisationName" : "XYZ BREWERY CO LTD",
        |    "isAGroup" : false
        |  },
        |  "address": {
        |    "addressLine1" : "XYZ  ESTATE",
        |    "addressLine2" : "XYZ DRIVE",
        |    "addressLine3" : "XYZ",
        |    "addressLine4" : "XYZ",
        |    "postalCode" : "AA2 2AA",
        |    "countryCode" : "GB"
        |  },
        |  "contactDetails": {
        |    ,"mobileNumber" : "0121 812222"
        |  }
        |}
      """.stripMargin

    val matchSuccessResponseStripContactDetailsJson =
      """{
        |  "sapNumber" : "111111111111",
        |  "safeId" : "XV111111111111",
        |  "isEditable" : false,
        |  "isAnAgent" : false,
        |  "isAnIndividual" : false,
        |  "organisation": {
        |    "organisationName" : "XYZ BREWERY CO LTD",
        |    "isAGroup" : false
        |  },
        |  "address": {
        |    "addressLine1" : "XYZ  ESTATE",
        |    "addressLine2" : "XYZ DRIVE",
        |    "addressLine3" : "XYZ",
        |    "addressLine4" : "XYZ",
        |    "postalCode" : "AA2 2AA",
        |    "countryCode" : "GB"
        |  }
        |}
      """.stripMargin.replaceAll("[\r\n\t]", "")

    val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

    "for a successful match, return business details" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, matchSuccessResponse.toString)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchSuccessResponse)
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "for a successful match with invalid JSON response, truncate contact details and return valid json" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val responseJson = HttpResponse(OK, matchSuccessResponseInvalidJson.toString)
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(responseJson))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(Json.parse(matchSuccessResponseStripContactDetailsJson))
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "for unsuccessful match, return error message" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, matchFailureResponse.toString)))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchFailureResponse)
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw service unavailable exception, if service is unavailable" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage must include("Service unavailable")
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw bad request exception, if bad request is passed" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw internal server error, if Internal server error status is returned" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Internal server error")
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    "throw runtime exception, unknown status is returned" in {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockHttp.POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, "")))
      val result = TestBusinessMatchingConnector.lookup(matchBusinessData, userType, service)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Unknown response")
      verify(mockHttp, times(1)).POST[MatchBusinessData, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
  }
}
