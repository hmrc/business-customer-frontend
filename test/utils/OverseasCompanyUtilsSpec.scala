/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

class OverseasCompanyUtilsSpec extends PlaySpec  with GuiceOneServerPerSuite {

  trait Setup {
    val utils: OverseasCompanyUtils = new OverseasCompanyUtils {}
  }

  "OverseasCompanyUtils" must {
    "return the correct data for a client" in new Setup {
      val details = utils.displayDetails(false, false, "awrs")
      details.addClient must be (false)
      details.title must be ("bc.nonuk.overseas.client.title")
      details.header must be ("bc.nonuk.overseas.client.header")
      details.subHeader must be ("bc.nonuk.overseas.client.subheader")
    }

    "return the correct data for an agent" in new Setup {
      val details = utils.displayDetails(true, false, "ated")
      details.addClient must be (false)
      details.title must be ("bc.nonuk.overseas.agent.title")
      details.header must be ("bc.nonuk.overseas.agent.header")
      details.subHeader must be ("bc.nonuk.overseas.agent.subheader")
    }

    "return the correct data for an agent adding a client" in new Setup {
      val details = utils.displayDetails(true, true, "ated")
      details.addClient must be (true)
      details.title must be ("bc.nonuk.overseas.agent.add-client.title")
      details.header must be ("bc.nonuk.overseas.agent.add-client.header")
      details.subHeader must be ("bc.nonuk.overseas.agent.add-client.subheader")
    }
  }
}
