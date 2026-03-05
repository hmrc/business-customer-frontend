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

package audit

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext

class AuditableSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  implicit val hc: HeaderCarrier        = HeaderCarrier()
  implicit val ec: ExecutionContext     = scala.concurrent.ExecutionContext.global
  val appName                           = "business-customer-frontend"

  class Setup {
    val mockAuditConnector: DefaultAuditConnector = mock[DefaultAuditConnector]
    val auditable: Auditable = new Auditable(appName, mockAuditConnector)
  }

  "Auditable" must {

    "return an Audit instance" in new Setup {
      val result = auditable.audit
      result mustBe a[uk.gov.hmrc.play.audit.model.Audit]
    }

    "send a data event with default parameters" in new Setup {
      val transactionName = "testTransaction"
      val detail          = Map("key1" -> "value1", "key2" -> "value2")

      auditable.sendDataEvent(transactionName, detail = detail)

      verify(mockAuditConnector, times(1)).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
    }

    "send a data event with custom path and tags" in new Setup {
      val transactionName = "testTransaction"
      val path            = "/test/path"
      val tags            = Map("tagKey" -> "tagValue")
      val detail          = Map("detailKey" -> "detailValue")

      auditable.sendDataEvent(transactionName, path, tags, detail)

      verify(mockAuditConnector, times(1)).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
    }

    "send a data event with empty details" in new Setup {
      val transactionName = "testTransaction"

      auditable.sendDataEvent(transactionName, detail = Map.empty[String, String])

      verify(mockAuditConnector, times(1)).sendEvent(any[DataEvent])(any[HeaderCarrier], any[ExecutionContext])
    }
  }
}
