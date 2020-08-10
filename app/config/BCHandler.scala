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

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.SessionUtils

class BCHandlerImpl @Inject()(val messagesApi: MessagesApi,
                              config: ApplicationConfig) extends BCHandler {
  lazy val appConfig: ApplicationConfig = config
}

trait BCHandler extends FrontendErrorHandler with I18nSupport {
  implicit val appConfig: ApplicationConfig

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    val service = SessionUtils.findServiceInRequest(request)

    appConfig.templateError(pageTitle, heading, message, service, appConfig)
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
    appConfig.templateError(
      Messages("bc.generic.error.title"),
      Messages("bc.generic.error.header"),
      Messages("bc.generic.error.message"),
      SessionUtils.findServiceInRequest(request),
      appConfig)
  }
}
