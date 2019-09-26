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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{BackLinkController, BaseController}
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessCustomerContext, BusinessRegistration, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import utils.BCUtils
import utils.BusinessCustomerConstants.BusinessRegDetailsId

import scala.concurrent.Future

object BusinessRegController extends BusinessRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
  override val controllerId: String = "BusinessRegController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait BusinessRegController extends BackLinkController {

  def businessRegistrationCache: BusinessRegCacheConnector

  def register(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    for {
      backLink <- currentBackLink
      businessRegistration <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
    } yield {
      businessRegistration match {
        case Some(businessReg) =>
          Ok(views.html.nonUkReg.business_registration(businessRegistrationForm.fill(businessReg), service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
        case None =>
          Ok(views.html.nonUkReg.business_registration(businessRegistrationForm, service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
      }
    }
  }

  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, bcContext.user.isAgent).fold(
      formWithErrors => {
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.business_registration(formWithErrors, service, displayDetails(businessType, service), backLink, bcContext.user.isAgent))
        )
      },
      registrationData => {
        businessRegistrationCache.cacheDetails[BusinessRegistration](BusinessRegDetailsId,registrationData).flatMap {
          registrationSuccessResponse =>
            RedirectWithBackLink(
              OverseasCompanyRegController.controllerId,
              controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, false),
              Some(controllers.nonUKReg.routes.BusinessRegController.register(service, businessType).url)
            )
        }
      }
    )
  }

  private def displayDetails(businessType: String, service: String)(implicit bcContext: BusinessCustomerContext) = {
    if (bcContext.user.isAgent) {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.agent.non-uk.header"),
        Messages("bc.business-registration.text.agent", service),
        None,
        BCUtils.getIsoCodeTupleList)
    } else {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.non-uk.header"),
        Messages("bc.business-registration.text.client", service),
        Some(Messages("bc.business-registration.lede.text")),
        BCUtils.getIsoCodeTupleList)
    }
  }
}
