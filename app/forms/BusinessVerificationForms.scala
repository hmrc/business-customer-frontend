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


import config.BCUtils
import play.api.Environment
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.{Format, Json}

import scala.util.matching.Regex


case class SoleTraderMatch(firstName: String, lastName: String, saUTR: String)

object SoleTraderMatch {
  implicit val formats: Format[SoleTraderMatch] = Json.format[SoleTraderMatch]
}

case class BusinessName(businessName: String)

object BusinessName {
  implicit val formats: Format[BusinessName] = Json.format[BusinessName]
}

case class Utr(utr: String)

object Utr {
  implicit val formats: Format[Utr] = Json.format[Utr]
}
case class SoleTraderName(firstName: String, lastName: String)
object SoleTraderName {
  implicit val Oformat: Format[SoleTraderName] = Json.format[SoleTraderName]
}

case class LimitedCompanyMatch(businessName: String, cotaxUTR: String)

object LimitedCompanyMatch {
  implicit val formats: Format[LimitedCompanyMatch] = Json.format[LimitedCompanyMatch]
}


case class NonResidentLandlordMatch(businessName: String, saUTR: String)

object NonResidentLandlordMatch {
  implicit val formats: Format[NonResidentLandlordMatch] = Json.format[NonResidentLandlordMatch]
}


case class UnincorporatedMatch(businessName: String, cotaxUTR: String)

object UnincorporatedMatch {
  implicit val formats: Format[UnincorporatedMatch] = Json.format[UnincorporatedMatch]
}


case class OrdinaryBusinessPartnershipMatch(businessName: String, psaUTR: String)

object OrdinaryBusinessPartnershipMatch {
  implicit val formats: Format[OrdinaryBusinessPartnershipMatch] = Json.format[OrdinaryBusinessPartnershipMatch]
}


case class LimitedLiabilityPartnershipMatch(businessName: String, psaUTR: String)

object LimitedLiabilityPartnershipMatch {
  implicit val formats: Format[LimitedLiabilityPartnershipMatch] = Json.format[LimitedLiabilityPartnershipMatch]
}


case class LimitedPartnershipMatch(businessName: String, psaUTR: String)

object LimitedPartnershipMatch {
  implicit val formats: Format[LimitedPartnershipMatch] = Json.format[LimitedPartnershipMatch]
}


case class BusinessType(businessType: Option[String] = None, isSaAccount: Boolean, isOrgAccount: Boolean)

object BusinessType {
  implicit val formats: Format[BusinessType] = Json.format[BusinessType]
}


case class BusinessDetails(businessType: String,
                           soleTrader: Option[SoleTraderMatch],
                           ltdCompany: Option[LimitedCompanyMatch],
                           uibCompany: Option[UnincorporatedMatch],
                           obpCompany: Option[OrdinaryBusinessPartnershipMatch],
                           llpCompany: Option[LimitedLiabilityPartnershipMatch],
                           lpCompany: Option[LimitedPartnershipMatch],
                           nrlCompany: Option[NonResidentLandlordMatch])

object BusinessDetails {
  implicit val formats: Format[BusinessDetails] = Json.format[BusinessDetails]
}

object BusinessVerificationForms extends BCUtils {

  override val environment: Environment = Environment.simple()

  val length40 = 40
  val length0 = 0
  val length105 = 105
  val utrRegex: Regex = """^[0-9]{10}$""".r

  def validateBusinessType(businessTypeData: Form[BusinessType], service: String): Form[BusinessType] = {
    val isSaAccount = businessTypeData.data.get("isSaAccount").fold(false)(_.toBoolean)
    val isOrgAccount = businessTypeData.data.get("isOrgAccount").fold(false)(_.toBoolean)
    val businessType = businessTypeData.data.get("businessType") map (_.trim) filterNot (_.isEmpty)

    (businessType.nonEmpty, businessType.getOrElse(""), isSaAccount, isOrgAccount) match {
      case (true, "SOP", false, true) =>
        service match {
          case "amls" => businessTypeData
          case _ => businessTypeData.withError(
            key = "businessType",
            message = "bc.business-verification-error.type_of_business_organisation_invalid")
        }
      case (true, "SOP", true, false) => businessTypeData
      case (true, _, true, false) =>
        businessTypeData.withError(
          key = "businessType",
          message = "bc.business-verification-error.type_of_business_individual_invalid"
        )
      case _ => businessTypeData
    }
  }

