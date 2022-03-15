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

import config.BCUtils
import play.api.Environment
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Reads.verifying
import play.api.libs.json._

case class OverseasCompanyDisplayDetails(title: String,
                                         header: String,
                                         subHeader: String,
                                         addClient: Boolean)

case class BusinessRegistrationDisplayDetails(businessType: String,
                                              businessRegHeader: String,
                                              businessRegSubHeader: String,
                                              businessRegLede: Option[String],
                                              listOfIsoCode: List[(String, String)])

case class Address(line_1: String,
                   line_2: String,
                   line_3: Option[String],
                   line_4: Option[String],
                   postcode: Option[String] = None,
                   country: String) {
  override def toString: String = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
}

object Address extends BCUtils {

  override val environment: Environment = Environment.simple()
  implicit val writes: Writes[Address] = Json.writes[Address]
  implicit val read: Reads[Address] = (
    (JsPath \ "line_1").read[String] and
    (JsPath \ "line_2").read[String] and
    (JsPath \ "line_3").readNullable[String] and
    (JsPath \ "line_4").readNullable[String] and
    (JsPath \ "postcode").readNullable[String] and
    (JsPath \ "country").read[String](verifying[String](cc => getSelectedCountry(cc) != cc))
    )(Address.apply _)
}

case class BusinessRegistration(businessName: String,
                                businessAddress: Address)

object BusinessRegistration {
  implicit val formats: Format[BusinessRegistration] = Json.format[BusinessRegistration]
}


case class OverseasCompany(hasBusinessUniqueId: Option[Boolean] = Some(false),
                           businessUniqueId: Option[String] = None,
                           issuingInstitution: Option[String] = None,
                           issuingCountry: Option[String] = None)

object OverseasCompany {
  implicit val formats: Format[OverseasCompany] = Json.format[OverseasCompany]
}
