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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.BackLinkController
import controllers.auth.AuthActions
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import javax.inject.Inject
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.BusinessRegDetailsId

import scala.concurrent.ExecutionContext

class AgentRegisterNonUKClientController @Inject()(val authConnector: AuthConnector,
                                                   val backLinkCacheConnector: BackLinkCacheConnector,
                                                   config: ApplicationConfig,
                                                   template: views.html.nonUkReg.nonuk_business_registration,
                                                   businessRegistrationCache: BusinessRegCacheConnector,
                                                   overseasCompanyRegController: OverseasCompanyRegController,
                                                   mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with AuthActions with BackLinkController {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "AgentRegisterNonUKClientController"

  def view(service: String, backLinkUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      for {
        backLink <- currentBackLink
        businessRegistration <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
      } yield {
        val backLinkOption = if (backLinkUrl.isDefined) backLinkUrl else backLink
        businessRegistration match {
          case Some(businessReg) =>
            Ok(template(businessRegistrationForm.fill(businessReg), service, displayDetails, backLinkOption))
          case None =>
            Ok(template(businessRegistrationForm, service, displayDetails, backLinkOption))
        }
      }
    }
  }

  def submit(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit userContext =>
      BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, isAgent = true, appConfig).fold(
        formWithErrors => {
          currentBackLink.map(backLink =>
            BadRequest(template(formWithErrors, service, displayDetails, backLink))
          )
        },
        registerData => {
          businessRegistrationCache.cacheDetails[BusinessRegistration](BusinessRegDetailsId, registerData).flatMap { _ =>
            val redirectUrl: Option[String] = Some(appConfig.conf.getConfString(s"${service.toLowerCase}.serviceRedirectUrl", {
                logger.warn(s"[ReviewDetailsController][submit] - No Service config found for = $service")
                throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
            }))
            redirectWithBackLink(overseasCompanyRegController.controllerId,
              controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient = true, redirectUrl),
              Some(controllers.nonUKReg.routes.AgentRegisterNonUKClientController.view(service).url)
            )
          }
        }
      )
    }
  }

  private val displayDetails = BusinessRegistrationDisplayDetails(
      "NUK",
      "bc.non-uk-reg.header",
      "bc.non-uk-reg.sub-header",
      Some("bc.non-uk-reg.lede.text"),
      appConfig.getIsoCodeTupleList
    )

}
