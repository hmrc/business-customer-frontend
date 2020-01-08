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

package controllers.nonUKReg

import config.ApplicationConfig
import controllers.auth.AuthActions
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import javax.inject.Inject
import models.{BusinessRegistrationDisplayDetails, StandardAuthRetrievals}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class UpdateNonUKBusinessRegistrationController @Inject()(val authConnector: AuthConnector,
                                                          config: ApplicationConfig,
                                                          businessRegistrationService: BusinessRegistrationService,
                                                          mcc: MessagesControllerComponents) extends FrontendController(mcc) with AuthActions {
  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def editAgent(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      businessRegistrationService.getDetails.map {
        case Some(detailsTuple) =>
          Ok(views.html.nonUkReg.update_business_registration(
            businessRegistrationForm.fill(detailsTuple._2),
            service,
            displayDetails(service, isRegisterClient = false),
            None,
            isRegisterClient = false,
            getBackLink(service, None),
            authContext.isAgent))
        case _ =>
          Logger.warn(s"[UpdateNonUKBusinessRegistrationController][editAgent] - No registration details found to edit")
          throw new RuntimeException("No registration details found")
      }
    }
  }

  def edit(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      redirectUrl match {
        case Some(x) if !appConfig.isRelativeOrDev(x) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
        case _ =>
          businessRegistrationService.getDetails.map {
            case Some(detailsTuple) =>
              Ok(views.html.nonUkReg.update_business_registration(
                businessRegistrationForm.fill(detailsTuple._2),
                 service,
                 displayDetails(service, isRegisterClient = true),
                 redirectUrl,
                isRegisterClient = true,
                 getBackLink(service, redirectUrl),
                 authContext.isAgent))
            case _ =>
              Logger.warn(s"[UpdateNonUKBusinessRegistrationController][edit] - No registration details found to edit")
              throw new RuntimeException("No registration details found")
          }
      }
    }
  }

  def update(service: String, redirectUrl: Option[String], isRegisterClient: Boolean): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      redirectUrl match {
        case Some(x) if !appConfig.isRelativeOrDev(x) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
        case _ =>
          BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, authContext.isAgent, appConfig).fold(
            formWithErrors => {
              Future.successful(BadRequest(views.html.nonUkReg.update_business_registration(formWithErrors,
                service,
                displayDetails(service, isRegisterClient),
                redirectUrl,
                isRegisterClient,
                getBackLink(service, redirectUrl),
                authContext.isAgent)))
            },
            registerData => {
              businessRegistrationService.getDetails.flatMap {
                case Some(detailsTuple) =>
                  businessRegistrationService.updateRegisterBusiness(
                    registerData,
                    detailsTuple._3,
                    isGroup = false,
                    isNonUKClientRegisteredByAgent = true,
                    service,
                    isBusinessDetailsEditable = true
                  ).map { _ =>
                    redirectUrl match {
                      case Some(url) => Redirect(url)
                      case _ => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
                    }
                  }
                case _ =>
                  Logger.warn(s"[UpdateNonUKBusinessRegistrationController][update] - No registration details found to edit")
                  throw new RuntimeException("No registration details found")
              }
            }
          )
      }
    }
  }

  private def getBackLink(service: String, redirectUrl: Option[String]): Some[String] = {
    redirectUrl match {
      case Some(url) => Some(url)
      case None => Some(controllers.routes.ReviewDetailsController.businessDetails(service).url)
    }
  }

  private def displayDetails(service: String, isRegisterClient: Boolean)
                            (implicit authContext: StandardAuthRetrievals): BusinessRegistrationDisplayDetails = {
    (authContext.isAgent, isRegisterClient) match {
      case (true, true)  =>
        BusinessRegistrationDisplayDetails(
          "NUK",
          "bc.non-uk-reg.header",
          "bc.non-uk-reg.sub-header",
          Some("bc.non-uk-reg.lede.update-text"),
          appConfig.getIsoCodeTupleList)
      case (true, false) =>
        BusinessRegistrationDisplayDetails(
          "NUK",
          "bc.business-registration.agent.non-uk.header",
          "bc.business-registration.text.agent",
          None,
          appConfig.getIsoCodeTupleList)
      case _ =>
        BusinessRegistrationDisplayDetails(
          "NUK",
          "bc.business-registration.user.non-uk.header",
          "bc.business-registration.text.client",
          Some("bc.business-registration.lede.update-text"),
          appConfig.getIsoCodeTupleList)
    }
  }

}
