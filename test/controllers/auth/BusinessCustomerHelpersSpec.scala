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

package controllers.auth

import builders.AuthBuilder
import org.scalatestplus.play.PlaySpec

class BusinessCustomerHelpersSpec extends PlaySpec {

  "AuthLink" must {
    "Return the orgs link if we have org authorisation" in {
      implicit val aut = AuthBuilder.createUserAuthContext("userId", "TEST NAME")
      aut.user.authLink must startWith("org")
    }

    "Return the sa link if we have an sa individual" in {
      implicit val aut = AuthBuilder.createSaAuthContext("agentId", "Agent TEST NAME")
      aut.user.authLink must startWith("sa")
      aut.user.authLink mustNot startWith("sa/individual")
    }


    "Return the agent link if we have agent admin authorisation" in {
      implicit val aut = AuthBuilder.createAgentAuthContext("agentId", "Agent TEST NAME")
      aut.user.authLink must startWith("agent")
    }

    "Return the agent link if we have agent assistant authorisation" in {
      implicit val aut = AuthBuilder.createAgentAssistantAuthContext("agentId", "Agent TEST NAME")
      aut.user.authLink must startWith("agent")
    }

    "throws an exception if the user does not have the correct authorisation" in {
      implicit val aut = AuthBuilder.createInvalidAuthContext("userId", "TEST NAME")

      val thrown = the[RuntimeException] thrownBy aut.user.authLink
      thrown.getMessage must include("User does not have the correct authorisation")
    }
  }

  "isAgent" must {
    "Return true if this user is an agent" in {
      implicit val auth = AuthBuilder.createAgentAuthContext("agentId", "Agent TEST NAME")
      auth.user.isAgent must be(true)
    }

    "Return false if this user is not an agent" in {
      implicit val auth = AuthBuilder.createUserAuthContext("userId", "TEST NAME")
      auth.user.isAgent must be(false)
    }
  }

}
