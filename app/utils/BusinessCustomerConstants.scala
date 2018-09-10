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

package utils

object BusinessCustomerConstants {

  val CorporateBody = "Corporate Body"
  val UnincorporatedBody = "Unincorporated Body"
  val Llp = "LLP"
  val Partnership = "Partnership"

  val IdentifierArn = "arn"
  val IdentifierUtr = "utr"
  val IdentifierSafeId = "safeid"
  val BusinessRegDetailsId = "BC_NonUK_Business_Details"
  val OverseasRegDetailsId = "Overseas_Business_Details"
  val PaySaDetailsId = "Pay_Sa_Details"
  val NrlFormId = "NRL_Details"
}

trait ValidationConstants {
  val countryUK = "GB"

  val postcodeLength = 10
  val length40 = 40
  val length35 = 35
  val length0 = 0
  val length2 = 2
  val length60 = 60
  val length105 = 105

  val postcodeRegex =
    """(([gG][iI][rR] {0,}0[aA]{2})|((([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y]?[0-9][0-9]?)|(([a-pr-uwyzA-PR-UWYZ][0-9][a-hjkstuwA-HJKSTUW])|([a-pr-uwyzA-PR-UWYZ][a-hk-yA-HK-Y][0-9][abehmnprv-yABEHMNPRV-Y]))) {0,}[0-9][abd-hjlnp-uw-zABD-HJLNP-UW-Z]{2}))$"""
  val lineRegex = "^[A-Za-z0-9 \\-,.&']{1,35}$"
  val utrRegex = """^[0-9]{10}$"""
  val nameRegex = "^[a-zA-Z &`\\-\'^]{1,35}$"
  val businessNameRegex = "^[a-zA-Z0-9 '&\\\\/]{1,105}$"
  val idNumberRegex = "^[a-zA-Z0-9 '&\\-]{1,60}$"
  val issuingInstitutionRegex = "^[a-zA-Z0-9 '&\\-\\/]{1,40}$"
}