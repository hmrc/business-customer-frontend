/*
 * Copyright 2018 HM Revenue & Customs
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

package forms

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import models.{Address, BusinessRegistration}

class BusinessRegistrationFormsSpec extends PlaySpec with OneAppPerSuite {

  "Businessregistration form" should {
    "validate businessRegistrationForm with plain test of correct length" in {

        val formData = Map(
          "businessName" -> "Acme",
          "businessAddress.line_1" -> "Oxford house",
          "businessAddress.line_2" -> "Oxford",
          "businessAddress.postcode" -> "XX9 XX8",
          "businessAddress.country" -> "GB"
        )

      BusinessRegistrationForms.businessRegistrationForm.bind(formData).fold(
        formWithErrors => {
          print(formWithErrors.toString)
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.postcode must be (Some("XX9 XX8"))
          success.businessAddress.country must be ("GB")
        }
      )
    }
    "validate businessRegistrationForm with allowed chars in name" in {

        val formData = Map(
          "businessName" -> "Acme& '/",
          "businessAddress.line_1" -> "Oxford house",
          "businessAddress.line_2" -> "Oxford",
          "businessAddress.postcode" -> "XX9 XX8",
          "businessAddress.country" -> "GB"
        )

      BusinessRegistrationForms.businessRegistrationForm.bind(formData).fold(
        formWithErrors => {
          print(formWithErrors.toString)
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme& '/")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.postcode must be (Some("XX9 XX8"))
          success.businessAddress.country must be ("GB")
        }
      )
    }
    "fail regex for businessName with invalid characters" in {

      val formData = Map(
        "businessName" -> "Acm^*e",
        "businessAddress.line_1" -> "Oxford house",
        "businessAddress.line_2" -> "Oxford",
        "businessAddress.postcode" -> "XX9 XX8",
        "businessAddress.country" -> "GB"
      )

      BusinessRegistrationForms.businessRegistrationForm.bind(formData).fold(
        formWithErrors => {
          print(formWithErrors.toString)
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }
}
