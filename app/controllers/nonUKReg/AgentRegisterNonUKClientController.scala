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

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.BackLinkController
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails}
import play.api.Mode.Mode
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.{Configuration, Logger, Play}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BCUtils
import utils.BusinessCustomerConstants.BusinessRegDetailsId

object AgentRegisterNonUKClientController extends AgentRegisterNonUKClientController {
  // $COVERAGE-OFF$
  override val authConnector: AuthConnector = FrontendAuthConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
  override val controllerId: String = "AgentRegisterNonUKClientController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  // $COVERAGE-ON$
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait AgentRegisterNonUKClientController extends BackLinkController with RunMode {

  import play.api.Play.current

  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String, backLinkUrl: Option[String]) = AuthAction(service).async { implicit bcContext =>

    for {
      backLink <- currentBackLink
      businessRegistration <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
    } yield {
      val backlinkOption = if (backLinkUrl.isDefined) backLinkUrl else backLink
      businessRegistration match {
        case Some(busninessReg) =>
            Ok(views.html.nonUkReg.nonuk_business_registration(businessRegistrationForm.fill(busninessReg), service, displayDetails, backlinkOption))
        case None =>
            Ok(views.html.nonUkReg.nonuk_business_registration(businessRegistrationForm, service, displayDetails, backlinkOption))

        }
      }
    }


  def submit(service: String) = AuthAction(service).async { implicit bcContext =>
    BusinessRegistrationForms.validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest, service, true).fold(
      formWithErrors => {
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.nonuk_business_registration(formWithErrors, service, displayDetails, backLink))
        )
      },
      registerData => {
        businessRegistrationCache.cacheDetails[BusinessRegistration](BusinessRegDetailsId,registerData).flatMap {
          registrationSuccessResponse =>
            val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"microservice.services.${service.toLowerCase}.serviceRedirectUrl")
            serviceRedirectUrl match {
              case Some(redirectUrl) =>
                RedirectWithBackLink(OverseasCompanyRegController.controllerId,
                  controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, true, Some(ContinueUrl(redirectUrl))),
                  Some(controllers.nonUKReg.routes.AgentRegisterNonUKClientController.view(service).url)
                )
              case _ =>
                // $COVERAGE-OFF$
                Logger.warn(s"[ReviewDetailsController][submit] - No Service config found for = $service")
                throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
                // $COVERAGE-ON$
            }
        }
      }
    )
  }

  private def displayDetails = {
    BusinessRegistrationDisplayDetails("NUK",
      Messages("bc.non-uk-reg.header"),
      Messages("bc.non-uk-reg.sub-header"),
      Some(Messages("bc.non-uk-reg.lede.text")),
      BCUtils.getIsoCodeTupleList)
  }

}
