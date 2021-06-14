/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.BackLinkCacheConnector
import controllers.auth.AuthActions
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import javax.inject.Inject
import models.{BusinessRegistrationDisplayDetails, OverseasCompany}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.BusinessRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext


class BusinessRegUKController @Inject()(val authConnector: AuthConnector,
                                        val backLinkCacheConnector: BackLinkCacheConnector,
                                        config: ApplicationConfig,
                                        template: views.html.business_group_registration,
                                        businessRegistrationService: BusinessRegistrationService,
                                        reviewDetailsController: ReviewDetailsController,
                                        mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with BackLinkController with AuthActions {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "BusinessRegUKController"

  def register(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val newMapping = businessRegistrationForm.data + ("businessAddress.country" -> "GB")
      currentBackLink map (backLink =>
        Ok(template(businessRegistrationForm.copy(data = newMapping),
          authContext.isAgent, service, displayDetails(businessType, service), backLink))
      )
    }
  }

  def send(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service){ implicit authContext =>
      BusinessRegistrationForms.validateUK(businessRegistrationForm.bindFromRequest).fold(
        formWithErrors => currentBackLink map (backLink =>
          BadRequest(template(formWithErrors, authContext.isAgent, service, displayDetails(businessType, service), backLink))
        ),
        registrationData => {
          businessRegistrationService.registerBusiness(
            registrationData,
            OverseasCompany(),
            isGroup(businessType),
            isNonUKClientRegisteredByAgent = false,
            service
          ) flatMap { _ =>
            redirectWithBackLink(
              reviewDetailsController.controllerId,
              controllers.routes.ReviewDetailsController.businessDetails(service),
              Some(controllers.routes.BusinessRegUKController.register(service, businessType).url)
            )
          }
        }
      )
    }
  }

  private def isGroup(businessType: String) = businessType equals "GROUP"

  private def displayDetails(businessType: String, service: String) = {
    if (isGroup(businessType)) {
      BusinessRegistrationDisplayDetails(
        businessType,
        "bc.business-registration.user.group.header",
        "bc.business-registration.group.subheader",
        None,
        appConfig.getIsoCodeTupleList)
    }
    else {
      BusinessRegistrationDisplayDetails(
        businessType,
        "bc.business-registration.user.new-business.header",
        "bc.business-registration.business.subheader",
        None,
        appConfig.getIsoCodeTupleList)
    }
  }
}
