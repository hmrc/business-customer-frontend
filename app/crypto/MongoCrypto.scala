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

import play.api.Logging
import play.api.libs.json.{JsString, JsValue, Json, Writes}
import uk.gov.hmrc.crypto.{Crypted, PlainText}

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class MongoCrypto @Inject() (cryptoProvider: CryptoProvider) extends Logging {

  def encrypt[T: Writes](data: T): JsValue =
    JsString(cryptoProvider.crypto.encrypt(PlainText(Json.stringify(Json.toJson(data)))).value)

  def decrypt(stored: JsValue): JsValue = stored match {
    case JsString(value) => handleJson(stored, value)
    case plain           => plain
  }

  private def handleJson(stored: JsValue, value: String) = {
    Try(Json.parse(cryptoProvider.crypto.decrypt(Crypted(value)).value)) match {
      case Success(json) => json
      case Failure(_) =>
        logger.warn("[MongoCrypto][decrypt] Unable to decrypt cached value using raw value")
        stored
    }
  }

}
