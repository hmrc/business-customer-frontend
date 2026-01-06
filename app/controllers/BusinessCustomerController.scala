/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.AuthActions

import javax.inject.Inject
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

class BusinessCustomerController @Inject() (val authConnector: AuthConnector,
                                            config: ApplicationConfig,
                                            dataCacheService: DataCacheService,
                                            mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with AuthActions
    with I18nSupport
    with Logging {

  implicit val appConfig: ApplicationConfig       = config
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def clearCache(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      dataCacheService.clearCache
        .map { _ =>
          logger.info("session has been cleared")
          Ok
        }
        .recover { case t: Throwable =>
          logger.error(s"session has not been cleared for $service. Status: 500, Error: ${t.getMessage}")
          InternalServerError
        }
    }
  }

  def getReviewDetails(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      dataCacheService.fetchAndGetBusinessDetailsForSession.map {
        case Some(businessDetails) =>
          Ok(Json.toJson(businessDetails))
        case _ =>
          logger.warn(s"could not retrieve business details for $service")
          NotFound
      }
    }
  }

}
