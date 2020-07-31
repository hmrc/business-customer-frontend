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

package controllers

import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.auth.AuthActions
import javax.inject.Inject
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class BusinessCustomerController @Inject()(val authConnector: AuthConnector,
                                           config: ApplicationConfig,
                                           dataCacheConnector: DataCacheConnector,
                                           mcc: MessagesControllerComponents) extends FrontendController(mcc) with AuthActions with I18nSupport {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def clearCache(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      dataCacheConnector.clearCache.map { x =>
        x.status match {
          case OK | NO_CONTENT =>
            Ok
          case errorStatus =>
            Logger.error(s"session has not been cleared for $service. Status: $errorStatus, Error: ${x.body}")
            InternalServerError
        }
      }
    }
  }

  def getReviewDetails(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      dataCacheConnector.fetchAndGetBusinessDetailsForSession.map {
        case Some(businessDetails) =>
          Ok(Json.toJson(businessDetails))
        case _ =>
          Logger.warn(s"could not retrieve business details for $service")
          NotFound
      }
    }
  }

}
