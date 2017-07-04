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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{BackLinkController, BaseController, ReviewDetailsController}
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, OverseasCompany}
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.Messages
import services.BusinessRegistrationService
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.RunMode
import utils.{BCUtils, OverseasCompanyUtils}
import utils.BusinessCustomerConstants.{BusinessRegDetailsId, OverseasRegDetailsId}

import scala.concurrent.Future

object OverseasCompanyRegController extends OverseasCompanyRegController {
  override val authConnector = FrontendAuthConnector
  override val businessRegistrationService = BusinessRegistrationService
  override val businessRegistrationCache = BusinessRegCacheConnector
  override val controllerId: String = "OverseasCompanyRegController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait OverseasCompanyRegController extends BackLinkController with RunMode {

  def businessRegistrationService: BusinessRegistrationService
  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String, addClient: Boolean, redirectUrl: Option[ContinueUrl] = None) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        for {
          backLink <- currentBackLink
          overseasNumber <- businessRegistrationCache.fetchAndGetCachedDetails[OverseasCompany](OverseasRegDetailsId)
        } yield {
          overseasNumber match {
            case Some(oversea) =>
              Ok(views.html.nonUkReg.overseas_company_registration(overseasCompanyForm.fill(oversea), service,
                OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, backLink))
            case None => Ok(views.html.nonUkReg.overseas_company_registration(overseasCompanyForm, service,
              OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, backLink))
          }
        }
    }
  }


  def register(service: String, addClient: Boolean, redirectUrl: Option[ContinueUrl] = None) = AuthAction(service).async { implicit bcContext =>
    redirectUrl match {
      case Some(x) if !x.isRelativeOrDev(ApplicationConfig.env) => Future.successful(BadRequest("The redirect url is not correctly formatted"))
      case _ =>
        BusinessRegistrationForms.validateNonUK(overseasCompanyForm.bindFromRequest).fold(
          formWithErrors => {
            currentBackLink.map(backLink => BadRequest(views.html.nonUkReg.overseas_company_registration(formWithErrors, service,
              OverseasCompanyUtils.displayDetails(bcContext.user.isAgent, addClient, service), BCUtils.getIsoCodeTupleList, redirectUrl, backLink))
            )
          },
          overseasCompany => {
            for {
              cachedBusinessReg <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
              _ <- businessRegistrationCache.cacheDetails[OverseasCompany](OverseasRegDetailsId, overseasCompany)
              reviewDetails <-
              cachedBusinessReg match {
                case Some(businessReg) =>
                  businessRegistrationService.registerBusiness(businessReg, overseasCompany, isGroup = false, isNonUKClientRegisteredByAgent = addClient, service, isBusinessDetailsEditable = true)
                case None =>
                  throw new RuntimeException(s"[OverseasCompanyRegController][send] - service :$service. Error : No Cached BusinessRegistration")
              }
              redirectPage <- redirectUrl match {
                case Some(x) => RedirectToExernal(x.url, Some(controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient, Some(x)).url))
                case None => RedirectWithBackLink(
                  ReviewDetailsController.controllerId,
                  controllers.routes.ReviewDetailsController.businessDetails(service),
                  Some(controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient, redirectUrl).url)
                )
              }
            } yield {
              redirectPage
            }
          })
    }
  }

}
