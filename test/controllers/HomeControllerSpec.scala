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

package controllers

import java.util.UUID

import builders.{AuthBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import javax.inject.Provider
import models.{Address, ReviewDetails}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future


class HomeControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {

  val request = FakeRequest()

  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockBusinessVerificationControllerProv = mock[Provider[BusinessVerificationController]]
  val mockBusinessVerificationController = mock[BusinessVerificationController]
  val mockReviewDetailsController = mock[ReviewDetailsController]
  val mockBusinessRegCacheConnector: BusinessRegCacheConnector = mock[BusinessRegCacheConnector]

  val testAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "GB")
  val testAddressNoCountry = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "")

  val testReviewDetails = (address: Address) => ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

  val appConfig = inject[ApplicationConfig]
  implicit val mcc = inject[MessagesControllerComponents]

  object TestHomeController extends HomeController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    mockBusinessMatchingService,
    mockBusinessVerificationControllerProv,
    mockReviewDetailsController,
    mockBusinessRegCacheConnector,
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
            getWithAuthorisedUserMatched(testAddress) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/review-details/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(ArgumentMatchers.eq(false), ArgumentMatchers.eq(service))(ArgumentMatchers.any(), ArgumentMatchers.any())
            }
          }

          "if match is found but has no country code, set updateNotRegister flag and redirect to business verification" in {
            when(mockBusinessVerificationControllerProv.get())
              .thenReturn(mockBusinessVerificationController)
            when(mockBusinessVerificationController.controllerId)
              .thenReturn("test")
            getWithAuthorisedUserMatched(testAddressNoCountry) {
              result =>
                status(result) must be(SEE_OTHER)
                redirectLocation(result).get must include(s"/business-customer/business-verification/$service")
                verify(mockBusinessMatchingService, times(1)).matchBusinessWithUTR(ArgumentMatchers.eq(false), ArgumentMatchers.eq(service))(ArgumentMatchers.any(), ArgumentMatchers.any())
                verify(mockBusinessRegCacheConnector, times(1)).cacheDetails(ArgumentMatchers.eq("Update_No_Register"), ArgumentMatchers.eq(true))(ArgumentMatchers.any(), ArgumentMatchers.any())
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

  def getWithAuthorisedUserMatched(address: Address)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val reviewDetails = Json.toJson(testReviewDetails(address))
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
