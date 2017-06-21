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

class OverseasCompanyUtilsSpec extends PlaySpec  with OneServerPerSuite {

  "OverseasCompanyUtils" must {
    "return the correct data for a client" in {
      val details = OverseasCompanyUtils.displayDetails(false, false, "awrs")
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("AWRS registration")
    }

    "return the correct data for an agent" in {
      val details = OverseasCompanyUtils.displayDetails(true, false, "ated")
      details.addClient must be (false)
      details.title must be ("Do you have an overseas company registration number?")
      details.header must be ("Do you have an overseas company registration number?")
      details.subHeader must be ("ATED agency set up")
    }

    "return the correct data for an agent adding a client" in {
      val details = OverseasCompanyUtils.displayDetails(true, true, "ated")
      details.addClient must be (true)
      details.title must be ("Does your client have an overseas company registration number?")
      details.header must be ("Does your client have an overseas company registration number?")
      details.subHeader must be ("Add a client")
    }
  }
}
