/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}

case class Individual(firstName: String,
                      lastName: String,
                      dateOfBirth: Option[String])

object Individual {
  implicit val formats: Format[Individual] = Json.format[Individual]
}


case class Organisation(organisationName: String,
                        organisationType: String)

object Organisation {
  implicit val formats: Format[Organisation] = Json.format[Organisation]
}


case class OrganisationResponse(organisationName: String,
                                isAGroup: Option[Boolean],
                                organisationType: String)

object OrganisationResponse {
  implicit val formats: Format[OrganisationResponse] = Json.format[OrganisationResponse]
}


case class MatchBusinessData(acknowledgementReference: String,
                             utr: String,
                             requiresNameMatch: Boolean = false,
                             isAnAgent: Boolean = false,
                             individual: Option[Individual],
                             organisation: Option[Organisation])

object MatchBusinessData {
  implicit val formats: Format[MatchBusinessData] = Json.format[MatchBusinessData]
}


case class MatchFailureResponse(reason: String)

object MatchFailureResponse {
  implicit val formats: Format[MatchFailureResponse] = Json.format[MatchFailureResponse]
}


case class NRLQuestion(paysSA: Option[Boolean] = None)

object NRLQuestion {
  implicit val formats: Format[NRLQuestion] = Json.format[NRLQuestion]
}


case class PaySAQuestion(paySA: Option[Boolean] = None)

object PaySAQuestion {
  implicit val formats: Format[PaySAQuestion] = Json.format[PaySAQuestion]
}
