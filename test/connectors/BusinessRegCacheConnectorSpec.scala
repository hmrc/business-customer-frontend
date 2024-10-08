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
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegCacheConnectorSpec extends PlaySpec with GuiceOneServerPerSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  case class FormData(name: String)

  object FormData {
    implicit val formats: OFormat[FormData] = Json.format[FormData]
  }

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData = FormData("some-data")

  val formDataJson = Json.toJson(formData)

  val cacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  val appConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  class Setup extends ConnectorTest {
    val connector: BusinessRegCacheConnector = new BusinessRegCacheConnector(mockHttpClient, appConfig)
  }


  implicit val ec: ExecutionContext = mcc.executionContext

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "return Some formId of the cached form does exist for defined data type" in new Setup  {
          when(executeGet[CacheMap]).thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

          await(connector.fetchAndGetCachedDetails[FormData](formIdNotExist)) must be(Some(formData))
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in new Setup {
        val inputBody: JsValue = Json.toJson(formData)
        when(executePut[CacheMap](inputBody)).thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

        await(connector.cacheDetails[FormData](formId, formData)) must be(formData)
      }
    }
  }
}
