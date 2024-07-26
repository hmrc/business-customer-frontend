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
import com.codahale.metrics.Timer
import config.ApplicationConfig
import metrics.MetricsService
import models._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.connectors.ConnectorTest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import utils.GovernmentGatewayConstants

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


class TaxEnrolmentsConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with ConnectorTest with BeforeAndAfterEach with Injecting {
  val mockMetrics =  mock[MetricsService]
  val mockAuditable =  app.injector.instanceOf[Auditable]
  val appConfig =  app.injector.instanceOf[ApplicationConfig]

  val mockContext: Timer.Context = mock[Timer.Context]

  class Setup {
    val connector: TaxEnrolmentsConnector = new TaxEnrolmentsConnector(
      mockMetrics,
      appConfig,
      mockAuditable,
      mockHttpClient
    )
  }

  override def beforeEach(): Unit = {
    reset(mockMetrics)
    reset(mockHttpClient)
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

        //when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        //  (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
        //  thenReturn(Future.successful(HttpResponse(CREATED, successfulSubscribeJson.toString)))

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(CREATED, successfulSubscribeJson.toString)))


        val result = connector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }

      "return status is anything, for bad data sent for enrol" in new Setup {
       // when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
       //   ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        //  .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString)))
        when(mockMetrics.startTimer(ArgumentMatchers.any()))
          .thenReturn(mockContext)

        when(mockHttpClient.post(any())(any)).thenReturn(requestBuilder)
        when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString)))

        val result = connector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must not be(CREATED)
      }

    }
  }
}
