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

package forms

import models.BusinessRegistration
import org.scalatest.matchers.must.Matchers
import play.GuiceTestApp
import play.api.data.Form

class BusinessRegistrationFormsSpec extends GuiceTestApp with Matchers {

  val input: Map[String, String] = Map(
    "businessName"             -> "Business Name",
    "businessAddress.line_1"   -> "Address line 1",
    "businessAddress.line_2"   -> "Address line 2",
    "businessAddress.postcode" -> "1234",
    "businessAddress.country"  -> "IN"
  )

  "BusinessRegistrationForms" must {
    "pass validation when valid NonUK postcode applied" in {

      val testinput = input + ("businessAddress.postcode" -> "677 101")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 0
          },
          success => {
            success.businessAddress.postcode mustBe Some("677 101")
          }
        )
    }

    "pass validation when valid NonUK postcode with leading and trailing spaces applied" in {

      val testinput = input + ("businessAddress.postcode" -> "  677 101 ")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 0
          },
          success => {
            success.businessAddress.postcode mustBe Some("677 101")
          }
        )
    }

    "pass validation when valid NonUK postcode with extra punctuation applied" in {

      val testinput = input + ("businessAddress.postcode" -> "641-026.")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 0
          },
          success => {
            success.businessAddress.postcode mustBe Some("641026")
          }
        )
    }

    "pass validation when valid NonUK postcode and Country containing leading and trailing spaces applied" in {

      val testinput = input ++ Map("businessAddress.postcode" -> "1234567890", "businessAddress.country" -> " IN ")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 0
          },
          success => {
            success.businessAddress.country mustBe "IN"
          }
        )
    }

    "pass validation when NonUK postcode and Country contain leading and trailing spaces applied" in {

      val testinput = input ++ Map("businessAddress.postcode" -> "  111 111 ", "businessAddress.country" -> " IN ")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 0
          },
          success => {
            success.businessAddress.country mustBe "IN"
            success.businessAddress.postcode mustBe Some("111 111")
          }
        )
    }

    "fail validation when invalid NonUK postcode is applied" in {

      val testinput = input + ("businessAddress.postcode" -> "67,7  101")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "bc.business-registration-error.postcode.invalid"
          },
          _ => {
            fail("There is a problem")
          }
        )
    }

    "fail validation when NonUK postcode exceeding permitted length is applied" in {

      val testinput = input + ("businessAddress.postcode" -> "12345678901")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 2
            hasErrors.errors.head.message mustBe "bc.business-registration-error.postcode.length"
            hasErrors.errors(1).message mustBe "bc.business-registration-error.postcode.invalid"
          },
          _ => {
            fail("There is a problem")
          }
        )
    }

    "fail validation when NonUK postcode containing more than one space is applied" in {

      val testinput = input + ("businessAddress.postcode" -> "677  101")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "bc.business-registration-error.postcode.invalid"
          },
          _ => {
            fail("There is a problem")
          }
        )
    }

    "fail validation when NonUK postcode with GB country selected" in {

      val testinput = input ++ Map("businessAddress.postcode" -> "677 101", "businessAddress.country" -> "GB")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "bc.business-registration-error.non-uk"
          },
          _ => {
            fail("There is a problem")
          }
        )
    }

    "fail validation when empty NonUK postcode applied" in {

      val testinput = input + ("businessAddress.postcode" -> "")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "bc.business-registration-error.postcode"
          },
          _ => {
            fail("There is a problem")
          }
        )
    }

    "fail validation where there is no Country supplied yet still trims the postcode on amended form" in {

      val testinput = input ++ Map("businessAddress.postcode" -> " TF1 1TT ", "businessAddress.country" -> "")

      val form: Form[BusinessRegistration] = BusinessRegistrationForms.businessRegistrationForm.bind(testinput)
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(form, "ated", isAgent = false, appConfig)
        .fold(
          hasErrors => {
            hasErrors.errors.length mustBe 1
            hasErrors.errors.head.message mustBe "bc.business-registration-error.country"
          },
          success => {
            success.businessAddress.postcode mustBe Some("TF1 1TT")
          }
        )
    }
  }

}
