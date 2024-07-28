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

package connectors

import config.ApplicationConfig
import models.{Address, ReviewDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.connectors.ConnectorTest
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with ConnectorTest with Injecting {

  override implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockSessionCache = mock[SessionCache]
  val mockDefaultHttpClient = mock[HttpClientV2]

  val appConfig = inject[ApplicationConfig]

  object TestDataCacheConnector extends DataCacheConnector(
    mockHttpClient,
    appConfig
  ) {
    override val sourceId: String = "BC_Business_Details"
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {
      "fetch saved BusinessDetails from SessionCache" in {
        val reviewDetails: ReviewDetails =
          ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

          when(mockHttpClient.get(any())(any)).thenReturn(requestBuilder)
          when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))))

          val result: Future[Option[ReviewDetails]] = TestDataCacheConnector.fetchAndGetBusinessDetailsForSession
          await(result) must be(Some(reviewDetails))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in {
        val reviewDetails: ReviewDetails = ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

        when(mockHttpClient.put(any())(any)).thenReturn(requestBuilder)
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("BC_Business_Details" -> Json.toJson(reviewDetails)))))

        val result: Future[Option[ReviewDetails]] = TestDataCacheConnector.saveReviewDetails(reviewDetails)
        await(result).get must be(reviewDetails)
      }

    }

    "clearCache" must {
      "clear the cache for the session" in {
        when(mockHttpClient.delete(any())(any)).thenReturn(requestBuilder)
        when(requestBuilderExecute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, "")))

        val result: Future[Unit] = TestDataCacheConnector.clearCache
        await(result) must be(())
      }
    }
  }
}
