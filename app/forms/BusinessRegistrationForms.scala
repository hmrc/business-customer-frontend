/*
 * Copyright 2024 HM Revenue & Customs
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

import config.ApplicationConfig
import models._
import play.api.data.Form
import play.api.data.Forms._

object BusinessRegistrationForms {

  val postcodeLength = 10
  val length40 = 40
  val length35 = 35
  val length0 = 0
  val length2 = 2
  val length60 = 60
  val length105 = 105
  val postcodeRegex: String =
    """(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|
      |(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9]
      |[abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$""".stripMargin

  val NonUkPostCodeRegex = "^[a-zA-Z0-9]{1,10}+(?: [a-zA-Z0-9]{2,10})?$"
  val countryUK = "GB"

  val businessRegistrationForm: Form[BusinessRegistration] = Form(
    mapping(
      "businessName" -> text.
        verifying("bc.business-registration-error.businessName", _.trim.length > length0)
        .verifying("bc.business-registration-error.businessName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
      "businessAddress" -> mapping(
        "line_1" -> text.
          verifying("bc.business-registration-error.line_1", _.trim.length > length0)
          .verifying("bc.business-registration-error.line_1.length", x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "line_2" -> text.
          verifying("bc.business-registration-error.line_2", _.trim.length > length0)
          .verifying("bc.business-registration-error.line_2.length", x => x.isEmpty || (x.nonEmpty && x.length <= length35)),
        "line_3" -> optional(text)
          .verifying("bc.business-registration-error.line_3.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "line_4" -> optional(text)
          .verifying("bc.business-registration-error.line_4.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length35)),
        "postcode" -> optional(text)
          .verifying("bc.business-registration-error.postcode.length",
            x => x.isEmpty || (x.nonEmpty && x.get.length <= postcodeLength)),
        "country" -> text.
          verifying("bc.business-registration-error.country", _.length > length0)

      )(Address.apply)(Address.unapply)
    )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

  val overseasCompanyForm: Form[OverseasCompany] = Form(
    mapping(
      "hasBusinessUniqueId" -> optional(boolean).verifying("bc.business-registration-error.hasBusinessUniqueId.not-selected", x => x.isDefined),
      "businessUniqueId" -> optional(text)
        .verifying("bc.business-registration-error.businessUniqueId.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length60)),
      "issuingInstitution" -> optional(text)
        .verifying("bc.business-registration-error.issuingInstitution.length", x => x.isEmpty || (x.nonEmpty && x.get.length <= length40)),
      "issuingCountry" -> optional(text)
    )(OverseasCompany.apply)(OverseasCompany.unapply)
  )

  def checkFieldLengthIfPopulated(optionValue: Option[String], fieldLength: Int): Boolean = {
    optionValue match {
      case Some(value) => value.isEmpty || (value.nonEmpty && value.length <= fieldLength)
      case None => true
    }
  }

  def validateNonUK(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiers(registrationData)
  }

  def validateUK(registrationData: Form[BusinessRegistration]): Form[BusinessRegistration] = {
    validatePostCode(registrationData)
  }

  def validateNonUkIdentifiers(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    validateNonUkIdentifiersInstitution(validateNonUkIdentifiersCountry(validateNonUkIdentifiersId(registrationData)))
  }

  def validateNonUkIdentifiersInstitution(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingInstitution = registrationData.data.get("issuingInstitution") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingInstitution.isEmpty =>
        registrationData.withError(key = "issuingInstitution", message = "bc.business-registration-error.issuingInstitution.select")
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersCountry(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val issuingCountry = registrationData.data.get("issuingCountry") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if issuingCountry.isEmpty =>
        registrationData.withError(key = "issuingCountry", message = "bc.business-registration-error.issuingCountry.select")
      case Some(true) if issuingCountry.isDefined && issuingCountry.fold("")(x => x).matches(countryUK) =>
        registrationData.withError(key = "issuingCountry", message = "bc.business-registration-error.non-uk")
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersId(registrationData: Form[OverseasCompany]): Form[OverseasCompany] = {
    val hasBusinessUniqueId = registrationData.data.get("hasBusinessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    } map {
      _.toBoolean
    }
    val businessUniqueId = registrationData.data.get("businessUniqueId") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    hasBusinessUniqueId match {
      case Some(true) if businessUniqueId.isEmpty =>
        registrationData.withError(key = "businessUniqueId", message = "bc.business-registration-error.businessUniqueId.select")
      case _ => registrationData
    }
  }

  private def validatePostCode(registrationData: Form[BusinessRegistration]): Form[BusinessRegistration] = {
    val postCode = registrationData.data.get("businessAddress.postcode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    if (postCode.isEmpty) {
      registrationData.withError(key = "businessAddress.postcode",
        message = "bc.business-registration-error.postcode")
    } else {
      if (!postCode.fold("")(x => x).matches(postcodeRegex)) {
        registrationData.withError(key = "businessAddress.postcode",
          message = "bc.business-registration-error.postcode.invalid")
      } else {
        registrationData
      }
    }
  }

  def validateCountryNonUKAndPostcode(registrationData: Form[BusinessRegistration],
                                      service: String,
                                      isAgent: Boolean,
                                      appConf: ApplicationConfig): Form[BusinessRegistration] = {

    val trimmedCountry = registrationData.data.get("businessAddress.country") map {
      _.trim
    } filterNot {
      _.isEmpty
    }

    val postCode = registrationData.data.get("businessAddress.postcode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }

    def validateNonUkClientPostCode(service: String): Boolean = appConf.validateNonUkCode(service)

    val validatePostCode = validateNonUkClientPostCode(service) && !isAgent

    val formWithTrimmedCountryPostcode = amendedForm(registrationData, postCode, trimmedCountry)

    val form = if(postCode.isEmpty && validatePostCode) {
      formWithTrimmedCountryPostcode.withError(key = "businessAddress.postcode",
        message = "bc.business-registration-error.postcode")
    } else if(!postCode.fold("")(x => x).matches(NonUkPostCodeRegex) && validatePostCode) {
      formWithTrimmedCountryPostcode.withError(key = "businessAddress.postcode",
        message = "bc.business-registration-error.postcode.invalid")
    } else{
      formWithTrimmedCountryPostcode
    }

    val countryForm = {
      if (trimmedCountry.fold("")(x => x).matches(countryUK)) {
        form.withError(key = "businessAddress.country", message = "bc.business-registration-error.non-uk")
      } else {
        form
      }
    }

    //the following code makes sure the errors are displayed in the same order as the fields appear on the page
    if (trimmedCountry.isEmpty){
      countryForm.copy(errors = form.errors.filterNot(e => e.key == "businessAddress.country"))
        .withError("businessAddress.country", "bc.business-registration-error.country")
    } else{
      countryForm
    }
  }

  def amendedForm(form: Form[BusinessRegistration], postcode: Option[String], country: Option[String]): Form[BusinessRegistration] = {

    (postcode, country) match {
      case(Some(pcode), Some(ctry)) => form.discardingErrors.bind(data = form.data.updated("businessAddress.country", ctry)
        .updated("businessAddress.postcode", pcode))
      case(Some(pcode), None) => form.discardingErrors
        .bind(data = form.data.updated("businessAddress.postcode", pcode))
      case(None, Some(ctry)) => form.discardingErrors
        .bind(data = form.data.updated("businessAddress.country", ctry))
      case _ => form
    }

  }

  val nrlQuestionForm: Form[NRLQuestion] = Form(
    mapping(
      "paysSA" -> optional(boolean).verifying("bc.nrl.paysSA.not-selected.error", a => a.isDefined)
    )(NRLQuestion.apply)(NRLQuestion.unapply)
  )

  val paySAQuestionForm: Form[PaySAQuestion] = Form(
    mapping(
      "paySA" -> optional(boolean).verifying("bc.nonuk.paySA.not-selected.error", a => a.isDefined)
    )(PaySAQuestion.apply)(PaySAQuestion.unapply)
  )

}
