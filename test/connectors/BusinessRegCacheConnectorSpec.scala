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
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class BusinessRegCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar  with BeforeAndAfterEach with Injecting {

  val mockSessionCache = mock[SessionCache]

  case class FormData(name: String)

  object FormData {
    implicit val formats = Json.format[FormData]
  }

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData = FormData("some-data")

  val formDataJson = Json.toJson(formData)

  val cacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  override def beforeEach(): Unit = {
    reset(mockSessionCache)
  }

  val appConfig = inject[ApplicationConfig]
  implicit val mcc = inject[MessagesControllerComponents]

  val mockHttpClient = mock[DefaultHttpClient]

  object TestDataCacheConnector extends BusinessRegCacheConnector(
    mockHttpClient,
    appConfig
  ) {
    override val sourceId: String = "BC_NonUK_Business_Details"
  }

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test-sessionid")))

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "return Some" when {
        "formId of the cached form does exist for defined data type" in {
          when(mockHttpClient.GET[CacheMap](any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

          await(TestDataCacheConnector.fetchAndGetCachedDetails[FormData](formIdNotExist)) must be(Some(formData))
        }
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in {
        when(mockHttpClient.PUT[FormData, CacheMap]
          (any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

        await(TestDataCacheConnector.cacheDetails[FormData](formId, formData)) must be(formData)
      }
    }
  }
}
