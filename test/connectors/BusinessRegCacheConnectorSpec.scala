/*
 * Copyright 2017 HM Revenue & Customs
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

import config.BusinessCustomerSessionCache
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class BusinessRegCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar  with BeforeAndAfterEach{

  val mockSessionCache = mock[SessionCache]

  case class FormData(name: String)

  object FormData {
    implicit val formats = Json.format[FormData]
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData = FormData("some-data")

  val formDataJson = Json.toJson(formData)

  val cacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  override def beforeEach: Unit = {
    reset(mockSessionCache)
  }

  object TestDataCacheConnector extends BusinessRegCacheConnector {
    override val sessionCache: SessionCache = mockSessionCache
    override val sourceId: String = ""
  }

  "BusinessRegCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "use the correct session cache" in {
        DataCacheConnector.sessionCache must be(BusinessCustomerSessionCache)
      }

      "return Some" when {
        "formId of the cached form does exist for defined data type" in {

          when(mockSessionCache.fetchAndGetEntry[FormData](key = Matchers.eq(formIdNotExist))(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
            Future.successful(Some(formData))
          }
          await(TestDataCacheConnector.fetchAndGetCachedDetails[FormData](formIdNotExist)) must be(Some(formData))
        }
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in {
        when(mockSessionCache.cache[FormData](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(cacheMap)
        }
        await(TestDataCacheConnector.cacheDetails[FormData](formId, formData)) must be(formData)
      }
    }
  }
}
