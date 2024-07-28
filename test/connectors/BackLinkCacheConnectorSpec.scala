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

import audit.Auditable
import builders.AuthBuilder
import config.ApplicationConfig
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.connectors.ConnectorTest
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.audit.DefaultAuditConnector

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with ConnectorTest with BeforeAndAfterEach {

  override implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockSessionCache: SessionCache = mock[SessionCache]

  val appConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val connector: BackLinkCacheConnector = new BackLinkCacheConnector(mockHttpClient, appConfig)
  }

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
  }

  "BackLinkCacheConnector" must {
    "fetchAndGetBackLink" must {
      "fetch saved BusinessDetails from SessionCache with Feature Switch on" in new Setup {
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))


        when(mockSessionCache.fetchAndGetEntry[BackLinkModel](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(backLink)))

        when(mockHttpClient.get(any())(any)).thenReturn(requestBuilder)
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("BC_Back_Link:testPageId" -> Json.toJson(BackLinkModel(Some("testBackLink")))))))

        val result = connector.fetchAndGetBackLink("testPageId")
        await(result) must be(backLink.backLink)
      }
    }

    "saveAndReturnBusinessDetails" must {
      "save the fetched business details with Feature Switch on" in new Setup {
        val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))
        val returnedCacheMap: CacheMap = CacheMap("data", Map(connector.sourceId -> Json.toJson(backLink)))

        when(mockSessionCache.cache[ReviewDetails](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(returnedCacheMap))

        when(mockHttpClient.put(any())(any)).thenReturn(requestBuilder)
        when(requestBuilderExecute[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map("BC_Back_Link:testPageId" -> Json.toJson(BackLinkModel(Some("testBackLink")))))))

        val result = connector.saveBackLink("testPageId", backLink.backLink)
        await(result) must be(backLink.backLink)
      }
    }

  }
}
