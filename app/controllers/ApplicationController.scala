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
import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, DiscardingCookie, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

class ApplicationController @Inject()(val config: ApplicationConfig,
                                      templateUnauthorised: views.html.unauthorised,
                                      templateLogout: views.html.logout,
                                      mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport {

  implicit val appConfig: ApplicationConfig = config
  lazy val serviceRedirectUrl: String = appConfig.conf.getConfString("cancelRedirectUrl", "https://www.gov.uk/")

  def unauthorised(): Action[AnyContent] = Action {
    implicit request =>
      Ok(templateUnauthorised())
  }

  def cancel: Action[AnyContent] = Action {
    Redirect(serviceRedirectUrl)
  }

  def logout(service: String): Action[AnyContent] = Action {
    redirectForSignedOut(service)
  }
  def timedOut(service: String): Action[AnyContent] = Action { implicit request =>
    redirectForSignedOut(service, true)
  }

  def redirectForSignedOut(service: String, redirectToTimeOut : Boolean = false) = {

    service.toUpperCase match {
         case "ATED" =>
           Redirect(appConfig.conf.getConfString(s"${service.toLowerCase}.logoutUrl", "/ated/logout")).withNewSession
         case "AWRS" if(redirectToTimeOut) =>
            Redirect(appConfig.conf.getConfString(s"${service.toLowerCase}.timedOutUrl", s"/awrs/timeOut")).withNewSession
         case "AWRS" =>
           Redirect(appConfig.conf.getConfString(s"${service.toLowerCase}.logoutUrl", s"//alcohol-wholesale-scheme/logout")).withNewSession
         case "AMLS" =>
           Redirect(config.signOut)
         case "FHDDS" =>
           Redirect(appConfig.conf.getConfString(s"${service.toLowerCase}.logoutUrl", s"/fhdds/sign-out")).withNewSession
         case _ =>
           Redirect(controllers.routes.ApplicationController.signedOut).withNewSession
       }

  }

  def keepAlive: Action[AnyContent] = Action { _ => Ok("OK")}
  def signedOut: Action[AnyContent] = Action { implicit request => Ok(templateLogout())}

  def logoutAndRedirectToHome(service: String): Action[AnyContent] = Action {
    Redirect(controllers.routes.HomeController.homePage(service)).discardingCookies(DiscardingCookie("mdtp"))
  }
}
