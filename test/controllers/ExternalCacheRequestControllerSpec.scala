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

import builders.{AuthBuilder, SessionBuilder}
import config.ApplicationConfig
import connectors.DataCacheConnector
import models.{Address, ReviewDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.AuthConnector

import java.util.UUID
import scala.concurrent.Future

class ExternalCacheRequestControllerSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with MockitoSugar
    with Injecting
    with BeforeAndAfterEach {

  val service = "awrs"
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  lazy val appConfig: ApplicationConfig = inject[ApplicationConfig]
  lazy val mcc: MessagesControllerComponents =
    inject[MessagesControllerComponents]

  val testReviewBusinessDetails =
    ReviewDetails(
      businessName = "agency",
      businessType = Some("Corporate Body"),
      businessAddress = Address(
        line_1 = "23 High Street",
        line_2 = "Park View",
        line_3 = Some("Gloucester"),
        line_4 = Some("Gloucestershire"),
        postcode = Some("NE98 1ZZ"),
        country = "GB"
      ),
      sapNumber = "1234567890",
      safeId = "XE0001234567890",
      agentReferenceNumber = Some("JARN1234567")
    )

  object TestExternalCacheRequestController
      extends ExternalCacheRequestController(
        appConfig,
        mcc,
        mockAuthConnector,
        mockDataCacheConnector
      )

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheConnector)
  }

  "ExternalCacheRequestController" must {

    "fetchCachedBusinessReview" must {

      "return 200 with cached business review json when details exist" in {
        getWithAuthorisedUser(Some(testReviewBusinessDetails)) { result =>
          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(testReviewBusinessDetails)

          verify(mockDataCacheConnector, times(1))
            .fetchAndGetBusinessDetailsForSession(any(), any())
        }
      }

      "return 404 when no cached business review exists" in {
        getWithAuthorisedUser(None) { result =>
          status(result) mustBe NOT_FOUND

          verify(mockDataCacheConnector, times(1))
            .fetchAndGetBusinessDetailsForSession(any(), any())
        }
      }

      "redirect unauthorised users" in {
        getWithUnAuthorisedUser { result =>
          status(result) mustBe SEE_OTHER
        }
      }

      "redirect unauthorised users to the unauthorised page" in {
        getWithUnAuthorisedUser { result =>
          redirectLocation(result).value must include(
            "/business-customer/unauthorised"
          )
        }
      }
    }
  }

  private def getWithAuthorisedUser(
      reviewDetails: Option[ReviewDetails]
  )(
      test: Future[Result] => Any
  ): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(
      mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(any(), any())
    ).thenReturn(Future.successful(reviewDetails))

    val result =
      TestExternalCacheRequestController
        .fetchCachedBusinessReviewDetails(service)
        .apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

  private def getWithUnAuthorisedUser(
      test: Future[Result] => Any
  ): Unit = {
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result =
      TestExternalCacheRequestController
        .fetchCachedBusinessReviewDetails(service)
        .apply(SessionBuilder.buildRequestWithSession(userId))

    test(result)
  }

}
