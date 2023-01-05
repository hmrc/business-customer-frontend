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

package acceptance

import config.ApplicationConfig
import models.{Address, Identification, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.MockitoSugar
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import views.html.review_details_non_uk_agent

class ReviewDetailsNonUkAgentFeatureSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar
  with BeforeAndAfterEach with GivenWhenThen with Matchers with Injecting {

  val address: Address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "GB")
  val addressOverseas: Address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), None, "CI")
  val reviewDetails: ReviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", directMatch = true,
    agentReferenceNumber = Some("agent123"), identification = Some(Identification("id","inst", "FR")))
  val reviewDetailsOverseas: ReviewDetails = ReviewDetails(
    "OverseasAgentRegistration", Some("NonUKBusinessType"), addressOverseas, "sap123", "safe123", directMatch = true,
    agentReferenceNumber = Some("agent123"), identification = Some(Identification("id","inst", "CI")))

  implicit val lang: Lang = Lang.defaultLang
  implicit val appConfig: ApplicationConfig = inject[ApplicationConfig]
  val injectedViewInstance: review_details_non_uk_agent = inject[views.html.review_details_non_uk_agent]

  Feature("The user can view the review details page for a non uk agent") {

    info("as a user i want to be able to view my review details page")

    Scenario("return Review Details view for an agent, when agent can't be directly found with login credentials and the reg is editable") {

      Given("An agent has an editable business registration details")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages : play.api.i18n.Messages = play.api.i18n.MessagesImpl(Lang.defaultLang, messagesApi)

      val html = injectedViewInstance("ATED", reviewDetails.copy(directMatch = false), Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Confirm your business details")
      assert(document.select("h1").text contains "Check your agency details")

      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: ATED agency set up")

      And("The confirmation notice should display")
      assert(document.getElementById("check-agency-details").text ===
        "! Warning You are setting up your agency. These should be your company details not your client’s.")

      And("Business name is correct")

      assert(document.getElementById("business-name-title").text === "Business name")
      assert(document.getElementById("business-name").text === "ACME")
      assert(document.getElementById("business-name-edit").attr("href") === "/business-customer/agent/register/non-uk-client/ATED/edit")

      And("Business address is correct")
      assert(document.getElementById("business-address-title").text === "Registered address")
      assert(document.getElementById("business-address").text ===
        "line 1 line 2 line 3 line 4 AA1 1AA United Kingdom of Great Britain and Northern Ireland (the)")
      assert(document.getElementById("business-reg-edit").attr("href") === "/business-customer/agent/register/non-uk-client/ATED/edit")

      And("Overseas tax reference is correct")
      assert(document.getElementById("registration-number-title").text === "Overseas company registration number")
      assert(document.getElementById("registration-number").text === "id")
      assert(document.getElementById("issuing-country-title").text === "Country that issued the number")
      assert(document.getElementById("issuing-country").text === "France")
      assert(document.getElementById("issuing-institution-title").text === "Institution that issued the number")
      assert(document.getElementById("issuing-institution").text === "inst")

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "Confirm")

      And("There is a link to the accessibility statement")
      assert(document.select(".govuk-footer__inline-list-item:nth-child(2) > a")
        .attr("href") === "http://localhost:12346/accessibility-statement/ated-subscription?referrerUrl=%2F")
    }

    Scenario("return Review Details view for an overseas agent, when agent can't be directly found with login credentials and the reg is editable") {

      Given("An agent has editable overseas business registration details")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages : play.api.i18n.Messages = play.api.i18n.MessagesImpl(Lang.defaultLang, messagesApi)

      val html = injectedViewInstance("ATED", reviewDetailsOverseas.copy(directMatch = false), Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Confirm your business details")
      assert(document.select("h1").text contains "Check your agency details")

      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: ATED agency set up")

      And("The confirmation notice should display")
      assert(document.getElementById("check-agency-details").text ===
        "! Warning You are setting up your agency. These should be your company details not your client’s.")

      And("Business name is correct")

      assert(document.getElementById("business-name-title").text === "Business name")
      assert(document.getElementById("business-name").text === "OverseasAgentRegistration")
      assert(document.getElementById("business-name-edit").attr("href") === "/business-customer/agent/register/non-uk-client/ATED/edit")

      And("Business address is correct")
      assert(document.getElementById("business-address-title").text === "Registered address")
      assert(document.getElementById("business-address").text === "line 1 line 2 line 3 line 4 Côte d'Ivoire")
      assert(document.getElementById("business-reg-edit").attr("href") === "/business-customer/agent/register/non-uk-client/ATED/edit")

      And("Overseas tax reference is correct")
      assert(document.getElementById("registration-number-title").text === "Overseas company registration number")
      assert(document.getElementById("registration-number").text === "id")
      assert(document.getElementById("issuing-country-title").text === "Country that issued the number")
      assert(document.getElementById("issuing-country").text === "Côte d'Ivoire")
      assert(document.getElementById("issuing-institution-title").text === "Institution that issued the number")
      assert(document.getElementById("issuing-institution").text === "inst")

      And("The submit button is - Confirm")
      assert(document.getElementById("submit").text() === "Confirm")

      And("There is a link to the accessibility statement")
      assert(document.select(".govuk-footer__inline-list-item:nth-child(2) > a")
        .attr("href") === "http://localhost:12346/accessibility-statement/ated-subscription?referrerUrl=%2F")
    }
  }
}