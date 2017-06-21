/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json.Json

case class Individual(firstName: String, lastName: String, dateOfBirth: Option[String])

case class Organisation(organisationName: String, organisationType: String)

case class OrganisationResponse(organisationName: String, isAGroup: Option[Boolean], organisationType: Option[String])

case class MatchBusinessData(acknowledgementReference: String,
                             utr: String,
                             requiresNameMatch: Boolean = false,
                             isAnAgent: Boolean = false,
                             individual: Option[Individual],
                             organisation: Option[Organisation])

object Individual {
  implicit val formats = Json.format[Individual]
}

object Organisation {
  implicit val formats = Json.format[Organisation]
}

object OrganisationResponse {
  implicit val formats = Json.format[OrganisationResponse]
}

object MatchBusinessData {
  implicit val formats = Json.format[MatchBusinessData]
}

case class MatchFailureResponse(reason: String)

object MatchFailureResponse {
  implicit val formats = Json.format[MatchFailureResponse]
}

case class MatchSuccessResponse(isAnIndividual: Boolean,
                                agentReferenceNumber: Option[String],
                                sapNumber: Option[String],
                                safeId: String,
                                address: EtmpAddress,
                                organisation: Option[OrganisationResponse] = None,
                                individual: Option[Individual] = None)

object MatchSuccessResponse {
  implicit val format = Json.format[MatchSuccessResponse]
}

case class NRLQuestion(paysSA: Option[Boolean] = None)
object NRLQuestion {
  implicit val formats = Json.format[NRLQuestion]
}


case class PaySAQuestion(paySA: Option[Boolean] = None)
object PaySAQuestion {
  implicit val formats = Json.format[PaySAQuestion]
}

case class ClientPermission(permission: Option[Boolean] = None)
