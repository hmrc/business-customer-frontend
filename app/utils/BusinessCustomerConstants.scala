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
  val CountryUK = "GB"

  val PostcodeLength = 10
  val Length40 = 40
  val Length35 = 35
  val Length0 = 0
  val Length2 = 2
  val Length60 = 60
  val Length105 = 105

  val PostcodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}|BFPO\\s?[0-9]{1,10}$"
  val LineRegex = "^[A-Za-z0-9 \\-,.&']{1,35}$"
  val UtrRegex = """^[0-9]{10}$"""
  val NameRegex = "^[a-zA-Z &`\\-\'^]{1,35}$"
  val BusinessNameRegex = "^[a-zA-Z0-9 '&\\\\/]{1,105}$"
  val IdNumberRegex = "^[a-zA-Z0-9 '&\\-]{1,60}$"
  val IssuingInstitutionRegex = "^[a-zA-Z0-9 '&\\-\\/]{1,40}$"
  val IssuingCountryRegex = "(?!^GB$)^[A-Z]{2}$"
}