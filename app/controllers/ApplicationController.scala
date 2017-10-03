/*
 * Copyright 2017 HM Revenue & Customs
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

import audit.Auditable
import config.BusinessCustomerFrontendAuditConnector
import controllers.auth.ExternalUrls._
import models.FeedBack
import models.FeedbackForm.feedbackForm
import play.api.mvc.DiscardingCookie
import play.api.{Logger, Play}
import uk.gov.hmrc.play.audit.model.{Audit, EventTypes}
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.frontend.controller.{FrontendController, UnauthorisedAction}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier

object ApplicationController extends ApplicationController {
  override val audit: Audit = new Audit(AppName.appName, BusinessCustomerFrontendAuditConnector)
  override val appName: String = AppName.appName
}

trait ApplicationController extends FrontendController with RunMode with Auditable {

  import play.api.Play.current

  def unauthorised() = UnauthorisedAction {
    implicit request =>
      Ok(views.html.unauthorised())
  }

  def cancel = UnauthorisedAction { implicit request =>
    val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"cancelRedirectUrl")
    Redirect(serviceRedirectUrl.getOrElse("https://www.gov.uk/"))
  }

  def logout(service: String) = UnauthorisedAction { implicit request =>
    service.toUpperCase match {
      case "ATED" => {
        Redirect(Play.configuration.getString(s"services.${service.toLowerCase}.logoutUrl").getOrElse("/ated/logout")).withNewSession
      }
      case _ => Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
    }
  }

  def feedback(service: String) = UnauthorisedAction { implicit request =>
    service.toUpperCase match {
      case "ATED" => Ok(views.html.feedback(feedbackForm.fill(FeedBack(referer = request.headers.get(REFERER))), service))
      case _ => Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
    }
  }

  def submitFeedback(service: String) = UnauthorisedAction { implicit request =>
    feedbackForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.feedback(formWithErrors, service)),
      feedback => {
        def auditFeedback(feedBack: FeedBack)(implicit hc: HeaderCarrier) = {
          sendDataEvent(s"$service-exit-survey", detail = Map(
            "easyToUse" -> feedback.easyToUse.mkString,
            "satisfactionLevel" -> feedback.satisfactionLevel.mkString,
            "howCanWeImprove" -> feedback.howCanWeImprove.mkString,
            "referer" -> feedBack.referer.mkString,
            "status" ->  EventTypes.Succeeded
          ))
        }
        auditFeedback(feedback)
        Redirect(controllers.routes.ApplicationController.feedbackThankYou(service))
      }
    )
  }

  def feedbackThankYou(service: String) = UnauthorisedAction { implicit request =>
    Ok(views.html.feedbackThankYou(service))
  }

  def keepAlive = UnauthorisedAction { implicit request =>
    Ok("OK")
    }

  def signedOut = UnauthorisedAction { implicit request =>
    Ok(views.html.logout())
  }

  def logoutAndRedirectToHome(service: String) = UnauthorisedAction { implicit request =>
    Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }
}
