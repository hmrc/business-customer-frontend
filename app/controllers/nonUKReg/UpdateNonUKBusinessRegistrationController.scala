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

package controllers.nonUKReg

import config.{ApplicationConfig, FrontendAuthConnector}
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistrationDisplayDetails}
import play.api.Mode.Mode
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.{Configuration, Logger, Play}
import services.BusinessRegistrationService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils

import scala.concurrent.Future

object UpdateNonUKBusinessRegistrationController extends UpdateNonUKBusinessRegistrationController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  // $COVERAGE-ON$
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait UpdateNonUKBusinessRegistrationController extends BaseController with RunMode  {

  def businessRegistrationService: BusinessRegistrationService

  def editAgent(service: String) = AuthAction(service).async { implicit bcContext =>
    businessRegistrationService.getDetails.map{
      businessDetails =>
        businessDetails match {
          case Some(detailsTuple) =>
            Ok(views.html.nonUkReg.update_business_registration(businessRegistrationForm.fill(detailsTuple._2),
              service,
              displayDetails(service, false),
              None,
              false,
              getBackLink(service, None),
              bcContext.user.isAgent))
          case _ =>
            Logger.warn(s"[UpdateNonUKBusinessRegistrationController][editAgent] - No registration details found to edit")
            throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
        }
    }
  }

  def edit(service: String, redirectUrl : Option[ContinueUrl]) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        businessRegistrationService.getDetails.map {
          businessDetails =>
            businessDetails match {
              case Some(detailsTuple) =>
                Ok(views.html.nonUkReg.update_business_registration(businessRegistrationForm.fill(detailsTuple._2),
                  service,
                  displayDetails(service, true),
                  redirectUrl,
                  true,
                  getBackLink(service, redirectUrl),
                  bcContext.user.isAgent))
              case _ =>
                Logger.warn(s"[UpdateNonUKBusinessRegistrationController][edit] - No registration details found to edit")
                throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
            }
        }
    }
  }

  def update(service: String, redirectUrl : Option[ContinueUrl], isRegisterClient: Boolean) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, bcContext.user.isAgent).fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.nonUkReg.update_business_registration(formWithErrors,
              service,
              displayDetails(service, isRegisterClient),
              redirectUrl,
              isRegisterClient,
              getBackLink(service, redirectUrl),
              bcContext.user.isAgent)))
          },
          registerData => {
            businessRegistrationService.getDetails.flatMap {
              businessDetails =>
                businessDetails match {
                  case Some(detailsTuple) =>
                    businessRegistrationService.updateRegisterBusiness(registerData, detailsTuple._3, isGroup = false, isNonUKClientRegisteredByAgent = true, service, isBusinessDetailsEditable = true).map { response =>
                      redirectUrl match {
                        case Some(serviceUrl) => Redirect(serviceUrl.url)
                        case _ => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
                      }
                    }
                  case _ =>
                    Logger.warn(s"[UpdateNonUKBusinessRegistrationController][update] - No registration details found to edit")
                    throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
                }
            }
          }
        )
    }
  }

  private def getBackLink(service: String, redirectUrl: Option[ContinueUrl]) = {
    redirectUrl match {
      case Some(x) => Some(x.url)
      case None => Some(controllers.routes.ReviewDetailsController.businessDetails(service).url)
    }
  }

  private def displayDetails(service: String, isRegisterClient: Boolean)(implicit bcContext: BusinessCustomerContext) = {
    if (bcContext.user.isAgent){
      if (isRegisterClient) {
        BusinessRegistrationDisplayDetails("NUK",
          Messages("bc.non-uk-reg.header"),
          Messages("bc.non-uk-reg.sub-header"),
          Some(Messages("bc.non-uk-reg.lede.update-text")),
          BCUtils.getIsoCodeTupleList)
      } else {
          BusinessRegistrationDisplayDetails("NUK",
            Messages("bc.business-registration.agent.non-uk.header"),
            Messages("bc.business-registration.text.agent", service),
            None,
            BCUtils.getIsoCodeTupleList)
      }
    }
    else {
      BusinessRegistrationDisplayDetails("NUK",
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        Some(Messages("bc.business-registration.lede.update-text")),
        BCUtils.getIsoCodeTupleList)
    }
  }

}