  val businessTypeForm: Form[BusinessType] = Form(mapping(
    "businessType" -> optional(text).verifying("bc.business-verification-error.not-selected", x => x.isDefined),
    "isSaAccount" -> boolean,
    "isOrgAccount" -> boolean
  )(BusinessType.apply)(BusinessType.unapply)
  )

  val soleTraderForm: Form[SoleTraderMatch] = Form(mapping(
    "firstName" -> text
      .verifying("bc.business-verification-error.firstname", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.firstname.length", x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
    "lastName" -> text
      .verifying("bc.business-verification-error.surname", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.surname.length", x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
    "saUTR" -> text
      .verifying("bc.business-verification-error.sautr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.sautr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidSAUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(SoleTraderMatch.apply)(SoleTraderMatch.unapply))

  val limitedCompanyForm: Form[LimitedCompanyMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "cotaxUTR" -> text
      .verifying("bc.business-verification-error.cotaxutr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.cotaxutr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidCOUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(LimitedCompanyMatch.apply)(LimitedCompanyMatch.unapply))

  val businessName: Form[BusinessName] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105))
  )(BusinessName.apply)(BusinessName.unapply))

  val utr: Form[Utr] = Form(mapping(
    "utr" -> text
      .verifying("bc.business-verification-error.cotaxutr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.cotaxutr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidCOUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(Utr.apply)(Utr.unapply))

  val nonResidentLandlordForm: Form[NonResidentLandlordMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "saUTR" -> text
      .verifying("bc.business-verification-error.sautr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.sautr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidSAUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(NonResidentLandlordMatch.apply)(NonResidentLandlordMatch.unapply))

  val unincorporatedBodyForm: Form[UnincorporatedMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "cotaxUTR" -> text
      .verifying("bc.business-verification-error.cotaxutr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.cotaxutr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidCOUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(UnincorporatedMatch.apply)(UnincorporatedMatch.unapply))

  val ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessPartnerName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredPartnerName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying("bc.business-verification-error.psautr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.psautr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidPSAUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(OrdinaryBusinessPartnershipMatch.apply)(OrdinaryBusinessPartnershipMatch.unapply))

  val limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying("bc.business-verification-error.psautr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.psautr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidPSAUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(LimitedLiabilityPartnershipMatch.apply)(LimitedLiabilityPartnershipMatch.unapply))
  val limitedPartnershipForm: Form[LimitedPartnershipMatch] = Form(mapping(
    "businessName" -> text
      .verifying("bc.business-verification-error.businessPartnerName", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.registeredPartnerName.length", x => x.isEmpty || (x.nonEmpty && x.length <= length105)),
    "psaUTR" -> text
      .verifying("bc.business-verification-error.psautr", x => x.replaceAll(" ", "").length > length0)
      .verifying("bc.business-verification-error.psautr.length", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (trimmedString.nonEmpty && trimmedString.matches("""^[0-9]{10}$"""))}
      )
      .verifying("bc.business-verification-error.invalidPSAUTR", x => {
        val trimmedString = x.replaceAll(" ", "")
        trimmedString.isEmpty || (validateUTR(trimmedString) || !trimmedString.matches("""^[0-9]{10}$"""))
      })
  )(LimitedPartnershipMatch.apply)(LimitedPartnershipMatch.unapply))

  val soleTraderNameForm: Form[SoleTraderName] = Form(mapping(
    "firstName" -> text
      .verifying("bc.business-verification-error.firstname", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.firstname.length", x => x.isEmpty || (x.nonEmpty && x.length <= length40)),
    "lastName" -> text
      .verifying("bc.business-verification-error.surname", x => x.trim.length > length0)
      .verifying("bc.business-verification-error.surname.length", x => x.isEmpty || (x.nonEmpty && x.length <= length40)),

  )(SoleTraderName.apply)(SoleTraderName.unapply))
}
