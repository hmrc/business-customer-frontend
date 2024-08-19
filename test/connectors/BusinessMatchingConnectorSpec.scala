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
import models.{MatchBusinessData, StandardAuthRetrievals}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.DefaultAuditConnector

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BusinessMatchingConnectorSpec extends PlaySpec with GuiceOneServerPerSuite {

  val mockAudit: DefaultAuditConnector = app.injector.instanceOf[DefaultAuditConnector]
  val mockAuditable: Auditable = app.injector.instanceOf[Auditable]
  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val user: StandardAuthRetrievals = AuthBuilder.createUserAuthContext("userId", "userName")
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  class Setup extends ConnectorTest {
    val connector: BusinessMatchingConnector = new BusinessMatchingConnector (
      mockAuditable,
      mockHttpClient,
      mockAppConfig
    )
  }

  val userType = "sa"
  val service = "ATED"

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

    "for a successful match, return business details"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, matchSuccessResponse.toString)))

      val result = connector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchSuccessResponse)
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }

    "for a successful match with invalid JSON response, truncate contact details and return valid json"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val responseJson = HttpResponse(OK, matchSuccessResponseInvalidJson.toString)
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(responseJson))

      val result = connector.lookup(matchBusinessData, userType, service)
      await(result) must be(Json.parse(matchSuccessResponseStripContactDetailsJson))
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }

    "for unsuccessful match, return error message"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(OK, matchFailureResponse.toString)))

      val result = connector.lookup(matchBusinessData, userType, service)
      await(result) must be(matchFailureResponse)
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())}

    "throw service unavailable exception, if service is unavailable"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, "")))

      val result = connector.lookup(matchBusinessData, userType, service)
      val thrown = the[ServiceUnavailableException] thrownBy await(result)
      thrown.getMessage must include("Service unavailable")
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }

    "throw bad request exception, if bad request is passed"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

      val result = connector.lookup(matchBusinessData, userType, service)
      val thrown = the[BadRequestException] thrownBy await(result)
      thrown.getMessage must include("Bad Request")
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }

    "throw internal server error, if Internal server error status is returned"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = connector.lookup(matchBusinessData, userType, service)
      val thrown = the[InternalServerException] thrownBy await(result)
      thrown.getMessage must include("Internal server error")
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }

    "throw runtime exception, unknown status is returned"  in new Setup {
      val matchBusinessData = MatchBusinessData("sessionId", "1111111111", false, false, None, None)
      implicit val hc: HeaderCarrier = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val inputBody: JsValue = Json.toJson(matchBusinessData)

      when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(BAD_GATEWAY, "")))

      val result = connector.lookup(matchBusinessData, userType, service)
      val thrown = the[RuntimeException] thrownBy await(result)
      thrown.getMessage must include("Unknown response")
      verify(mockHttpClient, times(1)).post(ArgumentMatchers.any())(ArgumentMatchers.any())
    }
  }
}
