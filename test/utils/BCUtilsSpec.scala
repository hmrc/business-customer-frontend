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

package utils

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages

class BCUtilsSpec extends PlaySpec with OneServerPerSuite {

  "BCUtils" must {

    "validateUTR" must {
      "given valid UTR return true" in {
        BCUtils.validateUTR("1111111111") must be(true)
        BCUtils.validateUTR("1111111112") must be(true)
        BCUtils.validateUTR("8111111113") must be(true)
        BCUtils.validateUTR("6111111114") must be(true)
        BCUtils.validateUTR("4111111115") must be(true)
        BCUtils.validateUTR("2111111116") must be(true)
        BCUtils.validateUTR("2111111117") must be(true)
        BCUtils.validateUTR("9111111118") must be(true)
        BCUtils.validateUTR("7111111119") must be(true)
        BCUtils.validateUTR("5111111123") must be(true)
        BCUtils.validateUTR("3111111124") must be(true)
      }
      "given invalid UTR return false" in {
        BCUtils.validateUTR("2111111111") must be(false)
        BCUtils.validateUTR("211111111") must be(false)
        BCUtils.validateUTR("211111 111 ") must be(false)
        BCUtils.validateUTR("211111ab111 ") must be(false)
      }
    }

    "getSelectedCountry" must {
      "bring the correct country from the file" in {
        BCUtils.getSelectedCountry("GB") must be("United Kingdom")
        BCUtils.getSelectedCountry("US") must be("USA")
        BCUtils.getSelectedCountry("VG") must be("British Virgin Islands")
        BCUtils.getSelectedCountry("UG") must be("Uganda")
        BCUtils.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        BCUtils.getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        BCUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        BCUtils.getIsoCodeTupleList must contain(("UG", "Uganda"))
      }
    }


    "getNavTitle" must {
      "for ated as service name, return ated" in {
        BCUtils.getNavTitle("ated") must be(Some("Submit and view your ATED returns"))
      }
      "for awrs as service name, return awrs" in {
        BCUtils.getNavTitle("awrs") must be(Some("Register as an alcohol wholesaler for AWRS"))
      }
      "for amls as service name, return amls" in {
        BCUtils.getNavTitle("amls") must be(Some("Anti Money Laundering Supervision"))
      }
      "for investment-tax-relief as service name, return investment-tax-relief" in {
        BCUtils.getNavTitle("investment-tax-relief") must be(Some("Apply for Enterprise Investment Scheme"))
      }
      "for other as service name, return None" in {
        BCUtils.getNavTitle("abcd") must be(None)
      }
    }

    "businessTypeMap" must {

      "return the correct map for ated" in {
        val typeMap = BCUtils.businessTypeMap("ated", false)
        typeMap.size must be(7)
        typeMap.head._1 must be("LTD")
        typeMap(1)._1 must be("OBP")
      }

      "return the correct map for awrs" in {
        val typeMap = BCUtils.businessTypeMap("awrs", false)
        typeMap.size must be(7)
        typeMap.head._1 must be("GROUP")
        typeMap(1)._1 must be("SOP")
      }

      "return the correct map for amls" in {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val typeMap = BCUtils.businessTypeMap("amls", false)
        typeMap.size must be(5)
        typeMap mustBe Seq(
          "LTD" -> Messages("bc.business-verification.LTD"),
          "SOP" -> Messages("bc.business-verification.amls.SOP"),
          "OBP" -> Messages("bc.business-verification.amls.PRT"),
          "LLP" -> Messages("bc.business-verification.amls.LP.LLP"),
          "UIB" -> Messages("bc.business-verification.amls.UIB")
        )
      }

      "return the correct map for investment-tax-relief" in {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val typeMap = BCUtils.businessTypeMap("investment-tax-relief", false)
        typeMap.size must be(1)
        typeMap mustBe Seq(
          "LTD" -> Messages("bc.business-verification.LTD")
        )
      }

      "return the correct map for capital-gains-tax" in {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val typeMap = BCUtils.businessTypeMap("capital-gains-tax", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "NUK" -> Messages("bc.business-verification.NUK"),
          "LTD" -> Messages("bc.business-verification.LTD"),
          "OBP" -> Messages("bc.business-verification.PRT"),
          "LP" -> Messages("bc.business-verification.LP"),
          "LLP" -> Messages("bc.business-verification.LLP"),
          "UIB" -> Messages("bc.business-verification.UIB")
        )
      }

      "return the correct sequence for capital-gains-tax-agents" in {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val typeMap = BCUtils.businessTypeMap("capital-gains-tax-agents", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "LTD" -> Messages("bc.business-verification.LTD"),
          "LLP" -> Messages("bc.business-verification.LLP"),
          "SOP" -> Messages("bc.business-verification.SOP"),
          "OBP" -> Messages("bc.business-verification.PRT"),
          "LP" -> Messages("bc.business-verification.LP"),
          "NUK" -> Messages("bc.business-verification.NUK")
        )
      }

      "return default map when passed nothing" in {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val typeMap = BCUtils.businessTypeMap("", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "SOP" -> Messages("bc.business-verification.SOP"),
          "LTD" -> Messages("bc.business-verification.LTD"),
          "OBP" -> Messages("bc.business-verification.PRT"),
          "LP" -> Messages("bc.business-verification.LP"),
          "LLP" -> Messages("bc.business-verification.LLP"),
          "UIB" -> Messages("bc.business-verification.UIB")
        )
      }
    }

    "newService" must {
      "return true, if it's a new service" in {
        BCUtils.newService("hello") must be(true)
      }

      "return false, if it's an old service ATED" in {
        BCUtils.newService("ated") must be(false)
      }

      "return false, if it's an old service AMLS" in {
        BCUtils.newService("AMLS") must be(false)
      }
    }

  }

}
