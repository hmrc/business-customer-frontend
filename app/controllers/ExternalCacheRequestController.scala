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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataCacheService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ExternalCacheRequestController @Inject() (config: ApplicationConfig,
                                                mcc: MessagesControllerComponents,
                                                val authConnector: AuthConnector,
                                                val dataCacheService: DataCacheService)
    extends FrontendController(mcc)
    with AuthActions {

  implicit val appConfig: ApplicationConfig       = config
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def fetchCachedBusinessReviewDetails(service: String): Action[AnyContent] = {

    Action.async { implicit request =>
      implicit val transformedHeaderCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      authorisedFor(service) { implicit authContext =>
        dataCacheService.fetchAndGetBusinessDetailsForSession(transformedHeaderCarrier, executionContext).map {
          case Some(reviewDetails) => Ok(Json.toJson(reviewDetails))
          case _ =>
            logger.warn(s"could not retrieve business details for external calling $service")
            NotFound
        }
      }(hc = transformedHeaderCarrier, req = request, ec = executionContext)
    }
  }

}
