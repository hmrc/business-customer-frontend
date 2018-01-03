/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.DataCacheConnector
import models.{Address, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HttpResponse, SessionKeys }

class BusinessCustomerControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  override def beforeEach = {
    reset(mockDataCacheConnector)
    reset(mockAuthConnector)
  }

  object TestBusinessCustomerController extends BusinessCustomerController {
    override val authConnector = mockAuthConnector
    override val dataCacheConnector = mockDataCacheConnector
  }

  private def fakeRequestWithSession(userId: String) = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
  }

  "BusinessCustomerController" must {
    "unauthorised users trying to clear cache" must {
      "respond with a redirect to unauthorized page" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))

        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/unauthorised")
      }
    }

    "authorized users" must {
      "clearCache successfully" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.clearCache(Matchers.any())) thenReturn Future.successful(HttpResponse(OK))
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))
        status(result) must be(OK)
      }

      "clearCache gives error" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.clearCache(Matchers.any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
        val result = TestBusinessCustomerController.clearCache(service).apply(fakeRequestWithSession(userId))
        status(result) must be(INTERNAL_SERVER_ERROR)
      }
    }

    "unauthorised users trying to get business details" must {
      "respond with a redirect to unauthorized page" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))

        status(result) must be(SEE_OTHER)
        redirectLocation(result).get must include("/business-customer/unauthorised")
      }
    }

    "authorized users" must {
      "getReviewDetails successfully" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

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

        when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())) thenReturn Future.successful(Some(reviewDetails))
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))
        status(result) must be(OK)
      }

      "getReviewDetails cannot find details" in {
        val userId = s"user-${UUID.randomUUID}"
        builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
        when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())) thenReturn Future.successful(None)
        val result = TestBusinessCustomerController.getReviewDetails(service).apply(fakeRequestWithSession(userId))
        status(result) must be(NOT_FOUND)
      }
    }
  }
}
