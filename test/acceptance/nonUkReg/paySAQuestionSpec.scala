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

package acceptance.nonUkReg

import config.ApplicationConfig
import forms.BusinessRegistrationForms._
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.test.{FakeRequest, Injecting}

class paySAQuestionSpec extends FeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with Injecting {

  val service = "ATED"
  val injectedViewInstance = inject[views.html.nonUkReg.paySAQuestion]
  implicit val lang = Lang.defaultLang
  implicit val appConfig = inject[ApplicationConfig]

  feature("The user can the pay SA question") {

    info("as a user i want to be able to view the Pay SA question page")

    scenario("return Pay SA Question view for a user") {

      Given("client has directly matched a business registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages : play.api.i18n.Messages = play.api.i18n.MessagesImpl(Lang.defaultLang, messagesApi)

      val html = injectedViewInstance(paySAQuestionForm, service, Some("backLinkUri"), "statementUrl")

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Do you pay tax in the UK through Self Assessment?")
      assert(document.select("h1").text === ("Do you pay tax in the UK through Self Assessment?"))

      Then("The subheader should be - ATED registration")
      assert(document.getElementById("paySa-subheader").text() === "This section is: ATED registration")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

      And("The submit button is - continue")
      assert(document.getElementById("submit").text() === "Continue")

      And("There is a link to the accessibility statement")
      assert(document.select("#footer > div > div > div.footer-meta-inner > ul > li:nth-child(2) > a")
        .attr("href") === "http://localhost:12346/accessibility-statement/ated-subscription?referrerUrl=statementUrl")
    }
  }
}
