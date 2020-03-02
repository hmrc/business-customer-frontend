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

package config

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import org.mockito.Mockito._

class BCHandlerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockAppConfig = mock[ApplicationConfig]

  val bcHandler: BCHandler = new BCHandler {
    val messagesApi = mock[MessagesApi]
    val appConfig = mockAppConfig
  }

  "findServiceInRequest" should {
    "find a service in a request" when {
      "the url contains a service name" in {
        when(mockAppConfig.serviceList).thenReturn(List("ated"))

        val service = bcHandler.findServiceInRequest(
          FakeRequest(controllers.routes.BusinessVerificationController.businessVerification("ated"))
        )

        service mustBe "ated"
      }

      "the url contains a service name not at the end" in {
        when(mockAppConfig.serviceList).thenReturn(List("awrs", "ated"))

        val service = bcHandler.findServiceInRequest(
          FakeRequest(controllers.nonUKReg.routes.BusinessRegController.register("awrs", "businessType"))
        )

        service mustBe "awrs"
      }
    }

    "cannot find a service in a request" when {
      "the url does not contain the service name" in {
        when(mockAppConfig.serviceList).thenReturn(List("ated"))

        val service = bcHandler.findServiceInRequest(
          FakeRequest(controllers.routes.BusinessVerificationController.businessVerification("fakeservice"))
        )

        service mustBe "unknownservice"
      }
    }
  }

}
