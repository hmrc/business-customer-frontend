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

import builders.AuthBuilder
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.{Address, ReviewDetails}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{Headers, MessagesControllerComponents}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse

import java.util.UUID
import scala.concurrent.Future

class BusinessCustomerControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with Injecting {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  override def beforeEach() = {
    reset(mockDataCacheConnector)
    reset(mockAuthConnector)
  }

  val appConfig = inject[ApplicationConfig]
  implicit val mcc = inject[MessagesControllerComponents]

  object TestBusinessCustomerController extends BusinessCustomerController(
    mockAuthConnector,
    appConfig,
    mockDataCacheConnector,
    mcc
  )

  private def fakeRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
  }

  "BusinessCustomerController" must {
    "unauthorised users trying to clear cache" must {
      "respond with a redirect to unauthorized page" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))

        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/unauthorised")
      }
    }

    "authorized users" must {
      "clearCache successfully" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.clearCache(ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(OK, ""))
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))
        status(result) must be(OK)
      }

      "clearCache gives error" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.clearCache(ArgumentMatchers.any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, ""))
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "unauthorised users trying to get business details" must {
      "respond with a redirect to unauthorized page" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))

        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/unauthorised")
      }
    }

    "authorized users" must {
      "getReviewDetails successfully" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

        val address = Address("", "", None, None, None, "")

        val reviewDetails = ReviewDetails(
          "businessName",
          None,
          address,
          "sapNumber",
          "safeId",
          false,
          false,
          None,
          None,
          None,
          None,
          None,
          false
        )

        when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any())) thenReturn Future.successful(Some(reviewDetails))
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))
        status(result) must be(OK)
      }

      "getReviewDetails cannot find details" in {
        val userId = s"user-${UUID.randomUUID}"
        AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(ArgumentMatchers.any())) thenReturn Future.successful(None)
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))
        status(result) must be(NOT_FOUND)
      }
    }
  }
}
