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

import config.ApplicationConfig
import models._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http._
import utils.GovernmentGatewayConstants

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


class TaxEnrolmentsConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with Injecting {
  val appConfig =  app.injector.instanceOf[ApplicationConfig]

  class Setup extends ConnectorTest {
    val connector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector(
      mockMetrics,
      appConfig,
      mockAuditable,
      mockHttpClient
    )
  }

  lazy val groupId = "group-id"
  lazy val arn = "JARN123456"

  "TaxEnrolmentsConnector" must {
    val request = NewEnrolRequest(userId = "user-id",
      friendlyName = GovernmentGatewayConstants.FriendlyName,
      `type` = GovernmentGatewayConstants.enrolmentType,
      verifiers = List(Verifier("safeId", "1343343")))
    val response = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
    val successfulSubscribeJson = Json.toJson(response)
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    "enrol user" must {
      "works for a user" in new Setup {
        when(mockMetrics.startTimer(ArgumentMatchers.any()))
            .thenReturn(mockContext)
        val inputBody: JsValue = Json.toJson(request)
        when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(CREATED, successfulSubscribeJson.toString)))

        val result = connector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }

      "return status is anything, for bad data sent for enrol" in new Setup {
          when(mockMetrics.startTimer(ArgumentMatchers.any()))
            .thenReturn(mockContext)
          val inputBody: JsValue = Json.toJson(request)
          when(executePost[HttpResponse](inputBody)).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString)))

          val result = connector.enrol(request, groupId, arn)
          val enrolResponse = await(result)
          enrolResponse.status must not be(CREATED)
      }

    }
  }
}
