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
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.OverseasCompanyUtils

import scala.concurrent.{ExecutionContext, Future}

class UpdateOverseasCompanyRegController @Inject()(val authConnector: AuthConnector,
                                                   config: ApplicationConfig,
                                                   template: views.html.nonUkReg.update_overseas_company_registration,
                                                   businessRegistrationService: BusinessRegistrationService,
                                                   mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with AuthActions with OverseasCompanyUtils {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def viewForUpdate(service: String, addClient: Boolean, redirectUrl: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service){ implicit authContext =>
      redirectUrl match {
        case Some(x) if !appConfig.isRelative(x) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
        case _ =>
          Ok(template(overseasCompanyForm,
             service,
             displayDetails(authContext.isAgent, addClient, service),
            appConfig.getIsoCodeTupleList,
             redirectUrl,
             getBackLink(service, redirectUrl)))

          businessRegistrationService.getDetails.map {
            case Some(detailsTuple) =>
              Ok(template(overseasCompanyForm.fill(detailsTuple._3),
                 service,
                 displayDetails(authContext.isAgent, addClient, service),
                appConfig.getIsoCodeTupleList,
                 redirectUrl,
                 getBackLink(service, redirectUrl)))
            case _ =>
              Logger.warn(s"[UpdateOverseasCompanyRegController][viewForUpdate] - No registration details found to edit")
              throw new RuntimeException("No registration details found")
          }
      }
    }
  }


  def update(service: String, addClient: Boolean, redirectUrl: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service){ implicit authContext =>
      redirectUrl match {
        case Some(x) if !appConfig.isRelative(x) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
        case _ =>
          BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
            formWithErrors => {
              Future.successful(BadRequest(template(
                formWithErrors,
                service,
                displayDetails(authContext.isAgent, addClient, service),
                appConfig.getIsoCodeTupleList,
                redirectUrl,
                getBackLink(service, redirectUrl)))
              )
            },
            overseasCompany =>
              businessRegistrationService.getDetails.flatMap {
                case Some(detailsTuple) =>
                  businessRegistrationService.updateRegisterBusiness(
                    detailsTuple._2,
                    overseasCompany,
                    isGroup = false,
                    isNonUKClientRegisteredByAgent = addClient,
                    service,
                    isBusinessDetailsEditable = true
                  ).map { _ =>
                    Redirect(redirectUrl.getOrElse(controllers.routes.ReviewDetailsController.businessDetails(service).url))
                  }
                case _ =>
                  Logger.warn(s"[UpdateOverseasCompanyRegController][update] - No registration details found to edit")
                  throw new RuntimeException("No registration details found")
              }
            )
      }
    }
  }

  private def getBackLink(service: String, redirectUrl: Option[String]): Some[String] = {
    Some(redirectUrl.getOrElse(controllers.routes.ReviewDetailsController.businessDetails(service).url))
  }
}
