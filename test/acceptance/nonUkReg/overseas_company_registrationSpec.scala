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

package acceptance.nonUkReg

import forms.BusinessRegistrationForms._
import models.OverseasCompanyDisplayDetails
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages
import play.api.test.FakeRequest

class overseas_company_registrationSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val service = "ATED"
  val displayDetails = OverseasCompanyDisplayDetails(
    "dynamicTitle",
    "dynamicHeader",
    "dynamicSubHeader",
    false)
  feature("The user can view the overseas company registration question") {

    info("as a client i want to be able to view the overseas company registration question page")

    scenario("return overseas company registration view for a client") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages: play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service, displayDetails, List(("UK", "UK")), None, Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - dynamicHeader")
      assert(document.select("h1").text === displayDetails.header)

      Then("The subheader should be - dynamicSubHeader")
      assert(document.getElementById("overseas-subheader").text() === "dynamicSubHeader")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

      Then("The company registration number fields should exist")
      assert(document.getElementById("businessUniqueId_field").text() === "Overseas company registration number")
      assert(document.getElementById("issuingCountry_field").text() === "Country that issued the number")
      assert(document.getElementById("issuingInstitution_field").text() === "Institution that issued the number For example, an overseas tax department")
    }
  }
}
