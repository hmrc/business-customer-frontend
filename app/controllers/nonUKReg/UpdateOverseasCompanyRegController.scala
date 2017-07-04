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

package controllers.nonUKReg

import config.{ApplicationConfig, FrontendAuthConnector}
import controllers.BaseController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{BCUtils, OverseasCompanyUtils}

import scala.concurrent.Future

object UpdateOverseasCompanyRegController extends UpdateOverseasCompanyRegController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  // $COVERAGE-ON$
}

trait UpdateOverseasCompanyRegController extends BaseController with RunMode {

  def businessRegistrationService: BusinessRegistrationService

  def viewForUpdate(service: String, addClient: Boolean, redirectUrl: Option[ContinueUrl] = None) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        Ok(views.html.nonUkReg.update_overseas_company_registration(overseasCompanyForm, service,
          OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, getBackLink(service, redirectUrl)))

        businessRegistrationService.getDetails.map {
          businessDetails =>
            businessDetails match {
              case Some(detailsTuple) =>
                Ok(views.html.nonUkReg.update_overseas_company_registration(overseasCompanyForm.fill(detailsTuple._3), service,
                  OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, getBackLink(service, redirectUrl)))
              case _ =>
                Logger.warn(s"[UpdateOverseasCompanyRegController][viewForUpdate] - No registration details found to edit")
                throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
            }
        }
    }
  }


  def update(service: String, addClient: Boolean, redirectUrl: Option[ContinueUrl] = None) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.nonUkReg.update_overseas_company_registration(formWithErrors, service,
              OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, getBackLink(service, redirectUrl))))
          },
          overseasCompany => {
            businessRegistrationService.getDetails.flatMap {
              businessDetails =>
                businessDetails match {
                  case Some(detailsTuple) =>
                    businessRegistrationService.updateRegisterBusiness(detailsTuple._2, overseasCompany, isGroup = false, isNonUKClientRegisteredByAgent = addClient, service, isBusinessDetailsEditable = true).map { response =>
                      redirectUrl match {
                        case Some(serviceUrl) => Redirect(serviceUrl.url)
                        case _ => Redirect(controllers.routes.ReviewDetailsController.businessDetails(service))
                      }
                    }
                  case _ =>
                    Logger.warn(s"[UpdateOverseasCompanyRegController][update] - No registration details found to edit")
                    throw new RuntimeException(Messages("bc.agent-service.error.no-registration-details"))
                }
            }
          })
    }
  }

  private def getBackLink(service: String, redirectUrl: Option[ContinueUrl]) = {
    redirectUrl match {
      case Some(x) => Some(x.url)
      case None => Some(controllers.routes.ReviewDetailsController.businessDetails(service).url)
    }
  }
}
