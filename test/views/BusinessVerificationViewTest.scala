/*
 * Copyright 2025 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.BusinessVerificationForms.businessTypeForm
import org.jsoup.Jsoup
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.matchers.must.Matchers._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.business_verification


class BusinessVerificationViewTest
  extends AnyWordSpec
    with GuiceOneAppPerSuite {
  private val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(fakeRequest)
  private implicit val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  private val view: business_verification = app.injector.instanceOf[business_verification]
  private val defaultIsAgent = false
  private val agentCaption = "agency set up"
  private val userCaption = "registration"
  private val userHeading = "Your business"
  private val agentHeading = "What is the business type for your agency?"

  private def renderView(isAgent: Boolean): HtmlFormat.Appendable = {
    view(
      businessTypeForm = businessTypeForm,
      isAgent = isAgent,
      service = "ATED",
      isSaAccount = false,
      isOrgAccount = false,
      backLink = None
    )(fakeRequest, messages, appConfig)
  }
  "BusinessVerification view for user" should {
    val html = renderView(defaultIsAgent).body
    val doc  = Jsoup.parse(html)
    "have the correct page title" in {
      doc.title mustBe s"$userHeading - GOV.UK"
    }
    "display the correct caption" in {
      doc.select(".govuk-caption-xl").text must include(userCaption)
    }
    "display the correct heading" in {
      doc.select("h1").text must include(userHeading)
    }
    "include the lead paragraph" in {
      doc.select(".govuk-body").first().text must include("We will attempt to match your details")
    }
    "render business type radio options" in {
      val options = doc.select(".govuk-radios__item").eachText
      options must contain("Limited company")
      options must contain("Limited partnership")
    }
  }
  "BusinessVerification view for agent" should {
    val html = renderView(isAgent = true).body
    val doc  = Jsoup.parse(html)
    "have the correct page title" in {
      doc.title mustBe s"$agentHeading - GOV.UK"
    }
    "display the correct caption" in {
      doc.select(".govuk-caption-xl").text must include(agentCaption)
    }
  }
}