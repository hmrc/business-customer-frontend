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

import config.ApplicationConfig
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.play.mappers.StopOnFirstFail
import uk.gov.hmrc.play.mappers.StopOnFirstFail._
import utils.ValidationConstants

object BusinessRegistrationForms extends ValidationConstants {

  val businessRegistrationForm = Form(
    mapping(
      "businessName" -> text.verifying(StopOnFirstFail(
        constraint[String](Messages("bc.business-registration-error.businessName"), x => x.trim.length > Length0),
        constraint[String](Messages("bc.business-registration-error.businessName.length", Length105), x => x.isEmpty || (x.nonEmpty && x.length <= Length105)),
        constraint[String](Messages("bc.business-registration-error.businessName.invalid"), x => x.trim.matches(BusinessNameRegex)))
      ),
      "businessAddress" -> mapping(
        "line_1" -> text.verifying(StopOnFirstFail(
          constraint[String](Messages("bc.business-registration-error.line_1"), x => x.trim.length > Length0),
          constraint[String](Messages("bc.business-registration-error.line_1.length", Length35), x => x.isEmpty || (x.nonEmpty && x.length <= Length35)),
          constraint[String](Messages("bc.business-registration-error.address.invalid", Messages("bc.business-registration.line_1")),
            x => x.trim.matches(LineRegex)))
        ),
        "line_2" -> text.verifying(StopOnFirstFail(
          constraint[String](Messages("bc.business-registration-error.line_2"), x => x.trim.length > Length0),
          constraint[String](Messages("bc.business-registration-error.line_2.length", Length35), x => x.isEmpty || (x.nonEmpty && x.length <= Length35)),
          constraint[String](Messages("bc.business-registration-error.address.invalid", Messages("bc.business-registration.line_2")),
            x => x.trim.matches(LineRegex)))
        ),
        "line_3" -> optional(text).verifying(StopOnFirstFail(
          constraint[Option[String]](Messages("bc.business-registration-error.line_3.length", Length35), x => x.fold(true)(_.length <= Length35)),
          constraint[Option[String]](Messages("bc.business-registration-error.address.invalid", Messages("bc.business-registration.line_3")),
            x => x.fold(true)(_.trim.matches(LineRegex))))
        ),
        "line_4" -> optional(text).verifying(StopOnFirstFail(
          constraint[Option[String]](Messages("bc.business-registration-error.line_4.length", Length35), x => x.fold(true)(_.length <= Length35)),
          constraint[Option[String]](Messages("bc.business-registration-error.address.invalid", Messages("bc.business-registration.line_4")),
            x => x.fold(true)(_.trim.matches(LineRegex))))
        ),
        "postcode" -> optional(text).verifying(StopOnFirstFail(
          constraint[Option[String]](Messages("bc.business-registration-error.postcode.length", PostcodeLength), x =>  x.fold(true)(_.length <= PostcodeLength)),
          constraint[Option[String]](Messages("bc.business-registration-error.postcode.invalid"), x => x.fold(true)(_.trim.matches(PostcodeRegex))))
        ),
        "country" -> text.
          verifying(Messages("bc.business-registration-error.country"), x => x.length > Length0)

      )(Address.apply)(Address.unapply)
    )(BusinessRegistration.apply)(BusinessRegistration.unapply)
  )

  val overseasCompanyForm = Form(
    mapping(
      "hasBusinessUniqueId" -> optional(boolean).verifying(Messages("bc.business-registration-error.hasBusinessUniqueId.not-selected"), x => x.isDefined),
      "businessUniqueId" -> optional(text).verifying(StopOnFirstFail(
        constraint[Option[String]](Messages("bc.business-registration-error.businessUniqueId.length", Length60), x => x.fold(true)(_.length <= Length60)),
        constraint[Option[String]](Messages("bc.business-registration-error.businessUniqueId.invalid"), x => x.fold(true)(_.trim.matches(IdNumberRegex))))
      ),
      "issuingInstitution" -> optional(text).verifying(StopOnFirstFail(
        constraint[Option[String]](Messages("bc.business-registration-error.issuingInstitution.length", Length40), x => x.fold(true)(_.length <= Length40)),
        constraint[Option[String]](Messages("bc.business-registration-error.issuingInstitution.invalid"), x => x.fold(true)(_.trim.matches(IssuingInstitutionRegex))))
      ),
      "issuingCountry" -> optional(text).verifying(Messages("bc.business-registration-error.non-uk"), x => x.fold(true)(_.trim.matches(IssuingCountryRegex)))
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

  def validateNonUkIdentifiersInstitution(registrationData: Form[OverseasCompany]) = {
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
        registrationData.withError(key = "issuingInstitution", message = Messages("bc.business-registration-error.issuingInstitution.select"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersCountry(registrationData: Form[OverseasCompany]) = {
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
        registrationData.withError(key = "issuingCountry", message = Messages("bc.business-registration-error.issuingCountry.select"))
      case Some(true) if issuingCountry.isDefined && issuingCountry.fold("")(x => x).matches(CountryUK) =>
        registrationData.withError(key = "issuingCountry", message = Messages("bc.business-registration-error.non-uk"))
      case _ => registrationData
    }
  }

  def validateNonUkIdentifiersId(registrationData: Form[OverseasCompany]) = {
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
        registrationData.withError(key = "businessUniqueId", message = Messages("bc.business-registration-error.businessUniqueId.select"))
      case _ => registrationData
    }
  }

  private def validatePostCode(registrationData: Form[BusinessRegistration]) = {
    val postCode = registrationData.data.get("businessAddress.postcode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    if (postCode.isEmpty) {
      registrationData.withError(key = "businessAddress.postcode",
        message = Messages("bc.business-registration-error.postcode"))
    } else {
      if (!postCode.fold("")(x => x).matches(PostcodeRegex)) {
        registrationData.withError(key = "businessAddress.postcode",
          message = Messages("bc.business-registration-error.postcode.invalid"))
      } else {
        registrationData
      }
    }
  }

  def validateCountryNonUKAndPostcode(registrationData: Form[BusinessRegistration], service: String, isAgent: Boolean) = {
    val country = registrationData.data.get("businessAddress.country") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    val countryForm = {
      if (country.fold("")(x => x).matches(CountryUK)) {
        registrationData.withError(key = "businessAddress.country", message = Messages("bc.business-registration-error.non-uk"))
      } else {
        registrationData
      }
    }

    val postCode = registrationData.data.get("businessAddress.postcode") map {
      _.trim
    } filterNot {
      _.isEmpty
    }
    
    val validatePostCode = ApplicationConfig.validateNonUkClientPostCode(service) && !isAgent
    if (postCode.isEmpty && validatePostCode) {
      countryForm.withError(key = "businessAddress.postcode",
        message = Messages("bc.business-registration-error.postcode"))
    } else {
      countryForm
    }
  }

  val nrlQuestionForm = Form(
    mapping(
      "paysSA" -> optional(boolean).verifying(Messages("bc.nrl.paysSA.not-selected.error"), a => a.isDefined)
    )(NRLQuestion.apply)(NRLQuestion.unapply)
  )

  val paySAQuestionForm = Form(
    mapping(
      "paySA" -> optional(boolean).verifying(Messages("bc.nonuk.paySA.not-selected.error"), a => a.isDefined)
    )(PaySAQuestion.apply)(PaySAQuestion.unapply)
  )

}
