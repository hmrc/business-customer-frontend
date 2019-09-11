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

package controllers

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthActions
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AgentRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class ReviewDetailsController @Inject()(val authConnector: AuthConnector,
                                        val backLinkCacheConnector: BackLinkCacheConnector,
                                        config: ApplicationConfig,
                                        val dataCacheConnector: DataCacheConnector,
                                        agentRegistrationService: AgentRegistrationService,
                                        mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with BackLinkController with AuthActions {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  
  val controllerId: String = "ReviewDetailsController"
  private val DuplicateUserError = "duplicate user error"
  private val WrongRoleUserError = "wrong role user error"

  def businessDetails(serviceName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(serviceName) { implicit authContext =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession flatMap {
        case Some(businessDetails) =>
          currentBackLink.map(backLink =>
            if (authContext.isAgent && businessDetails.isBusinessDetailsEditable) {
              Ok(views.html.review_details_non_uk_agent(serviceName, businessDetails, backLink))
            } else {
              Ok(views.html.review_details(serviceName, authContext.isAgent, businessDetails, backLink))
            }
          )
        case _ =>
          Logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for")
          throw new RuntimeException("We could not find your details. Check and try again.")
      }
    }
  }

  def continue(serviceName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(serviceName) { implicit authContext =>
      if (authContext.isAgent && agentRegistrationService.isAgentEnrolmentAllowed(serviceName)) {
        agentRegistrationService.enrolAgent(serviceName).flatMap { response =>
          response.status match {
            case CREATED =>
              redirectToExernal(
                appConfig.agentConfirmationPath(serviceName),
                Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url)
              )
            case BAD_REQUEST | CONFLICT =>
              val (header, title, lede) = formatErrorMessage(DuplicateUserError, serviceName)
              Logger.warn(s"[ReviewDetailsController][continue] - agency has already enrolled in EMAC")
              Future.successful(Ok(views.html.global_error(header, title, lede, serviceName)))
            case FORBIDDEN =>
              val (header, title, lede) = formatErrorMessage(WrongRoleUserError, serviceName)
              Logger.warn(s"[ReviewDetailsController][continue] - wrong role for agent enrolling in EMAC")
              Future.successful(Ok(views.html.global_error(header, title, lede, serviceName)))
            case _ =>
              Logger.warn(s"[ReviewDetailsController][continue] - allocation failed")
              throw new RuntimeException("We could not find your details. Check and try again.")
          }
        }
      } else {
        val url: String = appConfig.conf.getConfString(s"microservice.services.${serviceName.toLowerCase}.serviceRedirectUrl", {
          Logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $serviceName")
          throw new RuntimeException(Messages("bc.business-review.error.no-service", serviceName, serviceName.toLowerCase))
        })
        redirectToExernal(url, Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
      }
    }
  }

  private def formatErrorMessage(str: String, serviceName: String): (String, String, String) =
    str match {
      case DuplicateUserError =>
        ("bc.business-registration-error.duplicate.identifier.header",
          "bc.business-registration-error.duplicate.identifier.title",
          "bc.business-registration-error.duplicate.identifier.message")
      case WrongRoleUserError =>
        ("bc.business-registration-error.wrong.role.header",
          "bc.business-registration-error.wrong.role.title",
          "bc.business-registration-error.wrong.role.message")
    }
}
