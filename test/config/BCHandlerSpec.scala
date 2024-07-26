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

/*
package config

import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import views.html.global_error

class BCHandlerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with Injecting {

  implicit val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  implicit val appConfig: ApplicationConfig = inject[ApplicationConfig]
  val injectedViewInstanceError: global_error = inject[views.html.global_error]

  "internalServerErrorTemplate" must {

    "retrieve the correct messages" in {
      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val errorHandler = new BCHandlerImpl(mcc.messagesApi,injectedViewInstanceError, appConfig)
      val result = errorHandler.internalServerErrorTemplate
      val document = Jsoup.parse(contentAsString(result))

      document.title() must be("Sorry, there is a problem with the service - GOV.UK")
      document.getElementsByTag("h1").text() must include("Sorry, there is a problem with the service")
      document.getElementsByTag("p").text() must be("Try again later.")
    }
  }
  "calling onClientError for a page not found" must {

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    val errorHandler = new BCHandlerImpl(mcc.messagesApi,injectedViewInstanceError, appConfig)
    val result = errorHandler.notFoundTemplate
    val document = Jsoup.parse(contentAsString(result))

      "render page in English" in {
        document.title must be("Page not found - 404 - GOV.UK")
      }
    }
  }

 */