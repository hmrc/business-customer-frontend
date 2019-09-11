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

package controllers

import audit.Auditable
import config.ApplicationConfig
import models.FeedBack
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._

class ApplicationControllerSpec extends PlaySpec with OneServerPerSuite {
  val service = "ATED"

  implicit val lang = Lang.defaultLang
  implicit val appConfig = app.injector.instanceOf[ApplicationConfig]

  trait Setup {
    val controller = new ApplicationController(
      appConfig,
      app.injector.instanceOf[Auditable],
      app.injector.instanceOf[MessagesControllerComponents]
    )
  }

  "ApplicationController" must {

    "unauthorised" must {

      "respond with an OK" in new Setup {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val result = controller.unauthorised().apply(FakeRequest())
        status(result) must equal(OK)
      }

      "load the unauthorised page" in new Setup {
        implicit val messages : play.api.i18n.Messages = play.api.i18n.Messages.Implicits.applicationMessages
        val result = controller.unauthorised().apply(FakeRequest())
        val content = contentAsString(result)
        val doc = Jsoup.parse(content)
        doc.title() must be(Messages("bc.unauthorised.title").concat(" - GOV.UK"))
      }

    }

    "Cancel" must {

      "respond with a redirect" in new Setup {
        val result = controller.cancel().apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the login page" in new Setup {
        val result = controller.cancel().apply(FakeRequest())
        redirectLocation(result).get must include("https://www.gov.uk/")
      }

    }

    "Not the right business link" must {

      "respond with a redirect" in new Setup {
        val result = controller.logoutAndRedirectToHome(service).apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to login page" in new Setup {
        val result = controller.logoutAndRedirectToHome(service).apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/agent/ATED")
      }

    }

    "Keep Alive" must {

      "respond with an OK" in new Setup {
        val result = controller.keepAlive.apply(FakeRequest())

        status(result) must be(OK)
      }
    }
    
    "Logout" must {

      "respond with a redirect" in new Setup {
        val result = controller.logout(service).apply(FakeRequest())
        status(result) must be(SEE_OTHER)
      }

      "be redirected to the feedback page for ATED service" in new Setup {
        val result = controller.logout(service).apply(FakeRequest())
        redirectLocation(result).get must include("/ated/logout")
      }

      "be redirected to the logout page for any other service other than ATED" in new Setup {
        val result = controller.logout("AWRS").apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/signed-out")
      }

      "send to signed out page" in new Setup {
        val result = controller.signedOut().apply(FakeRequest())
        status(result) must be(OK)
      }

    }

    "feedback" must {
      "case service name = ATED, redirected to the feedback page" in new Setup {
        val result = controller.feedback(service).apply(FakeRequest())
        status(result) must be(OK)

      }

      "be redirected to the logout page for any other service other than ATED" in new Setup {
        val result = controller.feedback("AWRS").apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/signed-out")
      }

    }

    "submit feedback" must {
      "case service name = ATED, tbe redirected to the feedback page" in new Setup {
        val result = controller.submitFeedback(service).apply(FakeRequest())
        status(result) must be(SEE_OTHER)

      }

      "respond with BadRequest, for invalid submit"  in new Setup {
        val feedback = FeedBack(easyToUse = None, satisfactionLevel = None, howCanWeImprove = Some("A"*1201), referer = None)
        val testJson = Json.toJson(feedback)
        val result = controller.submitFeedback(service).apply(FakeRequest().withJsonBody(testJson))
        status(result) must be(BAD_REQUEST)
      }

      "be redirected to the logout page for any other service other than ATED" in new Setup {
        val result = controller.submitFeedback("AWRS").apply(FakeRequest())
        redirectLocation(result).get must include("/business-customer/thank-you/AWRS")
      }

    }

    "feedback thank you" must {
      "case service name = ATED, be redirected to the feedback page" in new Setup {
        val result = controller.feedbackThankYou(service).apply(FakeRequest())
        status(result) must be(OK)

      }


    }

  }
}
