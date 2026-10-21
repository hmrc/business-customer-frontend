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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers._
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegCacheConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  case class FormData(name: String)

  object FormData {
    implicit val formats: OFormat[FormData] = Json.format[FormData]
  }

  val formId = "form-id"
  val formData: FormData = FormData("some-data")

  class Setup {
    val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    val connector: BusinessRegCacheConnector = new BusinessRegCacheConnector(mockSessionCacheRepository)
  }

  "BusinessRegCacheConnector" must {

    "fetchAndGetCachedDetails" must {
      "return the cached form data for the form id" in new Setup {
        when(mockSessionCacheRepository.getFromSession[FormData](DataKey[FormData](eqTo(formId)))(any(), any()))
          .thenReturn(Future.successful(Some(formData)))

        await(connector.fetchAndGetCachedDetails[FormData](formId)) must be(Some(formData))
      }

      "return None when nothing is cached for the form id" in new Setup {
        when(mockSessionCacheRepository.getFromSession[FormData](DataKey[FormData](eqTo(formId)))(any(), any()))
          .thenReturn(Future.successful(None))

        await(connector.fetchAndGetCachedDetails[FormData](formId)) must be(None)
      }
    }

    "cacheDetails" must {
      "save the form data under the form id" in new Setup {
        when(mockSessionCacheRepository.putSession[FormData](DataKey[FormData](eqTo(formId)), eqTo(formData))(any(), any()))
          .thenReturn(Future.successful(formData))

        await(connector.cacheDetails[FormData](formId, formData)) must be(formData)
      }
    }
  }
}
