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

import models.BusinessRegistration
import org.scalatest.MustMatchers
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.data.Form

class BusinessRegistrationFormsSpec extends PlaySpec with MustMatchers with OneServerPerSuite {

  "BusinessRegistrationForms" must {
    "pass validation for when valid non uk post code" in {

      val input: Map[String, String] = Map("businessName" -> "Business Name",
      "businessAddress.line_1" -> "Address line 1",
      "businessAddress.line_2" -> "Address line 2",
      "businessAddress.postcode" -> "677 101",
      "businessAddress.country" -> "IN")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(input)
      BusinessRegistrationForms.validateCountryNonUKAndPostcode(form, "ated", false).fold(
        hasErrors => {
          hasErrors.errors.length mustBe 0
        },
          success => {
            success.businessAddress.postcode mustBe Some("677 101")
          }
      )
    }

    "fail validation for invalid non uk post code" in {

      val input: Map[String, String] = Map("businessName" -> "Business Name",
        "businessAddress.line_1" -> "Address line 1",
        "businessAddress.line_2" -> "Address line 2",
        "businessAddress.postcode" -> "67,7  101",
        "businessAddress.country" -> "IN")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(input)
      BusinessRegistrationForms.validateCountryNonUKAndPostcode(form, "ated", false).fold(
        hasErrors => {
          hasErrors.errors.length mustBe 1
          hasErrors.errors.head.message mustBe "The postcode is invalid"
        },
        _ => {
          fail("There is a problem")
        }
      )
    }

    "fail validation for empty non uk post code" in {

      val input: Map[String, String] = Map("businessName" -> "Business Name",
        "businessAddress.line_1" -> "Address line 1",
        "businessAddress.line_2" -> "Address line 2",
        "businessAddress.postcode" -> "",
        "businessAddress.country" -> "IN")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(input)
      BusinessRegistrationForms.validateCountryNonUKAndPostcode(form, "ated", false).fold(
        hasErrors => {
          hasErrors.errors.length mustBe 1
          hasErrors.errors.head.message mustBe "You must enter a postcode"
        },
        _ => {
          fail("There is a problem")
        }
      )
    }
  }

}
