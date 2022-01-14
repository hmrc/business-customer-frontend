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

package controllers

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, DataCacheConnector}
import controllers.auth.AuthActions
import javax.inject.Inject
import play.api.Logging
import play.api.i18n.{Messages, MessagesProvider}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AgentRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionUtils

import scala.concurrent.{ExecutionContext, Future}

class ReviewDetailsController @Inject()(val authConnector: AuthConnector,
                                        val backLinkCacheConnector: BackLinkCacheConnector,
                                        config: ApplicationConfig,
                                        templateNonUkAgent: views.html.review_details_non_uk_agent,
                                        templateReviewDetails: views.html.review_details,
                                        templateError: views.html.global_error,
                                        val dataCacheConnector: DataCacheConnector,
                                        agentRegistrationService: AgentRegistrationService,
                                        mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with BackLinkController with AuthActions with Logging {

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
              Ok(templateNonUkAgent(serviceName, businessDetails, backLink))
            } else {
              Ok(templateReviewDetails(serviceName, authContext.isAgent, businessDetails, backLink))
            }
          )
        case _ =>
          val service = SessionUtils.findServiceInRequest(request)

          logger.warn(s"[ReviewDetailsController][businessDetails] - No Service details found in DataCache for $serviceName")
          Future.successful(Ok(templateError(
            Messages("global.error.InternalServerError500.title"),
            Messages("global.error.InternalServerError500.heading"),
            Messages("global.error.InternalServerError500.message"),
            service
          )))
      }
    }
  }

  private def formatErrorMessage(str: String)(implicit messagesProvider: MessagesProvider): (String, String, String) = str match {
    case DuplicateUserError =>
      (Messages("bc.business-registration-error.duplicate.identifier.header"),
        Messages("bc.business-registration-error.duplicate.identifier.title"),
        Messages("bc.business-registration-error.duplicate.identifier.message"))
    case WrongRoleUserError =>
      (Messages("bc.business-registration-error.wrong.role.header"),
        Messages("bc.business-registration-error.wrong.role.title"),
        Messages("bc.business-registration-error.wrong.role.message"))
  }

  def continue(serviceName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(serviceName) { implicit authContext =>
      if (authContext.isAgent && agentRegistrationService.isAgentEnrolmentAllowed(serviceName)) {
        agentRegistrationService.enrolAgent(serviceName).flatMap { response =>
          response.status match {
            case CREATED =>
              redirectToExternal(
                appConfig.agentConfirmationPath(serviceName),
                Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url)
              )
            case BAD_REQUEST | CONFLICT =>
              val (header, title, lede) = formatErrorMessage(DuplicateUserError)
              logger.warn(s"[ReviewDetailsController][continue] - agency has already enrolled in EMAC")
              Future.successful(Ok(templateError(header, title, lede, serviceName)))
            case FORBIDDEN =>
              val (header, title, lede) = formatErrorMessage(WrongRoleUserError)
              logger.warn(s"[ReviewDetailsController][continue] - wrong role for agent enrolling in EMAC")
              Future.successful(Ok(templateError(header, title, lede, serviceName)))
            case _ =>
              logger.warn(s"[ReviewDetailsController][continue] - allocation failed")
              throw new RuntimeException("We could not find your details. Check and try again.")
          }
        }
      } else {
        val url: String = appConfig.conf.getConfString(s"${serviceName.toLowerCase}.serviceRedirectUrl", {
          logger.warn(s"[ReviewDetailsController][continue] - No Service config found for = $serviceName")
          throw new RuntimeException(Messages("bc.business-review.error.no-service", serviceName, serviceName.toLowerCase))
        })
        redirectToExternal(url, Some(controllers.routes.ReviewDetailsController.businessDetails(serviceName).url))
      }
    }
  }
}
