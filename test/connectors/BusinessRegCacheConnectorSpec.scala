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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{Json, OFormat}
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegCacheConnectorSpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID()}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  case class FormData(name: String)

  object FormData {
    implicit val formats: OFormat[FormData] = Json.format[FormData]
  }

  val formId: String        = "form-id"
  val formIdNotExist: String = "no-form-id"

  val formData: FormData = FormData("some-data")

  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]

  class Setup extends ConnectorTest {
    val connector: BusinessRegCacheConnector =
      new BusinessRegCacheConnector(mockSessionCacheRepo)
  }

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "return Some(formData) if cached form exists for defined data type" in new Setup {
        when(
          mockSessionCacheRepo.getFromSession[FormData](
            DataKey(ArgumentMatchers.any())
          )(any(), any())
        ).thenReturn(Future.successful(Some(formData)))

        val result = connector.fetchAndGetCachedDetails[FormData](formIdNotExist)
        await(result) mustBe Some(formData)
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in new Setup {
        when(
          mockSessionCacheRepo.putSession[FormData](
            DataKey(ArgumentMatchers.any()),
            ArgumentMatchers.eq(formData)
          )(any(), any(), any())
        ).thenReturn(Future.successful(formData))

        val result = connector.cacheDetails[FormData](formId, formData)
        await(result) mustBe formData
      }
    }
  }
}
