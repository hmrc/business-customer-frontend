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

package controllers.auth

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class BusinessCustomerRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val serviceName: String = "ATED"

  "BusinessCustomerRegime" must {

    "define isAuthorised" must {

      val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

      "return true when the user is registered for Org account" in {
        when(accounts.org.isDefined).thenReturn(true)
        BusinessCustomerRegime(serviceName).isAuthorised(accounts) must be(true)
      }

      "return true when the user is an agent" in {
        when(accounts.agent.isDefined).thenReturn(true)
        BusinessCustomerRegime(serviceName).isAuthorised(accounts) must be(true)
      }

      "return false when the user is not registered for Org account" in {
        when(accounts.org.isDefined).thenReturn(false)
        when(accounts.agent.isDefined).thenReturn(false)
        BusinessCustomerRegime(serviceName).isAuthorised(accounts) must be(false)
      }

    }

    "define the authentication type as the BusinessCustomer GG" in {
      BusinessCustomerRegime(serviceName).authenticationType must be(BusinessCustomerGovernmentGateway(serviceName))
    }

    "define the unauthorised landing page as /unauthorised" in {
      BusinessCustomerRegime(serviceName).unauthorisedLandingPage.isDefined must be(true)
      BusinessCustomerRegime(serviceName).unauthorisedLandingPage.get must be("/business-customer/unauthorised")
    }

  }
  
}
