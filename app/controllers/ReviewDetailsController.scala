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

package controllers

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.ExternalUrls
import models.{EnrolErrorResponse, EnrolResponse}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.{Logger, Play}
import services.{AgentRegistrationService, NewAgentRegistrationService}
import uk.gov.hmrc.play.config.RunMode
import utils.ErrorMessageUtils._
import utils.FeatureSwitch

import scala.concurrent.Future

object ReviewDetailsController extends ReviewDetailsController {
  val dataCacheConnector = DataCacheConnector
  val authConnector = FrontendAuthConnector
  val agentRegistrationService = AgentRegistrationService
  val newAgentRegistrationService = NewAgentRegistrationService
  override val controllerId: String = "ReviewDetailsController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait ReviewDetailsController extends BackLinkController with RunMode {

  import play.api.Play.current

  def dataCacheConnector: DataCacheConnector

  def agentRegistrationService: AgentRegistrationService

  def newAgentRegistrationService: NewAgentRegistrationService

  def businessDetails(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
      case Some(businessDetails) =>
        currentBackLink.map(backLink =>
          if (bcContext.user.isAgent && businessDetails.isBusinessDetailsEditable) {
            Ok(views.html.review_details_non_uk_agent(serviceName, businessDetails, backLink))
          } else {
            Ok(views.html.review_details(serviceName, bcContext.user.isAgent, businessDetails, backLink))
          }
        )
      case _ =>
        Logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for")
        throw new RuntimeException(Messages("bc.business-review.error.not-found"))
    }
  }

  def continue(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    if (bcContext.user.isAgent && agentRegistrationService.isAgentEnrolmentAllowed(serviceName)) {
      if(FeatureSwitch.isEnabled("registration.usingGG")) {
        agentRegistrationService.enrolAgent(serviceName).flatMap { response =>
          response.status match {
            case OK => RedirectToExernal(ExternalUrls.agentConfirmationPath(serviceName), Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
            case BAD_GATEWAY if matchErrorResponse(response) =>
              Logger.warn(s"[ReviewDetailsController][continue] - Already Registered Error")
              Future.successful(Ok(views.html.global_error(Messages("bc.business-registration-error.duplicate.identifier.header"),
                Messages("bc.business-registration-error.duplicate.identifier.title"),
                Messages("bc.business-registration-error.duplicate.identifier.message"), serviceName)))
            case _ =>
              Logger.warn(s"[ReviewDetailsController][continue] - Exception other than status - OK and BAD_GATEWAY")
              throw new RuntimeException(Messages("bc.business-review.error.not-found"))
          }
        }
      }
      else {
        newAgentRegistrationService.enrolAgent(serviceName).flatMap { response =>
          response.status match {
            case OK => RedirectToExernal(ExternalUrls.agentConfirmationPath(serviceName), Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
            case BAD_GATEWAY if matchErrorResponse(response) =>
              Logger.warn(s"[ReviewDetailsController][continue] - Already Registered Error")
              Future.successful(Ok(views.html.global_error(Messages("bc.business-registration-error.duplicate.identifier.header"),
                Messages("bc.business-registration-error.duplicate.identifier.title"),
                Messages("bc.business-registration-error.duplicate.identifier.message"), serviceName)))
            case _ =>
              Logger.warn(s"[ReviewDetailsController][continue] - Exception other than status - OK and BAD_GATEWAY")
              throw new RuntimeException(Messages("bc.business-review.error.not-found"))
          }
        }
      }
    } else {
      val serviceRedirectUrl: Option[String] = Play.configuration.getString(s"microservice.services.${serviceName.toLowerCase}.serviceRedirectUrl")
      serviceRedirectUrl match {
        case Some(serviceUrl) => RedirectToExernal(serviceUrl, Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
        case _ =>
          // $COVERAGE-OFF$
          Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $serviceName")
          throw new RuntimeException(Messages("bc.business-review.error.no-service", serviceName, serviceName.toLowerCase))
          // $COVERAGE-ON$
      }
    }
  }
}
