/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.BackLinkController
import controllers.auth.AuthActions
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import javax.inject.Inject
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails, StandardAuthRetrievals}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.BusinessRegDetailsId

import scala.concurrent.ExecutionContext

class BusinessRegController @Inject()(val authConnector: AuthConnector,
                                      val backLinkCacheConnector: BackLinkCacheConnector,
                                      config: ApplicationConfig,
                                      template: views.html.nonUkReg.business_registration,
                                      businessRegistrationCache: BusinessRegCacheConnector,
                                      overseasCompanyRegController: OverseasCompanyRegController,
                                      mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with AuthActions with BackLinkController {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "BusinessRegController"


  def register(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      for {
        backLink <- currentBackLink
        businessRegistration <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
      } yield {
        businessRegistration match {
          case Some(businessReg) =>
            Ok(template(
              businessRegistrationForm.fill(businessReg),
              service,
              displayDetails(businessType, service),
              backLink,
              authContext.isAgent
            ))
          case None =>
            Ok(template(
              businessRegistrationForm, service, displayDetails(businessType, service),
              backLink, authContext.isAgent
            ))
        }
      }
    }
  }

  def send(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, authContext.isAgent, appConfig).fold(
        formWithErrors => {
          currentBackLink.map(backLink =>
            BadRequest(template(formWithErrors, service,
              displayDetails(businessType, service), backLink, authContext.isAgent))
          )
        },
        registrationData => {
          businessRegistrationCache.cacheDetails[BusinessRegistration](BusinessRegDetailsId, registrationData).flatMap { _ =>
              redirectWithBackLink(
                overseasCompanyRegController.controllerId,
                controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient = false),
                Some(controllers.nonUKReg.routes.BusinessRegController.register(service, businessType).url)
              )
          }
        }
      )
    }
  }

  private def displayDetails(businessType: String, service: String)(implicit authContext: StandardAuthRetrievals) = {
    if (authContext.isAgent) {
      BusinessRegistrationDisplayDetails(
        businessType,
        "bc.business-registration.agent.non-uk.header",
        "bc.business-registration.text.agent",
        None,
        appConfig.getIsoCodeTupleList
      )
    } else {
      BusinessRegistrationDisplayDetails(
        businessType,
        "bc.business-registration.user.non-uk.header",
        "bc.business-registration.text.client",
        Some("bc.business-registration.lede.text"),
        appConfig.getIsoCodeTupleList
      )
    }
  }
}
