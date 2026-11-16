/*
 * Copyright 2026 HM Revenue & Customs
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

import models.BackLinkModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import repositories.SessionCacheRepository
import services.BackLinkCacheService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheServiceSpec extends PlaySpec with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val backLink: BackLinkModel = BackLinkModel(Some("testBackLink"))

  class Setup {
    val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    val service: BackLinkCacheService = new BackLinkCacheService(mockSessionCacheRepository)
  }

  "BackLinkCacheService" must {
    "fetchAndGetBackLink" must {
      "fetch the saved back link for the page" in new Setup {
        when(mockSessionCacheRepository.getFromSession[BackLinkModel](DataKey[BackLinkModel](eqTo("BC_Back_Link:testPageId")))(any(), any()))
          .thenReturn(Future.successful(Some(backLink)))

        await(service.fetchAndGetBackLink("testPageId")) must be(backLink.backLink)
      }

      "return None when no back link is saved for the page" in new Setup {
        when(mockSessionCacheRepository.getFromSession[BackLinkModel](DataKey[BackLinkModel](eqTo("BC_Back_Link:testPageId")))(any(), any()))
          .thenReturn(Future.successful(None))

        await(service.fetchAndGetBackLink("testPageId")) must be(None)
      }
    }

    "saveBackLink" must {
      "save the back link for the page" in new Setup {
        when(mockSessionCacheRepository.putSession[BackLinkModel](DataKey[BackLinkModel](eqTo("BC_Back_Link:testPageId")), eqTo(backLink))(any(), any()))
          .thenReturn(Future.successful(backLink))

        await(service.saveBackLink("testPageId", backLink.backLink)) must be(backLink.backLink)
      }
    }
  }
}
