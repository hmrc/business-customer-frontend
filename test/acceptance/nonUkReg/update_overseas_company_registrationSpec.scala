/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.BusinessRegistrationForms._
import models.OverseasCompanyDisplayDetails
import org.jsoup.Jsoup
import org.mockito.MockitoSugar
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.test.{FakeRequest, Injecting}

class update_overseas_company_registrationSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with Injecting {

  val service = "ATED"
  val injectedViewInstance = inject[views.html.nonUkReg.update_overseas_company_registration]
  val displayDetails = OverseasCompanyDisplayDetails(
    "dynamicTitle",
    "dynamicHeader",
    "dynamicSubHeader",
    false)

  implicit val lang = Lang.defaultLang
  implicit val appConfig = inject[ApplicationConfig]

  Feature("The user can view the overseas company registration question") {

    info("as a client i want to be able to view the overseas company registration question page")

    Scenario("return overseas company registration view for a client") {

      Given("the client has a non uk company and the arrive at the overseas company registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages : play.api.i18n.Messages = play.api.i18n.MessagesImpl(Lang.defaultLang, messagesApi)

      val html = injectedViewInstance(overseasCompanyForm, service, displayDetails, List(("UK", "UK")), None, Some("http://backLinkUrl"))

      val document = Jsoup.parse(html.toString())

      Then("The header should match - dynamicHeader")
      assert(document.select("h1").text === displayDetails.header)

      Then("The subheader should be - dynamicSubHeader")
      assert(document.getElementById("overseas-subheader").text() === "This section is: " + displayDetails.subHeader)

      Then("We should have a back link")
      assert(document.getElementById("backLinkHref").text() === "Back")
      assert(document.getElementById("backLinkHref").attr("href") === "http://backLinkUrl")

      Then("The options should be Yes and No")
      assert(document.select(".govuk-radios__item").text() === "Yes No")

      Then("The company registration number fields should exist")
      assert(document.getElementsByAttributeValue("for","businessUniqueId").text() === "Overseas company registration number")
      assert(document.getElementsByAttributeValue("for","issuingCountry").text() === "Country that issued the number")
      assert(document.getElementsByAttributeValue("for","issuingInstitution").text() === "Institution that issued the number")
      assert(document.select("#issuingInstitution-hint").text() === "For example, an overseas tax department")

      And("There is a link to the accessibility statement")
      assert(document.select(".govuk-footer__inline-list-item:nth-child(2) > a")
        .attr("href") === "http://localhost:12346/accessibility-statement/ated-subscription?referrerUrl=http%3A%2F%2Flocalhost%3A9923%2F")
    }
  }
}