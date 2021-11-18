/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.GovernmentGatewayConstants

import java.util.UUID
import scala.concurrent.Future


class TaxEnrolmentsConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {
  val mockMetrics = mock[MetricsService]
  val mockHttpClient = mock[DefaultHttpClient]
  val mockAuditable = mock[Auditable]
  val appConfig = inject[ApplicationConfig]

  val mockContext: Timer.Context = mock[Timer.Context]

  object TestTaxEnrolmentsConnector extends TaxEnrolmentsConnector(
    mockMetrics,
    appConfig,
    mockAuditable,
    mockHttpClient
  ) {
    override val enrolmentUrl: String = ""
  }

  override def beforeEach: Unit = {
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

    "enrol user" must {
      "works for a user" in {
        when(mockMetrics.startTimer(ArgumentMatchers.any()))
            .thenReturn(mockContext)

        when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).
          thenReturn(Future.successful(HttpResponse(CREATED, successfulSubscribeJson.toString)))

        val result = TestTaxEnrolmentsConnector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }

      "return status is anything, for bad data sent for enrol" in {
        when(mockHttpClient.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, subscribeFailureResponseJson.toString)))
        val result = TestTaxEnrolmentsConnector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must not be(CREATED)
      }

    }
  }
}
