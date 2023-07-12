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

package builders

import models.StandardAuthRetrievals
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.domain._

import scala.concurrent.Future

object AuthBuilder {

  val saUtr = new SaUtrGenerator().nextSaUtr
  val nino = new Generator().nextNino

  type RetrievalType = Enrolments ~
    Option[AffinityGroup] ~
    Option[Credentials] ~
    Option[String]

  def buildRetrieval(retrievals: StandardAuthRetrievals): RetrievalType = {
    new ~(
      new ~(
        new ~(
          Enrolments(retrievals.enrolments),
          retrievals.affinityGroup
        ),
        Some(Credentials("mockProvi", "type"))
      ),
      retrievals.groupId
    )
  }

  def createUserAuthContext(userId: String, userName: String): StandardAuthRetrievals = {
    val authData: StandardAuthRetrievals = StandardAuthRetrievals(
      Set(),
      Some(AffinityGroup.Organisation),
      None,
      None
    )

    authData
  }

  def createAgentAuthContext(userId: String, userName: String): StandardAuthRetrievals = {
    createAgentAuthority(agentRefNo = Some("JARN1234567"))
  }

  def createAgentAssistantAuthContext(userId: String, userName: String): StandardAuthRetrievals = {
    createAgentAuthority(Assistant, agentRefNo = Some("JARN1234567"))
  }

  def mergeAuthRetrievals(arone: StandardAuthRetrievals, artwo: StandardAuthRetrievals): StandardAuthRetrievals = {
    arone.copy(
      enrolments = arone.enrolments ++ artwo.enrolments
    )
  }

  def createSaUser(): StandardAuthRetrievals = {
    StandardAuthRetrievals(
      Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")),
      Some(AffinityGroup.Individual),
      Some(Credentials("provID", "provType")),
      Some("groupId")
    )
  }

  def createCtUser(): StandardAuthRetrievals = {
    StandardAuthRetrievals(
      Set(Enrolment("IR-CT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")),
      Some(AffinityGroup.Organisation),
      None,
      None
    )
  }

  def createOrgUser(): StandardAuthRetrievals = {
    StandardAuthRetrievals(
      Set(),
      Some(AffinityGroup.Organisation),
      None,
      None
    )
  }

  def mockAuthorisedUser(userId:String, mockAuthConnector: AuthConnector): Unit = {
    val authData: StandardAuthRetrievals = StandardAuthRetrievals(
      Set(),
      Some(AffinityGroup.Organisation),
      None,
      None
    )

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(authData)))
  }

  def mockAuthorisedSaUser(userId:String, mockAuthConnector: AuthConnector): Unit = {
    val authData: StandardAuthRetrievals = StandardAuthRetrievals(
      Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")),
      Some(AffinityGroup.Individual),
      None,
      None
    )

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(authData)))
  }

  def mockAuthorisedSaOrgUser(userId:String, mockAuthConnector: AuthConnector): Unit = {
    val authData: StandardAuthRetrievals = StandardAuthRetrievals(
      Set(Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated")),
      Some(AffinityGroup.Organisation),
      None,
      None
    )

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(authData)))
  }

  def mockAuthorisedAgent(userId:String, mockAuthConnector: AuthConnector): Unit = {
    val authData: StandardAuthRetrievals = createAgentAuthority(agentRefNo = Some("JARN1234567"))

    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(buildRetrieval(authData)))
  }

  def mockUnAuthorisedUser(userId:String, mockAuthConnector: AuthConnector): Unit = {
    when(mockAuthConnector.authorise[RetrievalType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(InsufficientEnrolments("message")))
  }

  def produceAgentEnrolment(agentRef: String): Enrolment = {
    Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", agentRef)), "Activated")
  }

  private def createAgentAuthority(agentRole: CredentialRole = User, agentRefNo: Option[String]): StandardAuthRetrievals = {

    val agentEnrolment: Option[Enrolment] = agentRefNo.map { agentRef =>
      produceAgentEnrolment(agentRef)
    }

    val authData = StandardAuthRetrievals(
      Set(agentEnrolment).flatten,
      Some(AffinityGroup.Agent),
      Some(Credentials("provId", "provType")),
      Some("groupID")
    )

    authData
  }

}
