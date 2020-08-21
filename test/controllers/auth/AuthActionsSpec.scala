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

package controllers.auth

import config.ApplicationConfig
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result, Results}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits, Injecting}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthActionsSpec extends PlaySpec with MockitoSugar with GuiceOneServerPerSuite with FutureAwaits with DefaultAwaitTimeout with Injecting {

  val mockAppConfig: ApplicationConfig = inject[ApplicationConfig]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  class Setup {
    val authActionsHarness: AuthActions = new AuthActions {
      override implicit val appConfig: ApplicationConfig = mockAppConfig

      override def authConnector: AuthConnector = mockAuthConnector
    }
  }

  type RetType =
    Enrolments ~
    Option[AffinityGroup] ~
    Option[Credentials] ~
    Option[String]

  def authRetrieval(affinityGroup : AffinityGroup = AffinityGroup.Organisation): RetType =
    new ~(
      new ~(
        new ~(
          Enrolments(Set()),
          Some(affinityGroup)
        ),
        Some(Credentials("cred", "provType"))
      ),
      Some("groupId")
    )

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("fake")))
  implicit val fq: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = mock[Messages]

  "authorisedFor" should {
    "authorise for a user" when {
      "the user has valid enrolments" in new Setup {
        when(mockAuthConnector.authorise[RetType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(authRetrieval()))

        val authFor: Result = await(authActionsHarness.authorisedFor("ated") {
          _ => Future.successful(Results.Ok)
        })

        authFor.header.status mustBe 200
      }
    }

    "unauthorise for a user" when {
      "the user has the incorrect affinity group" in new Setup {
        when(mockAuthConnector.authorise[RetType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.failed(UnsupportedAffinityGroup("test")))

        val authFor: Result = await(authActionsHarness.authorisedFor("ated") {
          _ => Future.successful(Results.Ok)
        })

        authFor.header.status mustBe 303
      }

      "the user is not authorised" in new Setup {
        when(mockAuthConnector.authorise[RetType](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.failed(MissingBearerToken("test")))

        val authFor: Result = await(authActionsHarness.authorisedFor("ated") {
          _ => Future.successful(Results.Ok)
        })

        authFor.header.status mustBe 303
      }
    }
  }

}
