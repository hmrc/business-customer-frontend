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

import java.util.UUID

import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Headers}
import play.api.test.FakeRequest

object SessionBuilder {


  def updateRequestWithSession(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded], userId: String): FakeRequest[AnyContentAsFormUrlEncoded] = {
    val sessionId = s"session-${UUID.randomUUID}"
    fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
  }

  def buildRequestWithSession(userId: String): FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
  }

  def buildRequestWithSessionNoUser(): FakeRequest[AnyContentAsEmpty.type] = {
    val sessionId = s"session-${UUID.randomUUID}"
    FakeRequest().withSession(
      "sessionId" -> sessionId)
      .withHeaders(Headers("Authorization" -> "value"))
  }

}
