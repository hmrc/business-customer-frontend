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

import models.BackLinkModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheConnectorSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar {

  implicit val hc: HeaderCarrier =
    HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]

  class Setup extends ConnectorTest {
    val connector: BackLinkCacheConnector = new BackLinkCacheConnector(mockSessionCacheRepo)
  }

  "BackLinkCacheConnector" must {

    "fetchAndGetBackLink" must {

      "fetch saved back link from SessionCache" in new Setup {
        val backLinkModel = BackLinkModel(Some("testBackLink"))

        when(
          mockSessionCacheRepo
            .getFromSession[BackLinkModel](
              DataKey(ArgumentMatchers.any())
            )(any(), any())
        ).thenReturn(Future.successful(Some(backLinkModel)))

        val result = connector.fetchAndGetBackLink("testPageId")
        await(result) mustBe backLinkModel.backLink
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the back link to SessionCache" in new Setup {
        val backLinkModel = BackLinkModel(Some("testBackLink"))

        when(
          mockSessionCacheRepo
            .putSession[BackLinkModel](
              DataKey(ArgumentMatchers.any()),
              any[BackLinkModel]
            )(any(), any(), any())
        ).thenReturn(Future.successful(backLinkModel))

        val result = connector.saveBackLink("testPageId", backLinkModel.backLink)
        await(result) mustBe backLinkModel.backLink
      }
    }
  }
}
