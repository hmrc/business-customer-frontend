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

package utils

import config.ApplicationConfig
import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrlPolicy.Id
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl, SafeRedirectUrl}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object RedirectUtils {

  def redirectUrlGetRelativeOrDev(redirectUrl: RedirectUrl)
                                 (implicit applicationConfig: ApplicationConfig): Id[SafeRedirectUrl] = {
    redirectUrl.get(OnlyRelative | AbsoluteWithHostnameFromAllowlist(applicationConfig.allowedHosts: _*))
  }

  def getRelativeOrBadRequest(redirectUrl: RedirectUrl)(action: String => Future[Result])
                             (implicit applicationConfig: ApplicationConfig): Future[Result] = {
    Try(redirectUrlGetRelativeOrDev(redirectUrl).url) match {
      case Success(value) =>
        action(value)
      case Failure(exception) =>
        Future.successful(BadRequest("The redirect url is not correctly formatted"))
    }
  }

  def getRelativeOrBadRequestOpt(redirectUrl: Option[RedirectUrl])(action: Option[String] => Future[Result])
                                (implicit applicationConfig: ApplicationConfig): Future[Result] = {
    Try(redirectUrl.map(redirectUrlGetRelativeOrDev(_).url)) match {
      case Success(value) =>
        action(value)
      case Failure(exception) =>
        Future.successful(BadRequest("The redirect url is not correctly formatted"))
    }
  }
}
