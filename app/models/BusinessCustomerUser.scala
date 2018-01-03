/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

case class BusinessCustomerContext(request: Request[AnyContent], user: BusinessCustomerUser)

case class BusinessCustomerUser(authContext: AuthContext) {

  def isAgent: Boolean = authContext.principal.accounts.agent.isDefined

  def isSa: Boolean = authContext.principal.accounts.sa.isDefined

  def isOrg: Boolean = authContext.principal.accounts.ct.isDefined || authContext.principal.accounts.org.isDefined

  def authLink: String = {
    (authContext.principal.accounts.org, authContext.principal.accounts.sa, authContext.principal.accounts.agent) match {
      case (Some(orgAccount), _, _) => orgAccount.link
      case (None, Some(saAccount), _) => saAccount.link.replaceAllLiterally("/individual", "")
      case (None, None, Some(agent)) => agent.link
      case _ => throw new RuntimeException("User does not have the correct authorisation")
    }
  }
}
