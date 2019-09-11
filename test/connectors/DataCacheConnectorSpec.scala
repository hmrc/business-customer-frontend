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

package connectors

import config.ApplicationConfig
import models.{Address, BackLinkModel, ReviewDetails}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockSessionCache = mock[SessionCache]
  val mockDefaultHttpClient = mock[DefaultHttpClient]

  val appConfig = app.injector.instanceOf[ApplicationConfig]

  object TestDataCacheConnector extends DataCacheConnector(
    mockDefaultHttpClient,
    appConfig
  ) {
    override val sourceId: String = "BC_Business_Details"
  }

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test")))

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {
      "fetch saved BusinessDetails from SessionCache" in {
        val reviewDetails: ReviewDetails =
          ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

        when(mockDefaultHttpClient.GET[CacheMap](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(CacheMap("test", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))))

        val result: Future[Option[ReviewDetails]] = TestDataCacheConnector.fetchAndGetBusinessDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

        when(mockDefaultHttpClient.PUT[ReviewDetails, CacheMap](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(CacheMap("test", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))))

        val result: Future[Option[ReviewDetails]] = TestDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be(reviewDetails)
      }

    }

    "clearCache" must {
      "clear the cache for the session" in {
        when(mockDefaultHttpClient.DELETE[HttpResponse]
          (Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())
        ).thenReturn(Future.successful(HttpResponse(OK)))

        val result: Future[HttpResponse] = TestDataCacheConnector.clearCache
        await(result).status must be(OK)
      }
    }
  }
}
