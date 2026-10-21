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

package repositories

import com.typesafe.config.ConfigFactory
import config.ApplicationConfig
import crypto.{CryptoProvider, MongoCrypto}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.test.MongoSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SessionCacheRepositorySpec extends PlaySpec with MongoSupport with MockitoSugar with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val appConfig: ApplicationConfig = mock[ApplicationConfig]
  when(appConfig.mongoSessionTtl).thenReturn(15.minutes)

  private val mongoCrypto = new MongoCrypto(new CryptoProvider(
    Configuration(ConfigFactory.parseString("""json.encryption.key = "gvBoGdgzqG1AarzF1LY0zQ=="""")))
  )

  private val repository = new SessionCacheRepository(mongoComponent, new CurrentTimestampSupport, mongoCrypto)

  private val sessionId = "session-12345"
  private val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  private val dataKey = DataKey[JsObject]("BC_Business_Details")
  private val data = Json.obj("businessName" -> "ACME Trading Ltd", "safeId" -> "XE0001234567890")

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  private def rawStoredValue: JsValue = {
    val document = await(mongoComponent.database.getCollection("sessions").find(Filters.equal("_id", sessionId)).first().toFuture())
    (Json.parse(document.toJson()) \ "data" \ dataKey.unwrap).as[JsValue]
  }

  "SessionCacheRepository" must {
    "store data encrypted" in {
      await(repository.putSession(dataKey, data)(implicitly, hc))

      val stored = rawStoredValue
      stored mustBe a[JsString]
      stored.toString must not include "ACME Trading Ltd"
    }

    "read stored data back decrypted" in {
      await(repository.putSession(dataKey, data)(implicitly, hc))

      await(repository.getFromSession(dataKey)(implicitly, hc)) mustBe Some(data)
    }

    "return None when nothing is stored for the key" in {
      await(repository.getFromSession(dataKey)(implicitly, hc)) mustBe None
    }

    "delete all data for the session" in {
      await(repository.putSession(dataKey, data)(implicitly, hc))
      await(repository.deleteFromSession(hc))

      await(repository.getFromSession(dataKey)(implicitly, hc)) mustBe None
    }

    "not store or read data for a request without a session id" in {
      val noSessionHc = HeaderCarrier()

      an[IllegalStateException] must be thrownBy await(repository.putSession(dataKey, data)(implicitly, noSessionHc))
      an[IllegalStateException] must be thrownBy await(repository.getFromSession(dataKey)(implicitly, noSessionHc))
    }
  }
}
