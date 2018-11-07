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

  "Businessegistration form" should {
    "render businessRegistrationForm correctly with plain test of correct length" in {

      BusinessRegistrationForms.businessRegistrationForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessName must be("Acme")
          success.businessAddress.line_1 must be("Oxford house")
          success.businessAddress.line_2 must be("Oxford")
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessName" in {
      val newFormData = formData.updated("businessName", "Acme& '/")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessName must be("Acme& '/")
          success.businessAddress.line_1 must be("Oxford house")
          success.businessAddress.line_2 must be("Oxford")
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "should throw error if businessName is empty" in {
      val newFormData = formData.updated("businessName", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("You must enter a business name")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessName length is less than 105" in {
      val newFormData = formData.updated("businessName", "Acme" * 10000)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("The business name cannot be more than 105 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should catch invalid characters in businessName" in {
      val newFormData = formData.updated("businessName", "Acm^*e")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Business Name must only include letters a to z, numbers 0 to 9, apostrophes (‘), forward slashes (/) and ampersands (&)")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessAddress.line_1" in {
      val newFormData = formData.updated("businessAddress.line_1", "Oxford house& -,.")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
        },
        success => {
          success.businessName must be("Acme")
          success.businessAddress.line_1 must be("Oxford house& -,.")
          success.businessAddress.line_2 must be("Oxford")
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "should throw error if businessAddress.line_1 is empty" in {
      val newFormData = formData.updated("businessAddress.line_1", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("You must enter an address into Address line 1")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessAddress.line_1 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_1", "Oxford house" * 10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Address line 1 cannot be more than 35 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should catch invalid characters in businessAddress.line_1" in {
      val newFormData = formData.updated("businessAddress.line_1", "Oxford hous^*e")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Address line 1 must only include letters a to z, numbers 0 to 9, commas (,), full stops (.), hyphens (-), spaces, apostrophes (‘) and ampersands (&)")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessAddress.line_2" in {
      val newFormData = formData.updated("businessAddress.line_2", "Oxford& -,.")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessName must be("Acme")
          success.businessAddress.line_1 must be("Oxford house")
          success.businessAddress.line_2 must be("Oxford& -,.")
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "should throw error if businessAddress.line_2 is empty" in {
      val newFormData = formData.updated("businessAddress.line_2", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("You must enter an address into Address line 2")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessAddress.line_2 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_2", "Oxford house" * 10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Address line 2 cannot be more than 35 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    // Error message needs to be updated
    "should catch invalid characters in businessAddress.line_2" in {
      val newFormData = formData.updated("businessAddress.line_2", "Oxfor^*d")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Address line 2 must only include letters a to z, numbers 0 to 9, commas (,), full stops (.), hyphens (-), spaces, apostrophes (‘) and ampersands (&)")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessAddress.line_3" in {
      val newFormData = formData.updated("businessAddress.line_3", "Address Line 3& -,.")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessName must be("Acme")
          success.businessAddress.line_1 must be("Oxford house")
          success.businessAddress.line_2 must be("Oxford")
          success.businessAddress.line_3 must be(Some("Address Line 3& -,."))
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "should validate that businessAddress.line_3 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_3", "Oxford house" * 10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("Address line 3 cannot be more than 35 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should catch invalid characters in businessAddress.line_3" in {
      val newFormData = formData.updated("businessAddress.line_3", "^%&$%&$^*")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("Address line 3 (optional) must only include letters a to z, numbers 0 to 9, commas (,), full stops (.), hyphens (-), spaces, apostrophes (‘) and ampersands (&)")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessAddress.line_4" in {
      val newFormData = formData.updated("businessAddress.line_4", "Address Line 4& -,.")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessName must be("Acme")
          success.businessAddress.line_1 must be("Oxford house")
          success.businessAddress.line_2 must be("Oxford")
          success.businessAddress.line_4 must be(Some("Address Line 4& -,."))
          success.businessAddress.postcode must be(Some("OX1 4BH"))
          success.businessAddress.country must be("GB")
        }
      )
    }

    "should validate that businessAddress.line_4 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_4", "Oxford house" * 10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("Address line 4 cannot be more than 35 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should catch invalid characters in businessAddress.line_4" in {
      val newFormData = formData.updated("businessAddress.line_4", "^%&$%&$^*")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("Address line 4 (optional) must only include letters a to z, numbers 0 to 9, commas (,), full stops (.), hyphens (-), spaces, apostrophes (‘) and ampersands (&)")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that postcode length is less than 10" in {
      val newFormData = formData.updated("businessAddress.postcode", "XX" * 10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("The postcode cannot be more than 10 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should catch invalid characters in postcode" in {
      val newFormData = formData.updated("businessAddress.postcode", "XX9 XX^*8")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("The postcode is invalid")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should throw error if businessAddress.country code is empty" in {
      val newFormData = formData.updated("businessAddress.country", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("You must enter a country")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  val formData: Map[String, String] = Map(
    "businessName" -> "Acme",
    "businessAddress.line_1" -> "Oxford house",
    "businessAddress.line_2" -> "Oxford",
    "businessAddress.postcode" -> "OX1 4BH",
    "businessAddress.country" -> "GB"

  )

  "overseasCompanyForm form" should {

    "render overseasCompanyForm successfully on entering valid input data" in {
      BusinessRegistrationForms.overseasCompanyForm.bind(companyFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success => {
          success.businessUniqueId must be(Some("Unique&-Id"))
          success.hasBusinessUniqueId must be(Some(true))
          success.issuingInstitution must be(Some("institute&'/"))
          success.issuingCountry must be(Some("EE"))
        }
      )
    }

    "throw overseasCompanyForm error on entering invalid input data" in {
      BusinessRegistrationForms.overseasCompanyForm.bind(invalidCompanyFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("The overseas company registration number must only include letters a to z, numbers 0 to 9, ampersands (&), apostrophes (‘) and hyphens (-)")
          formWithErrors.errors(1).message must be("Issuing institution must only include letters a to z, numbers 0 to 9, ampersands (&), apostrophes (‘), forward slashes (/) and hyphens (-)")
          formWithErrors.errors(2).message must be("You cannot select United Kingdom when entering an overseas address")
          formWithErrors.errors.length must be(3)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "throw overseasCompanyForm error on entering input data which exceeds max length" in {
      BusinessRegistrationForms.overseasCompanyForm.bind(maxLengthCompanyFormData).fold(
        formWithErrors => {
          formWithErrors.errors.head.message must be("The overseas company registration number cannot be more than 60 characters")
          formWithErrors.errors(1).message must be("The institution that issued the overseas company registration number cannot be more than 40 characters")
          formWithErrors.errors(2).message must be("You cannot select United Kingdom when entering an overseas address")
          formWithErrors.errors.length must be(3)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  val companyFormData: Map[String, String] = Map(
    "hasBusinessUniqueId" -> "true",
    "businessUniqueId" -> "Unique&-Id",
    "issuingInstitution" -> "institute&'/",
    "issuingCountry" -> "EE"
  )

  val invalidCompanyFormData: Map[String, String] = Map(
    "hasBusinessUniqueId" -> "true",
    "businessUniqueId" -> "Unique&-Id***",
    "issuingInstitution" -> "institute&'/$$",
    "issuingCountry" -> "GB$"
  )

  val maxLengthCompanyFormData: Map[String, String] = Map(
    "hasBusinessUniqueId" -> "true",
    "businessUniqueId" -> "ab"*61,
    "issuingInstitution" -> "abc"*42,
    "issuingCountry" -> "GB"*2
  )
}
