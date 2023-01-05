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

package config

import java.net.URLEncoder

import javax.inject.Inject
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.NotFound
import play.api.mvc.{Request, RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.SessionUtils

class BCHandlerImpl @Inject()(val messagesApi: MessagesApi,
                              val templateError: views.html.global_error,
                              config: ApplicationConfig) extends BCHandler {
  lazy val appConfig: ApplicationConfig = config
}

trait BCHandler extends FrontendErrorHandler with I18nSupport {
  implicit val appConfig: ApplicationConfig
  implicit val templateError: views.html.global_error

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    val service = SessionUtils.findServiceInRequest(request)

    templateError(pageTitle, heading, message, service, URLEncoder.encode(request.uri, "UTF8"))
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
    templateError(
      Messages("bc.generic.error.title"),
      Messages("bc.generic.error.header"),
      Messages("bc.generic.error.message"),
      SessionUtils.findServiceInRequest(request),
      URLEncoder.encode(request.uri, "UTF8")
  )
  }

  override def notFoundTemplate(implicit request: Request[_]): Html = {
    templateError(
      Messages("bc.notFound.error.title"),
      Messages("bc.notFound.error.header"),
      Messages("bc.notFound.error.message"),
      SessionUtils.findServiceInRequest(request),
      URLEncoder.encode(request.uri, "UTF8")
    )
  }

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = ex.getMessage match {
    case "Service name not found" => NotFound(notFoundTemplate(Request.apply(rh, "")))
    case _ => super.resolveError(rh, ex)
  }
}
