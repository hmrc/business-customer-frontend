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
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.postcode must be (Some("OX1 4BH"))
          success.businessAddress.country must be ("GB")
        }
      )
    }

    "render businessRegistrationForm correctly with allowed characters in businessName" in {
        val newFormData = formData.updated("businessName", "Acme& '/")

        BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
          formWithErrors => {
            formWithErrors.errors.length must be (0)
          },
          success => {
            success.businessName must be ("Acme& '/")
            success.businessAddress.line_1 must be ("Oxford house")
            success.businessAddress.line_2 must be ("Oxford")
            success.businessAddress.postcode must be (Some("OX1 4BH"))
            success.businessAddress.country must be ("GB")
          }
        )
    }

   "should throw error if businessName is empty" in {
      val newFormData = formData.updated("businessName", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessName length is less than 105" in {
      val newFormData = formData.updated("businessName", "Acme"*10000)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    // TODO Error message needs to be updated
    "should catch invalid characters in businessName" in {
      val newFormData = formData.updated("businessName", "Acm^*e")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Invalid Message")
          formWithErrors.errors.length must be (1)
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
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house& -,.")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.postcode must be (Some("OX1 4BH"))
          success.businessAddress.country must be ("GB")
        }
      )
    }

    "should throw error if businessAddress.line_1 is empty" in {
      val newFormData = formData.updated("businessAddress.line_1", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter an address into Address line 1")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessAddress.line_1 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_1", "Oxford house"*10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Address line 1 cannot be more than 35 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    // TODO Error message needs to be updated
    "should catch invalid characters in businessAddress.line_1" in {
      val newFormData = formData.updated("businessAddress.line_1", "Oxford hous^*e")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Invalid error message")
          formWithErrors.errors.length must be (1)
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
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford& -,.")
          success.businessAddress.postcode must be (Some("OX1 4BH"))
          success.businessAddress.country must be ("GB")
        }
      )
    }

    "should throw error if businessAddress.line_2 is empty" in {
      val newFormData = formData.updated("businessAddress.line_2", "")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter an address into Address line 2")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that businessAddress.line_2 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_2", "Oxford house"*10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Address line 2 cannot be more than 35 characters")
          formWithErrors.errors.length must be (1)
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
          formWithErrors.errors(0).message must be("Invalid error")
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
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.line_3 must be (Some("Address Line 3& -,."))
          success.businessAddress.postcode must be (Some("OX1 4BH"))
          success.businessAddress.country must be ("GB")
        }
      )
    }

    "should validate that businessAddress.line_3 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_3", "Oxford house"*10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Address line 3 cannot be more than 35 characters")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    // TODO Error message needs to be updated
    "should catch invalid characters in businessAddress.line_3" in {
      val newFormData = formData.updated("businessAddress.line_3", "^%&$%&$^*")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Invalid error message")
          formWithErrors.errors.length must be (1)
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
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.businessAddress.line_1 must be ("Oxford house")
          success.businessAddress.line_2 must be ("Oxford")
          success.businessAddress.line_4 must be (Some("Address Line 4& -,."))
          success.businessAddress.postcode must be (Some("OX1 4BH"))
          success.businessAddress.country must be ("GB")
        }
      )
    }

    "should validate that businessAddress.line_4 length is less than 35" in {
      val newFormData = formData.updated("businessAddress.line_4", "Oxford house"*10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Address line 4 cannot be more than 35 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    // TODO Error message needs to be updated
    "should catch invalid characters in businessAddress.line_4" in {
      val newFormData = formData.updated("businessAddress.line_4", "^%&$%&$^*")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Invalid error message")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "should validate that postcode length is less than 10" in {
      val newFormData = formData.updated("businessAddress.postcode", "XX"*10)

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The postcode cannot be more than 10 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    //TODO Error message needs to be updated
    "should catch invalid characters in postcode" in {
      val newFormData = formData.updated("businessAddress.postcode", "XX9 XX^*8")

      BusinessRegistrationForms.businessRegistrationForm.bind(newFormData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("The postcode is invalid")
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
          formWithErrors.errors(0).message must be ("You must enter a country")
          formWithErrors.errors.length must be (1)
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

    "render businessRegistrationForm correctly with plain test of correct length" in {

    }

    "should throw error if businessAddress.country code is empty" in {

    }
  }

  val formData: Map[String, String] = Map(
    "businessUniqueId" -> "",
    "issuingInstitution" -> "",
    "issuingCountry" -> ""
  )
