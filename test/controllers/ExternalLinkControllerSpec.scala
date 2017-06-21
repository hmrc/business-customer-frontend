/*
 * Copyright 2017 HM Revenue & Customs
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

import config.BusinessCustomerSessionCache
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import models.{Address, EnrolResponse, Identifier, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, SessionKeys}

import scala.concurrent.Future


class ExternalLinkControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  object TestExternalLinkController extends ExternalLinkController {
    override val controllerId = "test"
    override val authConnector = mockAuthConnector
    override val backLinkCacheConnector = mockBackLinkCache
  }


  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "ExternalLinkController" must {

    "use the correctbackLinkCacheConnector" in {
      controllers.ExternalLinkController.backLinkCacheConnector must be(BackLinkCacheConnector)
    }

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
      SessionKeys.userId -> userId))

    test(result)
  }

}
