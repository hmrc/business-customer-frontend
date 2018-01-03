/*
 * Copyright 2018 HM Revenue & Customs
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

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{OverseasCompany, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import services.BusinessRegistrationService
import utils.BCUtils

import scala.concurrent.Future

object BusinessRegUKController extends BusinessRegUKController {
  val authConnector = FrontendAuthConnector
  val businessRegistrationService = BusinessRegistrationService
  override val controllerId: String = "BusinessRegUKController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait BusinessRegUKController extends BackLinkController {

  def businessRegistrationService: BusinessRegistrationService

  def register(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    val newMapping = businessRegistrationForm.data + ("businessAddress.country" -> "GB")
    currentBackLink.map(backLink =>
      Ok(views.html.business_group_registration(businessRegistrationForm.copy(data = newMapping), service, displayDetails(businessType, service), backLink))
    )
  }

  def send(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateUK(businessRegistrationForm.bindFromRequest).fold(
      formWithErrors => {
        currentBackLink.map(backLink => BadRequest(views.html.business_group_registration(formWithErrors, service, displayDetails(businessType, service), backLink)))
      },
      registrationData => {
        businessRegistrationService.registerBusiness(registrationData,
          OverseasCompany(),
          isGroup(businessType),
          isNonUKClientRegisteredByAgent = false,
          service,
          isBusinessDetailsEditable = false).flatMap {
          registrationSuccessResponse =>
            RedirectWithBackLink(
              ReviewDetailsController.controllerId,
              controllers.routes.ReviewDetailsController.businessDetails(service),
              Some(controllers.routes.BusinessRegUKController.register(service, businessType).url))
        }
      }
    )
  }

  private def isGroup(businessType: String) = {
    businessType.equals("GROUP")
  }

  private def displayDetails(businessType: String, service: String) = {
    if (isGroup(businessType)) {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.group.header", service.toUpperCase),
        Messages("bc.business-registration.group.subheader"),
        None,
        BCUtils.getIsoCodeTupleList)
    }
    else {
      BusinessRegistrationDisplayDetails(businessType,
        Messages("bc.business-registration.user.new-business.header"),
        Messages("bc.business-registration.business.subheader"),
        None,
        BCUtils.getIsoCodeTupleList)
    }
  }
}
