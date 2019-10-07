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

package controllers

import java.util.UUID

import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{Headers, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future


class ExternalLinkControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val mcc = app.injector.instanceOf[MessagesControllerComponents]

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
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(backLink))
    val result = TestExternalLinkController.backLink(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

}
