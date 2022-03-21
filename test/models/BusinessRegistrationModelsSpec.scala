/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

class BusinessRegistrationModelsSpec extends PlaySpec {

  val fullAddress: Address = Address("line1", "line2", Some("line3"), Some("line4"), Some("AB1 2CD"), "GB")
  val addressJson: JsObject = Json.obj(
    "line_1" -> "line1",
  "line_2" -> "line2",
  "line_3" -> "line3",
  "line_4" -> "line4",
  "postcode" -> "AB1 2CD",
  "country" -> "GB"
  )
  def jsonCountry(country: String): JsObject = Json.obj(
    "line_1" -> "line1",
    "line_2" -> "line2",
    "line_3" -> "line3",
    "line_4" -> "line4",
    "country" -> country
  )

  "Address model" must {
    "write to json correctly" in {
      Json.toJson[Address](fullAddress) mustBe addressJson
    }

    "read from json correctly" in {
      Json.fromJson[Address](addressJson) mustBe JsSuccess(fullAddress)
    }

    "fail to read json that has an empty country field" in {
      Json.fromJson[Address](jsonCountry("")) must matchPattern { case JsError(_) => }
    }

    "fail to read json that has an invalid country field" in {
      Json.fromJson[Address](jsonCountry("UK")) must matchPattern { case JsError(_) => }
    }
  }

}
