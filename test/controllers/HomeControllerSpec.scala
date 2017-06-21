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

import builders.{AuthBuilder, SessionBuilder}
import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  val testAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")

  val testReviewDetails = ReviewDetails("ACME", Some("Limited"), testAddress, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

  object TestHomeController extends HomeController {
    override val businessMatchService: BusinessMatchingService = mockBusinessMatchingService
    override val authConnector = mockAuthConnector
    override val controllerId = "test"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBusinessMatchingService)
    reset(mockBackLinkCache)
  }

  "HomeController" must {

    "implement correct Auth connector" in {
      HomeController.authConnector must be(FrontendAuthConnector)
    }

    "implement correct BusinessMatching service" in {
      HomeController.businessMatchService must be(BusinessMatchingService)
    }

    "homePage" must {
      "unauthorised users" must {
        "respond with a redirect" in {
          getWithUnAuthorisedUser { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "be redirected to the login page" in {
          getWithUnAuthorisedUser { result =>
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }
      }

      "Authorised users must" must {
        "if have valid utr" must {
          "if match is found, be redirected to Review Details page" in {
            getWithAuthorisedUserMatched {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/review-details/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false), Matchers.eq(service))(Matchers.any(), Matchers.any())
            }
          }

          "if match is Not found, be redirected to Business verification page" in {
            getWithAuthorisedUserNotMatched {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false), Matchers.eq(service))(Matchers.any(), Matchers.any())
            }
          }
        }
        "if has no UTR, be redirected to Business verification page" in {
          getWithAuthorisedUserNoUTR {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
              verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(Matchers.eq(false), Matchers.eq(service))(Matchers.any(), Matchers.any())
          }
        }
      }
    }

  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithUnAuthenticated(test: Future[Result] => Any) {
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSessionNoUser())
    test(result)
  }

  def getWithAuthorisedUserMatched(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val reviewDetails = Json.toJson(testReviewDetails)
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Some(Future.successful(reviewDetails)))
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNotMatched(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val notFound = Json.parse( """{"Reason" : "Text from reason column"}""")
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Some(Future.successful(notFound)))
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNoUTR(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessMatchingService.matchBusinessWithUTR(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(None)
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
