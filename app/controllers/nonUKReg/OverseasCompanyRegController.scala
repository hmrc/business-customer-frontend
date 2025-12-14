/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.{BackLinkController, ReviewDetailsController}
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, OverseasCompany}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BusinessRegCacheService, BusinessRegistrationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.{BusinessRegDetailsId, OverseasRegDetailsId, UpdateNotRegisterId}
import utils.RedirectUtils.redirectUrlGetRelativeOrDev
import utils.{OverseasCompanyUtils, RedirectUtils}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OverseasCompanyRegController @Inject() (val authConnector: AuthConnector,
                                              val backLinkCacheConnector: BackLinkCacheService,
                                              config: ApplicationConfig,
                                              template: views.html.nonUkReg.overseas_company_registration,
                                              businessRegistrationService: BusinessRegistrationService,
                                              businessRegistrationCache: BusinessRegCacheService,
                                              reviewDetailsController: ReviewDetailsController,
                                              mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with AuthActions
    with BackLinkController
    with OverseasCompanyUtils {

  implicit val appConfig: ApplicationConfig       = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String                        = "OverseasCompanyRegController"

  def view(service: String, addClient: Boolean, redirectUrl: Option[RedirectUrl] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      RedirectUtils.getRelativeOrBadRequestOpt(redirectUrl) { newUrl =>
        for {
          backLink       <- currentBackLink
          overseasNumber <- businessRegistrationCache.fetchAndGetCachedDetails[OverseasCompany](OverseasRegDetailsId)
        } yield {
          overseasNumber match {
            case Some(oversea) =>
              Ok(
                template(
                  overseasCompanyForm.fill(oversea),
                  service,
                  displayDetails(authContext.isAgent, addClient, service),
                  appConfig.getIsoCodeTupleList,
                  newUrl,
                  backLink))
            case None =>
              Ok(
                template(
                  overseasCompanyForm,
                  service,
                  displayDetails(authContext.isAgent, addClient, service),
                  appConfig.getIsoCodeTupleList,
                  newUrl,
                  backLink))
          }
        }
      }
    }
  }

  def register(service: String, addClient: Boolean, redirectUrl: Option[RedirectUrl] = None): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      BusinessRegistrationForms
        .validateNonUK(overseasCompanyForm.bindFromRequest())
        .fold(
          formWithErrors => {
            currentBackLink.map(backLink =>
              BadRequest(template(
                formWithErrors,
                service,
                displayDetails(authContext.isAgent, addClient, service),
                appConfig.getIsoCodeTupleList,
                redirectUrl.map(redirectUrlGetRelativeOrDev(_).url),
                backLink
              )))
          },
          overseasCompany =>
            for {
              cachedBusinessReg <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
              updateOrRegister  <- businessRegistrationCache.fetchAndGetCachedDetails[Boolean](UpdateNotRegisterId)
              _                 <- businessRegistrationCache.cacheDetails[OverseasCompany](OverseasRegDetailsId, overseasCompany)
              _ <- cachedBusinessReg match {
                case Some(businessReg) if updateOrRegister.getOrElse(false) =>
                  businessRegistrationService.updateRegisterBusiness(
                    businessReg,
                    overseasCompany,
                    isGroup = false,
                    isNonUKClientRegisteredByAgent = addClient,
                    service,
                    isBusinessDetailsEditable = true
                  )
                case Some(businessReg) =>
                  businessRegistrationService.registerBusiness(
                    businessReg,
                    overseasCompany,
                    isGroup = false,
                    isNonUKClientRegisteredByAgent = addClient,
                    service,
                    isBusinessDetailsEditable = true
                  )
                case None =>
                  throw new RuntimeException(s"[OverseasCompanyRegController][send] - service :$service. Error : No Cached BusinessRegistration")
              }
              redirectPage <- redirectUrl match {
                case Some(x) =>
                  RedirectUtils.getRelativeOrBadRequest(x) { newUrl =>
                    redirectToExternal(
                      newUrl,
                      Some(
                        controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient, Some(x)).url
                      ))
                  }
                case None =>
                  redirectWithBackLink(
                    reviewDetailsController.controllerId,
                    controllers.routes.ReviewDetailsController.businessDetails(service),
                    Some(
                      controllers.nonUKReg.routes.OverseasCompanyRegController
                        .view(
                          service,
                          addClient,
                          None
                        )
                        .url)
                  )
              }
            } yield redirectPage
        )
    }
  }

}
