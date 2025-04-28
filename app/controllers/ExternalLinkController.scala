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

package controllers

import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import controllers.auth.AuthActions
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class ExternalLinkController @Inject()(val authConnector: AuthConnector,
                                       val backLinkCacheConnector: BackLinkCacheConnector,
                                       config: ApplicationConfig,
                                       mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with BackLinkController with AuthActions with I18nSupport {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "ExternalLinkController"

  def backLink(serviceName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(serviceName){ implicit authContext =>
      currentBackLink.map(_.map(Redirect(_)).getOrElse(NoContent))
    }
  }
}
