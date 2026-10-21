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

package crypto

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.{JsString, Json}

class MongoCryptoSpec extends PlaySpec {

  private def randomBase64Key(): String = {
    val bytes = new Array[Byte](32)
    new java.security.SecureRandom().nextBytes(bytes)
    java.util.Base64.getEncoder.encodeToString(bytes)
  }

  private def mongoCrypto(key: String = randomBase64Key()): MongoCrypto =
    new MongoCrypto(new CryptoProvider(Configuration(ConfigFactory.parseString(s"""json.encryption.key = "$key""""))))

  private val data = Json.obj("businessName" -> "Test Business Ltd", "postcode" -> "NE1 1AA")

  "MongoCrypto" should {
    "encrypt a value so the plain JSON is not stored" in {
      val encrypted = mongoCrypto().encrypt(data)

      encrypted mustBe a[JsString]
      encrypted.toString must not include "Test Business Ltd"
    }

    "decrypt an encrypted value back to the original JSON" in {
      val crypto = mongoCrypto()

      crypto.decrypt(crypto.encrypt(data)) mustBe data
    }

    "produce different ciphertext each time for the same value (AES-GCM random nonce)" in {
      val crypto = mongoCrypto()

      crypto.encrypt(data) must not equal crypto.encrypt(data)
    }

    "return unencrypted JSON cached before encryption was reinstated as-is" in {
      mongoCrypto().decrypt(data) mustBe data
    }

    "return a string value as-is when it cannot be decrypted" in {
      val encryptedWithOtherKey = mongoCrypto().encrypt(data)

      mongoCrypto().decrypt(encryptedWithOtherKey) mustBe encryptedWithOtherKey
    }
  }
}
