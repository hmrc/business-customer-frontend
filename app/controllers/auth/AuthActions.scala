/*
 * Copyright 2019 HM Revenue & Customs
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
import models.StandardAuthRetrievals
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import utils.ValidateUri

import scala.concurrent.{ExecutionContext, Future}


trait AuthActions extends AuthorisedFunctions {

  implicit val appConfig: ApplicationConfig
  lazy val loginURL: String = appConfig.loginURL

  def continueURL(serviceName: String): String = appConfig.continueURL(serviceName)

  def loginParams(serviceName: String)(implicit request: Request[AnyContent]): Map[String, Seq[String]] = Map(
    "continue" -> Seq(loginURL),
    "origin" -> Seq(continueURL(serviceName))
  )

  private def isValidUrl(serviceName: String): Boolean = {
    ValidateUri.isValid(appConfig.serviceList, serviceName)
  }

  private def recoverAuthorisedCalls(serviceName: String)(implicit request: Request[AnyContent], messages: Messages): PartialFunction[Throwable, Result] = {
    case e: NoActiveSession        =>
      Logger.warn(s"[recoverAuthorisedCalls] NoActiveSession: $e")
      Redirect(loginURL, loginParams(serviceName))
    case e: AuthorisationException =>
      Logger.error(s"[recoverAuthorisedCalls] Auth exception: $e")
      Redirect(controllers.routes.ApplicationController.unauthorised())
  }

  def authorisedFor(serviceName: String)(body: StandardAuthRetrievals => Future[Result])
                   (implicit req: Request[AnyContent], ec: ExecutionContext, hc: HeaderCarrier, messages: Messages): Future[Result] = {
    val newHC = if(hc.authorization.isEmpty) hc.copy(authorization = Some(Authorization(req.headers.get("Authorization").get))) else hc
    if (!isValidUrl(serviceName)) {
      Logger.info(s"[authorisedFor] Given invalid service name of $serviceName")
      Future.successful(Results.NotFound)
    } else {
      authorised((AffinityGroup.Organisation or AffinityGroup.Agent or Enrolment("IR-SA")) and ConfidenceLevel.L50)
        .retrieve(allEnrolments and affinityGroup and credentials and groupIdentifier) {
          case Enrolments(enrolments) ~ affGroup ~ creds ~ groupId => body(StandardAuthRetrievals(enrolments, affGroup, creds, groupId))
        }(newHC, ec) recover recoverAuthorisedCalls(serviceName)
    }
  }
}
