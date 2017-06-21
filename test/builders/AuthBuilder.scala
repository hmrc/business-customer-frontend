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

package builders

import models.{BusinessCustomerUser, BusinessCustomerContext}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.FakeRequest
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import scala.concurrent.Future

object AuthBuilder {

  val saUtr = new SaUtrGenerator().nextSaUtr
  val nino = new Generator().nextNino

  def createUserAuthContext(userId: String, userName: String): BusinessCustomerContext = {
    val ac = AuthContext(authority = createUserAuthority(userId),  nameFromSession = Some(userName))
    BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(ac))
  }

  def createSaAuthContext(userId: String, userName: String): BusinessCustomerContext = {
    val ac = AuthContext(authority = createSaAuthority(userId), nameFromSession = Some(userName))
    BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(ac))
  }

  def createAgentAuthContext(userId: String, userName: String): BusinessCustomerContext = {
    val ac = AuthContext(authority = createAgentAuthority(userId, AgentAdmin), nameFromSession = Some(userName))
    BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(ac))
  }

  def createAgentAssistantAuthContext(userId: String, userName: String): BusinessCustomerContext = {
    val ac = AuthContext(authority = createAgentAuthority(userId, AgentAssistant), nameFromSession = Some(userName))
    BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(ac))
  }
  def createInvalidAuthContext(userId: String, userName: String): BusinessCustomerContext = {
    val ac = AuthContext(authority = createInvalidAuthority(userId),  nameFromSession = Some(userName))
    BusinessCustomerContext(FakeRequest(), BusinessCustomerUser(ac))
  }

  def mockAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createUserAuthority(userId)))
    }
  }

  def mockAuthorisedSaUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createSaAuthority(userId)))
    }
  }

  def mockAuthorisedSaOrgUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createSaOrgAuthority(userId)))
    }
  }

  def mockAuthorisedAgent(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createAgentAuthority(userId, AgentAdmin)))
    }
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector) {
    when(mockAuthConnector.currentAuthority(Matchers.any())) thenReturn {
      Future.successful(Some(createInvalidAuthority(userId)))
    }
  }

  private def createInvalidAuthority(userId: String): Authority = {
    Authority(userId, Accounts(paye = Some(PayeAccount(s"paye/$nino", nino))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

  private def createUserAuthority(userId: String): Authority = {
    Authority(userId, Accounts(org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

  private def createSaAuthority(userId: String): Authority = {
    Authority(userId, Accounts(sa = Some(SaAccount(s"sa/individual/$saUtr", saUtr))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

  private def createSaOrgAuthority(userId: String): Authority = {
    Authority(userId, Accounts(sa = Some(SaAccount("sa/individual/1234567890", saUtr)),
      org = Some(OrgAccount("org/1234", Org("1234")))), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

  private def createAgentAuthority(userId: String, agentRole : AgentRole): Authority = {
    val agentAccount = AgentAccount(link = "agent/1234",
      agentCode = AgentCode(""),
      agentUserId = AgentUserId(userId),
      agentUserRole = agentRole,
      payeReference = None)
    Authority(userId, Accounts(agent = Some(agentAccount)), None, None, CredentialStrength.Weak, ConfidenceLevel.L50, Some(""), Some(""), Some(""), "")
  }

}
