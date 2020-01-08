/*
 * Copyright 2020 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import javax.inject.Provider
import models.{Address, ReviewDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockBusinessVerificationControllerProv = mock[Provider[BusinessVerificationController]]
  val mockBusinessVerificationController = mock[BusinessVerificationController]
  val mockReviewDetailsController = mock[ReviewDetailsController]

  val testAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")

  val testReviewDetails = ReviewDetails("ACME", Some("Limited"), testAddress, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

  val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val mcc = app.injector.instanceOf[MessagesControllerComponents]

  object TestHomeController extends HomeController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    mockBusinessMatchingService,
    mockBusinessVerificationControllerProv,
    mockReviewDetailsController,
    mcc
  ) {
    override val controllerId = "test"
  }

  override def beforeEach = {
    reset(mockAuthConnector)
    reset(mockBusinessMatchingService)
    reset(mockBackLinkCache)
  }

  "HomeController" must {

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
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(ArgumentMatchers.eq(false), ArgumentMatchers.eq(service))(ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }

          "if match is Not found, be redirected to Business verification page" in {
            when(mockBusinessVerificationControllerProv.get())
              .thenReturn(mockBusinessVerificationController)
            when(mockBusinessVerificationController.controllerId)
              .thenReturn("test")

            getWithAuthorisedUserNotMatched {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(ArgumentMatchers.eq(false), ArgumentMatchers.eq(service))(ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }
        }
        "if has no UTR, be redirected to Business verification page" in {
          when(mockBusinessVerificationControllerProv.get())
            .thenReturn(mockBusinessVerificationController)
          when(mockBusinessVerificationController.controllerId)
            .thenReturn("test")

          getWithAuthorisedUserNoUTR {
            result =>
              status(result) must be(SEE_OTHER)
              redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
              verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(ArgumentMatchers.eq(false), ArgumentMatchers.eq(service))(ArgumentMatchers.any(), ArgumentMatchers.any())
          }
        }
      }
    }

  }

  def getWithUnAuthorisedUser(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
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
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val reviewDetails = Json.toJson(testReviewDetails)
    when(mockBusinessMatchingService.matchBusinessWithUTR(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Some(Future.successful(reviewDetails)))
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNotMatched(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val notFound = Json.parse( """{"Reason" : "Text from reason column"}""")
    when(mockBusinessMatchingService.matchBusinessWithUTR(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Some(Future.successful(notFound)))
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUserNoUTR(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessMatchingService.matchBusinessWithUTR(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(None)
    val result = TestHomeController.homePage(service, None).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
