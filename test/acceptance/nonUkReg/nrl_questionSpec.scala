/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.MockitoSugar
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}
import views.html.nonUkReg.nrl_question

class nrl_questionSpec extends AnyFeatureSpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen with Injecting {

  val service = "ATED"
  val injectedViewInstance: nrl_question = inject[views.html.nonUkReg.nrl_question]

  implicit val lang: Lang = Lang.defaultLang
  implicit val appConfig: ApplicationConfig = inject[ApplicationConfig]

  Feature("The user can the nrl question") {

    info("as a user i want to be able to view the nrl question page")

    Scenario("return NRL Question view for a user") {

      Given("client has directly matched a business registration")
      When("The user views the page")
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val messagesApi: MessagesApi = inject[MessagesApi]
      implicit val messages : play.api.i18n.Messages = play.api.i18n.MessagesImpl(Lang.defaultLang, messagesApi)

      val html = injectedViewInstance(nrlQuestionForm, service, Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Are you a non-resident landlord?")
      assert(document.select("h1").text contains "Are you a non-resident landlord?")

      Then("The subheader should be - ATED registration")
      assert(document.getElementsByClass("govuk-caption-xl").text() === "This section is: ATED registration")

      Then("The options should be Yes and No")
      assert(document.select(".govuk-radios__item").text() === "Yes No")

      And("The submit button is - continue")
      assert(document.getElementById("submit").text() === "Continue")

      And("There is a link to the accessibility statement")
      assert(document.select(".govuk-footer__inline-list-item:nth-child(2) > a")
        .attr("href") === "http://localhost:12346/accessibility-statement/ated-subscription?referrerUrl=%2F")
    }
  }
}
