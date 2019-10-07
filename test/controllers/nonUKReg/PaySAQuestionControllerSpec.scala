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

package controllers.nonUKReg

import java.util.UUID

import builders.SessionBuilder
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.BusinessVerificationController
import javax.inject.Provider
import models.PaySAQuestion
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future


class PaySAQuestionControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockBusinessRegController = mock[BusinessRegController]
  val mockBusinessVerificationControllerProv = mock[Provider[BusinessVerificationController]]
  val mockBusinessVerificationController = mock[BusinessVerificationController]
  val service = "amls"
  val invalidService = "scooby-doo"
  val mockBusinessRegistrationCache = mock[BusinessRegCacheConnector]

  val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val mcc = app.injector.instanceOf[MessagesControllerComponents]

  object TestPaySaQuestionController extends PaySAQuestionController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    mockBusinessRegController,
    mcc,
    mockBusinessVerificationControllerProv,
    mockBusinessRegistrationCache
  ) {
    override val controllerId = "test"
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBackLinkCache)
  }

  "PaySAQuestionController" must {

    "view" must {

      "respond with NotFound when invalid service is in uri" in {
        viewWithAuthorisedClient(invalidService) { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "redirect present user to Pay SA question page, if user is not an agent" in {
        viewWithAuthorisedClient(service) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Do you pay tax in the UK through Self Assessment? - GOV.UK")
          document.select(".block-label").text() must include("Yes")
          document.select(".block-label").text() must include("No")
          document.getElementById("submit").text() must be("Continue")
        }
      }
      "redirect present user to Pay SA question page, if user is not an agent and showed cached data if we have some" in {
        viewWithAuthorisedClientWistSavedData(service) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title must be("Do you pay tax in the UK through Self Assessment? - GOV.UK")
          document.select(".block-label").text() must include("Yes")
          document.select(".block-label").text() must include("No")
          document.getElementById("paySA-false").attr("checked") must be("checked")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "redirect to register non-uk page, if user is an agent" in {
        viewWithAuthorisedAgent(service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/register/$service/NUK"))
        }
      }
    }

    "continue" must {

      "respond with NotFound when invalid service is in uri" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paySA": ""}"""))
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedClient(fakeRequest, invalidService) { result =>
          status(result) must be(NOT_FOUND)
        }
      }

      "if user doesn't select any radio button, show form error with bad_request" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paySA": ""}"""))
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
      "if user select 'yes', redirect it to business verification page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paySA": "true"}"""))
        when(mockBusinessVerificationControllerProv.get())
            .thenReturn(mockBusinessVerificationController)
        when(mockBusinessVerificationController.controllerId)
            .thenReturn("test")
        when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/business-verification/$service/businessForm/NRL"))

        }
      }
      "if user select 'no', redirect it to business registration page" in {
        val fakeRequest = FakeRequest().withJsonBody(Json.parse("""{"paySA": "false"}"""))
        continueWithAuthorisedClient(fakeRequest, service) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/register/$service/NUK"))

        }
      }

    }
  }


  def viewWithAuthorisedAgent(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPaySaQuestionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def viewWithAuthorisedClient(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPaySaQuestionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }

  def viewWithAuthorisedClientWistSavedData(serviceName: String)(test: Future[Result] => Any) = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    val successModel = PaySAQuestion(Some(false))

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[PaySAQuestion](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successModel)))
    val result = TestPaySaQuestionController.view(serviceName).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))
    test(result)
  }


  def continueWithAuthorisedClient(fakeRequest: FakeRequest[AnyContentAsJson], serviceName: String)(test: Future[Result] => Any) = {
    val userId = s"user-${UUID.randomUUID}"
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestPaySaQuestionController.continue(serviceName).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }

}
