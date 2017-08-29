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
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FeatureSpec, GivenWhenThen}
import org.scalatestplus.play.OneServerPerSuite
import play.api.test.FakeRequest

class nrl_questionSpec extends FeatureSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach with GivenWhenThen{

  val service = "ATED"

  feature("The user can the nrl question") {

    info("as a user i want to be able to view the nrl question page")

    scenario("return NRL Question view for a user") {

      Given("client has directly matched a business registration")
      When("The user views the page")
      implicit val request = FakeRequest()
      implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages

      val html = views.html.nonUkReg.nrl_question(nrlQuestionForm, service, Some("backLinkUri"))

      val document = Jsoup.parse(html.toString())

      Then("The title should match - Are you a non-resident landlord?")
      assert(document.select("h1").text === ("Are you a non-resident landlord?"))

      Then("The subheader should be - ATED registration")
      assert(document.getElementById("client-appoint-subheader").text() === "This section is: ATED registration")

      Then("The options should be Yes and No")
      assert(document.select(".block-label").text() === "Yes No")

      And("The submit button is - continue")
      assert(document.getElementById("submit").text() === "Continue")
    }
  }
}
