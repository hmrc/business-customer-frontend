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

import java.util.Properties
import play.api.Play
import scala.io.Source
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages

object BCUtils {
  val p = new Properties
  p.load(Source.fromInputStream(Play.classloader(Play.current).getResourceAsStream("country-code.properties"), "UTF-8").bufferedReader())

  private val ZERO = 0
  private val ONE = 1
  private val TWO = 2
  private val THREE = 3
  private val FOUR = 4
  private val FIVE = 5
  private val SIX = 6
  private val SEVEN = 7
  private val EIGHT = 8
  private val NINE = 9
  private val TEN = 10

  def validateUTR(utr: String): Boolean = {
    utr.trim.length == TEN && utr.trim.forall(_.isDigit) && {
      val actualUtr = utr.trim.toList
      val checkDigit = actualUtr.head.asDigit
      val restOfUtr = actualUtr.tail
      val weights = List(SIX, SEVEN, EIGHT, NINE, TEN, FIVE, FOUR, THREE, TWO)
      val weightedUtr = for ((w1, u1) <- weights zip restOfUtr) yield {
        w1 * u1.asDigit
      }
      val total = weightedUtr.sum
      val remainder = total % 11
      isValidUtr(remainder, checkDigit)
    }
  }

  private def isValidUtr(remainder: Int, checkDigit: Int): Boolean = {
    val mapOfRemainders = Map(ZERO -> TWO, ONE -> ONE, TWO -> NINE, THREE -> EIGHT, FOUR -> SEVEN, FIVE -> SIX,
      SIX -> FIVE, SEVEN -> FOUR, EIGHT -> THREE, NINE -> TWO, TEN -> ONE)
    mapOfRemainders.get(remainder).contains(checkDigit)
  }

  def getIsoCodeTupleList: List[(String, String)] = {
    val keys = p.propertyNames()
    val listOfCountryCodes: scala.collection.mutable.MutableList[(String, String)] = scala.collection.mutable.MutableList()
    while (keys.hasMoreElements) {
      val key = keys.nextElement().toString
      listOfCountryCodes.+=:((key, p.getProperty(key)))
    }
    listOfCountryCodes.toList.sortBy(_._2)
  }


  def getNavTitle(serviceName: String): Option[String] = {
    serviceName.toLowerCase match {
      case "ated" => Some(Messages("bc.ated.serviceName"))
      case "awrs" => Some(Messages("bc.awrs.serviceName"))
      case "amls" => Some(Messages("bc.amls.serviceName"))
      case "investment-tax-relief" => Some(Messages("bc.investment-tax-relief.serviceName"))
      case _ => None
    }
  }

  def businessTypeMap(service: String, isAgent: Boolean): Seq[(String, String)] = {
    val fixedBusinessTypes = Seq(
      "SOP" -> Messages("bc.business-verification.SOP"),
      "LTD" -> Messages("bc.business-verification.LTD"),
      "OBP" -> Messages("bc.business-verification.PRT"),
      "LP" -> Messages("bc.business-verification.LP"),
      "LLP" -> Messages("bc.business-verification.LLP"),
      "UIB" -> Messages("bc.business-verification.UIB")
    )
    val isAtedAgentBusinessTypes = Seq(
      "LTD" -> Messages("bc.business-verification.LTD"),
      "LLP" -> Messages("bc.business-verification.LLP"),
      "SOP" -> Messages("bc.business-verification.SOP"),
      "OBP" -> Messages("bc.business-verification.PRT"),
      "UIB" -> Messages("bc.business-verification.UIB"),
      "LP" -> Messages("bc.business-verification.LP"),
      "ULTD" -> Messages("bc.business-verification.ULTD"),
      "NUK" -> Messages("bc.business-verification.NUK")
    )
    val atedExtraBusinessTypes = Seq(
      "UT" -> Messages("bc.business-verification.UT"),
      "ULTD" -> Messages("bc.business-verification.ULTD"),
      "NUK" -> Messages("bc.business-verification.agent.NUK")
    )
    val isCGTBusinessTypes = Seq (
      "NUK" -> Messages("bc.business-verification.NUK"),
      "LTD" -> Messages("bc.business-verification.LTD"),
      "OBP" -> Messages("bc.business-verification.PRT"),
      "LP" -> Messages("bc.business-verification.LP"),
      "LLP" -> Messages("bc.business-verification.LLP"),
      "UIB" -> Messages("bc.business-verification.UIB")
    )
    val isCGTAgentTypes = Seq ("LTD", "LLP", "SOP", "OBP", "LP", "NUK")

    service.toLowerCase match {
      case "ated" if isAgent => isAtedAgentBusinessTypes.filterNot(p => p._1 == "UIB")
      case "ated" => fixedBusinessTypes.filterNot(p => p._1 == "UIB").filterNot(p => p._1 == "SOP") ++ atedExtraBusinessTypes
      case "awrs" => Seq("GROUP" -> Messages("bc.business-verification.GROUP")) ++ fixedBusinessTypes
      case "amls" => Seq("LTD" -> Messages("bc.business-verification.LTD"),
        "SOP" -> Messages("bc.business-verification.amls.SOP"),
        "OBP" -> Messages("bc.business-verification.amls.PRT"),
        "LLP" -> Messages("bc.business-verification.amls.LP.LLP"),
        "UIB" -> Messages("bc.business-verification.amls.UIB")
      )
      case "investment-tax-relief" => Seq("LTD" -> Messages("bc.business-verification.LTD"))
      case "capital-gains-tax" => isCGTBusinessTypes
      case "capital-gains-tax-agents" => isAtedAgentBusinessTypes.filter(p => isCGTAgentTypes.contains(p._1))
      case _ => fixedBusinessTypes
    }
  }

  def getSelectedCountry(isoCode: String): String = {
    def trimCountry(selectedCountry: String) = {
      val position = selectedCountry.indexOf(":")
      if (position > 0) {
        selectedCountry.substring(0, position).trim
      } else {
        selectedCountry
      }
    }
    def getCountry(isoCode: String): Option[String] = {
      val country = Option(p.getProperty(isoCode.toUpperCase))
      country.map(selectedCountry => trimCountry(selectedCountry))
    }
    getCountry(isoCode.toUpperCase).fold(isoCode) { x => x }
  }

  def validateGroupId(str: String) = if(str.trim.length != 36) {
    if(str.contains("testGroupId-")) str.replace("testGroupId-", "")
    else throw new RuntimeException("Invalid groupId from auth")
  } else str.trim
}
