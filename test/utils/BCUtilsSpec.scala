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

package utils

import config.{ApplicationConfig, BCUtils}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.Lang
import play.api.test.Injecting


class BCUtilsSpec extends PlaySpec with GuiceOneServerPerSuite with Injecting {

  val bcUtils: BCUtils = inject[ApplicationConfig]

  implicit val lang: Lang = Lang.defaultLang

  "BCUtils" must {

    "validateUTR" must {
      "given valid UTR return true" in {
        bcUtils.validateUTR("1111111111") must be(true)
        bcUtils.validateUTR("1111111112") must be(true)
        bcUtils.validateUTR("8111111113") must be(true)
        bcUtils.validateUTR("6111111114") must be(true)
        bcUtils.validateUTR("4111111115") must be(true)
        bcUtils.validateUTR("2111111116") must be(true)
        bcUtils.validateUTR("2111111117") must be(true)
        bcUtils.validateUTR("9111111118") must be(true)
        bcUtils.validateUTR("7111111119") must be(true)
        bcUtils.validateUTR("5111111123") must be(true)
        bcUtils.validateUTR("3111111124") must be(true)
      }
      "given invalid UTR return false" in {
        bcUtils.validateUTR("2111111111") must be(false)
        bcUtils.validateUTR("211111111") must be(false)
        bcUtils.validateUTR("211111 111 ") must be(false)
        bcUtils.validateUTR("211111ab111 ") must be(false)
      }
    }

    "getSelectedCountry" must {
      "bring the correct country from the file" in {
        bcUtils.getSelectedCountry("GB") must be("United Kingdom")
        bcUtils.getSelectedCountry("US") must be("USA")
        bcUtils.getSelectedCountry("VG") must be("British Virgin Islands")
        bcUtils.getSelectedCountry("UG") must be("Uganda")
        bcUtils.getSelectedCountry("zz") must be("zz")
      }
    }

    "getIsoCodeMap" must {
      "return map of country iso-code to country name" in {
        bcUtils.getIsoCodeTupleList must contain(("US", "USA :United States of America"))
        bcUtils.getIsoCodeTupleList must contain(("GB", "United Kingdom :UK, GB, Great Britain"))
        bcUtils.getIsoCodeTupleList must contain(("UG", "Uganda"))
      }
    }

    "getNavTitle" must {
      "for ated as service name, return ated" in {
        bcUtils.getNavTitle("ated") must be(Some("bc.ated.serviceName"))
      }
      "for awrs as service name, return awrs" in {
        bcUtils.getNavTitle("awrs") must be(Some("bc.awrs.serviceName"))
      }
      "for amls as service name, return amls" in {
        bcUtils.getNavTitle("amls") must be(Some("bc.amls.serviceName"))
      }
      "for investment-tax-relief as service name, return investment-tax-relief" in {
        bcUtils.getNavTitle("investment-tax-relief") must be(Some("bc.investment-tax-relief.serviceName"))
      }
      "for fhdds as service name, return investment-tax-relief" in {
        bcUtils.getNavTitle("fhdds") must be(Some("bc.fhdds.serviceName"))
      }
      "for other as service name, return None" in {
        bcUtils.getNavTitle("abcd") must be(None)
      }
    }

    "businessTypeMap" must {

      "return the correct map for ated" in {
        val typeMap = bcUtils.businessTypeMap("ated", false)
        typeMap.size must be(7)
        typeMap.head._1 must be("LTD")
        typeMap(1)._1 must be("OBP")
      }

      "return the correct map for awrs" in {
        val typeMap = bcUtils.businessTypeMap("awrs", false)
        typeMap.size must be(7)
        typeMap.head._1 must be("OBP")
        typeMap(1)._1 must be("GROUP")
      }

      "return the correct map for amls" in {
        val typeMap = bcUtils.businessTypeMap("amls", false)
        typeMap.size must be(5)
        typeMap mustBe Seq(
          "LTD" -> "bc.business-verification.LTD",
          "SOP" -> "bc.business-verification.amls.SOP",
          "OBP" -> "bc.business-verification.amls.PRT",
          "LLP" -> "bc.business-verification.amls.LP.LLP",
          "UIB" -> "bc.business-verification.amls.UIB"
        )
      }

      "return the correct map for investment-tax-relief" in {
        val typeMap = bcUtils.businessTypeMap("investment-tax-relief", false)
        typeMap.size must be(1)
        typeMap mustBe Seq(
          "LTD" -> "bc.business-verification.LTD"
        )
      }

      "return the correct map for capital-gains-tax" in {
        val typeMap = bcUtils.businessTypeMap("capital-gains-tax", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "NUK" -> "bc.business-verification.NUK",
          "LTD" -> "bc.business-verification.LTD",
          "OBP" -> "bc.business-verification.PRT",
          "LP" -> "bc.business-verification.LP",
          "LLP" -> "bc.business-verification.LLP",
          "UIB" -> "bc.business-verification.UIB"
        )
      }

      "return the correct sequence for capital-gains-tax-agents" in {
        val typeMap = bcUtils.businessTypeMap("capital-gains-tax-agents", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "LTD" -> "bc.business-verification.LTD",
          "LLP" -> "bc.business-verification.LLP",
          "SOP" -> "bc.business-verification.SOP",
          "OBP" -> "bc.business-verification.PRT",
          "LP" -> "bc.business-verification.LP",
          "NUK" -> "bc.business-verification.NUK"
        )
      }

      "return default map when passed nothing" in {
        val typeMap = bcUtils.businessTypeMap("", false)
        typeMap.size must be(6)
        typeMap mustBe Seq(
          "SOP" -> "bc.business-verification.SOP",
          "LTD" -> "bc.business-verification.LTD",
          "OBP" -> "bc.business-verification.PRT",
          "LP" -> "bc.business-verification.LP",
          "LLP" -> "bc.business-verification.LLP",
          "UIB" -> "bc.business-verification.UIB"
        )
      }
    }

    "validateGroupId" must {

      "throw an exception" when {
        "invalid string is passed" in {
          val thrown = the[RuntimeException] thrownBy bcUtils.validateGroupId("abc-def-ghi")
          thrown.getMessage must include("Invalid groupId from auth")
        }
      }

      "return groupId" when {
        "valid string is passed" in {
          val result = bcUtils.validateGroupId("42424200-0000-0000-0000-000000000000")
          result must be("42424200-0000-0000-0000-000000000000")
        }

        "string with testGroupId- is passed" in {
          val result = bcUtils.validateGroupId("testGroupId-0000-0000-0000-000000000000")
          result must be("0000-0000-0000-000000000000")
        }
      }

    }

  }

}
