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

package controllers

import audit.Auditable
import config.ApplicationConfig
import javax.inject.Inject
import models.FeedBack
import models.FeedbackForm.feedbackForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, DiscardingCookie, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.EventTypes
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

class ApplicationController @Inject()(val config: ApplicationConfig,
                                      audit: Auditable,
                                      mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport {

  implicit val appConfig: ApplicationConfig = config
  lazy val serviceRedirectUrl: String = appConfig.conf.getConfString("cancelRedirectUrl", "https://www.gov.uk/")

  def unauthorised(): Action[AnyContent] = Action {
    implicit request =>
      Ok(views.html.unauthorised())
  }

  def cancel: Action[AnyContent] = Action { implicit request =>
    Redirect(serviceRedirectUrl)
  }

  def logout(service: String): Action[AnyContent] = Action { implicit request =>
    service.toUpperCase match {
      case "ATED" =>
        Redirect(appConfig.conf.getConfString(s"${service.toLowerCase}.logoutUrl", "/ated/logout")).withNewSession
      case _ =>
        Redirect(controllers.routes.ApplicationController.signedOut()).withNewSession
    }
  }

  def feedback(service: String): Action[AnyContent] = Action { implicit request =>
    service.toUpperCase match {
      case "ATED" =>
        Ok(views.html.feedback(feedbackForm.fill(FeedBack(referer = request.headers.get(REFERER))), service, appConfig.serviceWelcomePath(service)))
      case _ => Redirect(controllers.routes.ApplicationController.signedOut()).withNewSession
    }
  }

  def submitFeedback(service: String): Action[AnyContent] = Action { implicit request =>
    feedbackForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.feedback(formWithErrors, service, appConfig.serviceWelcomePath(service))),
      feedback => {
        def auditFeedback(feedBack: FeedBack)(implicit hc: HeaderCarrier): Unit = {
          audit.sendDataEvent(s"$service-exit-survey", detail = Map(
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

  def feedbackThankYou(service: String): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.feedbackThankYou(service, appConfig.serviceWelcomePath(service)))
  }
  def keepAlive: Action[AnyContent] = Action { implicit request => Ok("OK")}
  def signedOut: Action[AnyContent] = Action { implicit request => Ok(views.html.logout())}

  def logoutAndRedirectToHome(service: String): Action[AnyContent] = Action { implicit request =>
    Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }
}
