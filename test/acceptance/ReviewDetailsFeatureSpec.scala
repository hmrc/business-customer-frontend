/*
 * Copyright 2019 HM Revenue & Customs
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

import models.{ReviewDetails, Address}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class ReviewDetailsFeatureSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "GB")
  val reviewDetails = ReviewDetails("ACME", Some("Limited"), address, "sap123", "safe123", isAGroup = false, directMatch = true, Some("agent123"))

  feature("The user can view the review details page") {

    info("as a user i want to be able to view my review details page")

    scenario("return Review Details view for a user, when we directly found this user") {

      Given("client has directly matched a business registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.review_details("ATED", isAgent = false, reviewDetails, Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      And("The submit button is - Confirm and continue")
      assert(document.getElementById("submit").text() === "Confirm")

      Then("The title should match - Confirm your business details")
      assert(document.select("h1").text === ("Check this is the business you want to register"))

      assert(document.getElementById("bc.business-registration.text").text() === ("This section is: ATED registration"))
      assert(document.getElementById("business-name").text === ("ACME"))
      assert(document.getElementById("business-address").text === ("line 1 line 2 line 3 line 4 AA1 1AA United Kingdom"))
      assert(document.getElementById("wrong-account-title").text === ("Not the right details?"))
      assert(document.getElementById("wrong-account-text").text === ("If this is not the right business, you should sign out and change to another account"))
      assert(document.getElementById("wrong-account-text-item-1").text().startsWith("If you are registered with Companies House, you must tell Companies House about changes to your details.") === true)
      assert(document.getElementById("wrong-account-text-item-2").text().startsWith("If you are not registered with Companies House, you must tell HMRC about a change to your personal details.") === true)
      assert(document.getElementById("check-agency-details") === null)

      assert(document.select(".button").text === ("Confirm"))
      assert(document.getElementById("bus-reg-edit") === null)
    }

    scenario("return Review Details view for a user, when user can't be directly found with login credentials") {

      Given("An agent has an editable business registration details")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.review_details("ATED", isAgent = false, reviewDetails.copy(directMatch = false), Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      And("The submit button is - Confirm and continue")
      assert(document.getElementById("submit").text() === "Confirm")

      Then("The title should match - Confirm your business details ")
      assert(document.select("h1").text === ("Check this is the business you want to register"))

      assert(document.getElementById("bc.business-registration.text").text() === ("This section is: ATED registration"))
      assert(document.getElementById("business-name").text === ("ACME"))
      assert(document.getElementById("business-address").text === ("line 1 line 2 line 3 line 4 AA1 1AA United Kingdom"))
      assert(document.getElementById("wrong-account-title").text === ("Not the right address?"))
      assert(document.getElementById("wrong-account-text").text === ("You will need to update your information outside of this service."))
      assert(document.getElementById("wrong-account-text-item-1").text().startsWith("If you are registered with Companies House, you must tell Companies House about changes to your details.") === true)
      assert(document.getElementById("wrong-account-text-item-2").text().startsWith("If you are not registered with Companies House, you must tell HMRC about a change to your personal details.") === true)
      assert(document.getElementById("check-agency-details") === null)

      assert(document.select(".button").text === ("Confirm"))
      assert(document.getElementById("bus-reg-edit") === null)
    }

    scenario("return Review Details view for an agent, when we directly found this") {

      Given("An agent has an editable business registration details")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.review_details("ATED", isAgent = true, reviewDetails.copy(directMatch = true), Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      And("The submit button is - Confirm and continue")
      assert(document.getElementById("submit").text() === "Confirm")

      Then("The title should match - Confirm your agency's details")
      assert(document.select("h1").text === ("Confirm your agency’s details"))

      assert(document.getElementById("wrong-account-title").text === ("Not the right details?"))
      assert(document.getElementById("bc.business-registration-agent.text").text() === ("This section is: ATED agency set up"))
      assert(document.getElementById("business-name").text === ("ACME"))
      assert(document.getElementById("business-address").text === ("line 1 line 2 line 3 line 4 AA1 1AA United Kingdom"))
      assert(document.select(".button").text === ("Confirm"))
      assert(document.getElementById("bus-reg-edit") === null)


    }
  }
}
