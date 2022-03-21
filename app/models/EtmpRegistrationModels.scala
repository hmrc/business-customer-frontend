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

import org.joda.time.LocalDate
import play.api.libs.json.JodaReads.DefaultJodaLocalDateReads
import play.api.libs.json.JodaWrites.DefaultJodaLocalDateWrites
import play.api.libs.json.{Format, Json}
import config.BCUtils
import play.api.Environment
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Reads.verifying
import play.api.libs.json._

case class EtmpAddress(addressLine1: String,
                       addressLine2: String,
                       addressLine3: Option[String],
                       addressLine4: Option[String],
                       postalCode: Option[String],
                       countryCode: String)

//object EtmpAddress {
//  implicit val formats: Format[EtmpAddress] = Json.format[EtmpAddress]
//}

object EtmpAddress extends BCUtils {

  override val environment: Environment = Environment.simple()
  implicit val writes: Writes[EtmpAddress] = Json.writes[EtmpAddress]
  implicit val read: Reads[EtmpAddress] = (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").read[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "postalCode").readNullable[String] and
      (JsPath \ "countryCode").read[String](verifying[String](cc => getSelectedCountry(cc) != cc))
    )(EtmpAddress.apply _)
}

case class EtmpOrganisation(organisationName: String)

object EtmpOrganisation {
  implicit val formats: Format[EtmpOrganisation] = Json.format[EtmpOrganisation]
}

case class EtmpIndividual(firstName: String,
                          middleName: Option[String] = None,
                          lastName: String,
                          dateOfBirth: LocalDate)

object EtmpIndividual {
  implicit val dateFormat: Format[LocalDate] = Format(DefaultJodaLocalDateReads, DefaultJodaLocalDateWrites)
  implicit val formats: Format[EtmpIndividual] = Json.format[EtmpIndividual]
}

case class EtmpContactDetails(phoneNumber: Option[String] = None,
                              mobileNumber: Option[String] = None,
                              faxNumber: Option[String] = None,
                              emailAddress: Option[String] = None)

object EtmpContactDetails {
  implicit val formats: Format[EtmpContactDetails] = Json.format[EtmpContactDetails]
}

case class EtmpIdentification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

object EtmpIdentification {
  implicit val formats: Format[EtmpIdentification] = Json.format[EtmpIdentification]
}

case class BusinessRegistrationRequest(acknowledgementReference: String,
                                       isAnAgent: Boolean,
                                       isAGroup: Boolean,
                                       identification: Option[EtmpIdentification],
                                       organisation: EtmpOrganisation,
                                       address: EtmpAddress,
                                       contactDetails: EtmpContactDetails)

object BusinessRegistrationRequest {
  implicit val formats: Format[BusinessRegistrationRequest] = Json.format[BusinessRegistrationRequest]
}


case class BusinessRegistrationResponse(processingDate: String,
                                        sapNumber: String,
                                        safeId: String,
                                        agentReferenceNumber: Option[String])

object BusinessRegistrationResponse {
  implicit val formats: Format[BusinessRegistrationResponse] = Json.format[BusinessRegistrationResponse]
}


case class UpdateRegistrationDetailsRequest(acknowledgementReference: String,
                                            isAnIndividual: Boolean,
                                            individual: Option[EtmpIndividual],
                                            organisation: Option[EtmpOrganisation],
                                            address: EtmpAddress,
                                            contactDetails: EtmpContactDetails,
                                            isAnAgent: Boolean,
                                            isAGroup: Boolean,
                                            identification: Option[EtmpIdentification] = None) {
}

object UpdateRegistrationDetailsRequest {
  implicit val formats: Format[UpdateRegistrationDetailsRequest] = Json.format[UpdateRegistrationDetailsRequest]
}
