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

class BusinessVerificationFormsSpec extends PlaySpec with OneAppPerSuite {
  "Soletrader form " should {
      "Validate correct data in all fields " in {
        val formData = Map("firstName"->"Jim", "lastName"->"Last","saUTR"->"1111111111")

        BusinessVerificationForms.soleTraderForm.bind(formData).fold(
          formWithErrors => {
            formWithErrors.errors.length must be (0)
          },
          success => {
            success.firstName must be ("Jim")
            success.lastName must be ("Last")
            success.saUTR must be ("1111111111")
          }
        )
      }

    "Catch incorrect firstName" in {
      val formData = Map("firstName"->"Ji*&m", "lastName"->"Last","saUTR"->"1123456789")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("bc.business-verification-error.firstname.invalid")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty firstName" in {
      val formData = Map("firstName"->"", "lastName"->"Jim","saUTR"->"1123456789")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a first name")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch incorrect lastName" in {
      val formData = Map("firstName"->"Jim", "lastName"->"Ji*&m","saUTR"->"1123456789")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("bc.business-verification-error.surname.invalid")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty lastName" in {
      val formData = Map("firstName"->"Jim", "lastName"->"","saUTR"->"1123456789")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a last name")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch invalid saUTR" in {
      val formData = Map("firstName"->"Jim", "lastName"->"Last","saUTR"->"012345678")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Self Assessment Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty saUTR" in {
      val formData = Map("firstName"->"Jim", "lastName"->"Last","saUTR"->"")
      BusinessVerificationForms.soleTraderForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a Self Assessment Unique Taxpayer Reference")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "businessTypeForm form " should {
    "validate business type" in {

      val formData = Map("businessType" -> "Soletrader", "isSaAccount" -> "true", "isOrgAccount" -> "true")

      BusinessVerificationForms.businessTypeForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(0)
        },
        success = {
          success => {
            success.businessType must be(Some("Soletrader"))
          }
        }
      )
    }

    "Catch empty businessType" in {
      val formData = Map("isSaAccount" -> "true", "isOrgAccount" -> "true")

      BusinessVerificationForms.businessTypeForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be(1)
          formWithErrors.errors(0).message must be ("Please select a type of business")
        },
        success = {
          success => {
            fail("Form should give an error")
          }
        }
      )
    }
  }

  "limitedCompanyForm" should {
    "validate correct limited company" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.limitedCompanyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.cotaxUTR must be ("1111111111")
        }
      )
    }

    "Catch incorrect businessName" in {
      val formData = Map("businessName"->"Acm&^$£e", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.limitedCompanyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty businessName" in {
      val formData = Map("businessName"->"", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.limitedCompanyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a registered company name")
          formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (2)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch incorrect ctUTR" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"0111111")
      BusinessVerificationForms.limitedCompanyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Corporation Tax Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty ctUTR" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"")
      BusinessVerificationForms.limitedCompanyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a Corporation Tax Unique Taxpayer Reference")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "nonResidentLandlordForm" should {
    "validate correct nonResidentLandlord" in {
      val formData = Map("businessName"->"Acme", "saUTR"->"1111111111")
      BusinessVerificationForms.nonResidentLandlordForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.saUTR must be ("1111111111")
        }
      )
    }

    "Catch incorrect businessName" in {
      val formData = Map("businessName"->"Acm&^$£e", "saUTR"->"1111111111")
      BusinessVerificationForms.nonResidentLandlordForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty businessName" in {
        val formData = Map("businessName"->"", "saUTR"->"1111111111")
        BusinessVerificationForms.nonResidentLandlordForm.bind(formData).fold(
          formWithErrors => {
            formWithErrors.errors(0).message must be ("You must enter a registered company name")
            formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
            formWithErrors.errors.length must be (2)
          },
          success => {
            fail("Form should give an error")
          }
        )
    }

    "Catch incorrect saUTR" in {
      val formData = Map("businessName"->"Acme", "saUTR"->"0111111")
      BusinessVerificationForms.nonResidentLandlordForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Self Assessment Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty saUTR" in {
      val formData = Map("businessName"->"Acme", "saUTR"->"")
      BusinessVerificationForms.nonResidentLandlordForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a Self Assessment Unique Taxpayer Reference")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "unincorporatedBodyForm" should {
    "valid correct unincorporatedBody" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.unincorporatedBodyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.cotaxUTR must be ("1111111111")
        }
      )
    }

    "Catch incorrect businessName" in {
      val formData = Map("businessName"->"Acm&^$£e", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.unincorporatedBodyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty businessName" in {
      val formData = Map("businessName"->"", "cotaxUTR"->"1111111111")
      BusinessVerificationForms.unincorporatedBodyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a registered company name")
          formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (2)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch incorrect ctUTR" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"0111111")
      BusinessVerificationForms.unincorporatedBodyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("Corporation Tax Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "Catch empty ctUTR" in {
      val formData = Map("businessName"->"Acme", "cotaxUTR"->"")
      BusinessVerificationForms.unincorporatedBodyForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a Corporation Tax Unique Taxpayer Reference")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "ordinaryBusinessPartnershipForm" should {
    "Validate correct ordinaryBusinessPartnership" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "1111111111")
      BusinessVerificationForms.ordinaryBusinessPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.psaUTR must be ("1111111111")
        }
      )
    }

    "fail with invalid businessName" in {
      val formData = Map("businessName"->"Acm&^$£e", "psaUTR"->"1111111111")
      BusinessVerificationForms.ordinaryBusinessPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with empty businessName" in {
      val formData = Map("businessName"->"", "psaUTR"->"1111111111")
      BusinessVerificationForms.ordinaryBusinessPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a registered company name")
          formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (2)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with invalid psaUTR" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "0111111")
      BusinessVerificationForms.ordinaryBusinessPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Partnership Self Assessment Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with empty psaUTR" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "")
      BusinessVerificationForms.ordinaryBusinessPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("You must enter a Partnership Self Assessment Unique Taxpayer Reference")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "limitedLiabilityPartnershipForm" should {
      "Validate correct limitedLiabilityPartnershipForm" in {
        val formData = Map("businessName" -> "Acme", "psaUTR" -> "1111111111")
        BusinessVerificationForms.limitedLiabilityPartnershipForm.bind(formData).fold(
          formWithErrors => {
            formWithErrors.errors.length must be (0)
          },
          success => {
            success.businessName must be ("Acme")
            success.psaUTR must be ("1111111111")
          }
        )
      }

      "fail with invalid businessName" in {
        val formData = Map("businessName"->"Acm&^$£e", "psaUTR"->"1111111111")
        BusinessVerificationForms.limitedLiabilityPartnershipForm.bind(formData).fold(
          formWithErrors => {
            formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
            formWithErrors.errors.length must be (1)
          },
          success => {
            fail("Form should give an error")
          }
        )
      }

    "fail with empty businessName" in {
      val formData = Map("businessName"->"", "psaUTR"->"1111111111")
      BusinessVerificationForms.limitedLiabilityPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a registered company name")
          formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (2)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

      "fail with invalid psaUTR" in {
        val formData = Map("businessName" -> "Acme", "psaUTR" -> "0111111")
        BusinessVerificationForms.limitedLiabilityPartnershipForm.bind(formData).fold(
          formWithErrors => {
            formWithErrors.errors(0).message must be("Partnership Self Assessment Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
            formWithErrors.errors.length must be(1)
          },
          success => {
            fail("Form should give an error")
          }
        )
      }

    "fail with empty psaUTR" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "")
      BusinessVerificationForms.limitedLiabilityPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("You must enter a Partnership Self Assessment Unique Taxpayer Reference")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

  "limitedPartnershipForm" should {
    "Validate correct limitedPartnershipForm" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "1111111111")
      BusinessVerificationForms.limitedPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors.length must be (0)
        },
        success => {
          success.businessName must be ("Acme")
          success.psaUTR must be ("1111111111")
        }
      )
    }

    "fail with invalid businessName" in {
      val formData = Map("businessName"->"Acm&^$£e", "psaUTR"->"1111111111")
      BusinessVerificationForms.limitedPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with empty businessName" in {
      val formData = Map("businessName"->"", "psaUTR"->"1111111111")
      BusinessVerificationForms.limitedPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be ("You must enter a registered company name")
          formWithErrors.errors(1).message must be ("The registered company name cannot be more than 105 characters")
          formWithErrors.errors.length must be (2)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with invalid psaUTR" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "0111111")
      BusinessVerificationForms.limitedPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("Partnership Self Assessment Unique Taxpayer Reference must be 10 digits. If it is 13 digits only enter the last 10")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }

    "fail with empty psaUTR" in {
      val formData = Map("businessName" -> "Acme", "psaUTR" -> "")
      BusinessVerificationForms.limitedPartnershipForm.bind(formData).fold(
        formWithErrors => {
          formWithErrors.errors(0).message must be("You must enter a Partnership Self Assessment Unique Taxpayer Reference")
          formWithErrors.errors.length must be(1)
        },
        success => {
          fail("Form should give an error")
        }
      )
    }
  }

}
