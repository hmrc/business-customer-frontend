/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.{NotFound, Ok}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.twirl.api.Html

import scala.concurrent.Future
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import utils.SessionUtils

import java.net.URLEncoder

class ErrorHandler @Inject()(val messagesApi: MessagesApi,
                             val templateError: views.html.global_error)
                            (implicit val configuration: ApplicationConfig, val ec: scala.concurrent.ExecutionContext) extends FrontendErrorHandler with I18nSupport {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)
                                    (implicit request: RequestHeader): Future[Html] = {
    val service = SessionUtils.findServiceInRequest(request)


    Future.successful(templateError(pageTitle, heading, message, service, URLEncoder.encode(request.uri, "UTF8")))
  }

  override def internalServerErrorTemplate(implicit request: RequestHeader): Future[Html] = {
    Future.successful(templateError(Messages("bc.generic.error.title"),
      Messages("bc.generic.error.header"),
      Messages("bc.generic.error.message"),
      "SessionUtils.findServiceInRequest(request)",
      URLEncoder.encode(request.uri, "UTF8")))
  }

  override def notFoundTemplate(implicit request: RequestHeader): Future[Html] = {
    Future.successful(templateError(
      Messages("bc.notFound.error.title"),
      Messages("bc.notFound.error.header"),
      Messages("bc.notFound.error.message"),
      SessionUtils.findServiceInRequest(request),
      URLEncoder.encode(request.uri, "UTF8"))
    )
  }

  override def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] = {
    ex.getMessage match {
      case "Service name not found" => //Future.successful(NotFound(notFoundTemplate(Request.apply(rh, ""))))
        notFoundTemplate(Request.apply(rh, "")).map(NotFound(_))

      case _ => super.resolveError(rh, ex)
    }
  }

  /*def resolveError(rh: RequestHeader, ex: Throwable): Future[Result] =
    ex match {
      case ApplicationException(result, _) => Future.successful(result)
      case _ =>
        internalServerErrorTemplate(rh)
          .map(html => InternalServerError(html).withHeaders(CACHE_CONTROL -> "no-cache"))
    }*/
}
