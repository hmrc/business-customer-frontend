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

package controllers

import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{Headers, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.AuthConnector

import java.util.UUID
import scala.concurrent.Future


class ExternalLinkControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  val appConfig = inject[ApplicationConfig]
  implicit val mcc = inject[MessagesControllerComponents]

  object TestExternalLinkController extends ExternalLinkController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    mcc
  ) {
    override val controllerId = "test"
  }


  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "ExternalLinkController" must {

    "Redirect if we have a value set in Back Link Cache" in {
      backLink(Some("http://backLInk")) { result =>
        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must be(s"http://backLInk")
      }
    }

    "Throw an exception if we have no back link" in {
      backLink(None) { result =>
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must be(s"[ExternalLinkController][backLink] No Back Link found. Service: $service")
      }
    }
  }


  def backLink(backLink: Option[String])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(backLink))
    val result = TestExternalLinkController.backLink(service).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

}
