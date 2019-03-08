/*
 * Copyright 2019 HM Revenue & Customs
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
import metrics.Metrics
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Mode.Mode
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.audit.model.Audit
import utils.GovernmentGatewayConstants

import scala.concurrent.Future


class TaxEnrolmentsConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp: CoreGet with CorePost = mock[MockedVerbs]

  object TestTaxEnrolmentsConnector extends TaxEnrolmentsConnector {
    override val http: CoreGet with CorePost = mockWSHttp
    override val audit: Audit = new TestAudit
    override val appName: String = "Test"
    override val enrolmentUrl: String = ""
    override def serviceUrl: String = ""
    override val metrics = Metrics
    override protected def mode: Mode = Play.current.mode
    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  lazy val groupId = "group-id"
  lazy val arn = "JARN123456"

  "TaxEnrolmentsConnector" must {
    "use correct metrics" in {
      TaxEnrolmentsConnector.metrics must be(Metrics)
    }
    val request = NewEnrolRequest(userId = "user-id",
      friendlyName = GovernmentGatewayConstants.FriendlyName,
      `type` = GovernmentGatewayConstants.enrolmentType,
      verifiers = List(Verifier("safeId", "1343343")))
    val response = EnrolResponse(serviceName = "ATED", state = "NotYetActivated", identifiers = List(Identifier("ATED", "Ated_Ref_No")))
    val successfulSubscribeJson = Json.toJson(response)
    val subscribeFailureResponseJson = Json.parse( """{"reason" : "Error happened"}""")
    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
    implicit val user = AuthBuilder.createUserAuthContext("User-Id", "name")

    "enrol user" must {
      "works for a user" in {

        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).
          thenReturn(Future.successful(HttpResponse(CREATED, responseJson = Some(successfulSubscribeJson))))

        val result = TestTaxEnrolmentsConnector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must be(CREATED)
      }

      "return status is anything, for bad data sent for enrol" in {
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(subscribeFailureResponseJson))))
        val result = TestTaxEnrolmentsConnector.enrol(request, groupId, arn)
        val enrolResponse = await(result)
        enrolResponse.status must not be(CREATED)
      }

    }
  }
}
