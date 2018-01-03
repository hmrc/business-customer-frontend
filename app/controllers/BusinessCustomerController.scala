/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.DataCacheConnector
import play.api.Logger
import play.api.libs.json.Json


object BusinessCustomerController extends BusinessCustomerController {
  // $COVERAGE-OFF$
  override val authConnector = FrontendAuthConnector
  override val dataCacheConnector = DataCacheConnector
  // $COVERAGE-ON$
}

trait BusinessCustomerController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def clearCache(service: String) = AuthAction(service).async { implicit bcContext =>
    dataCacheConnector.clearCache.map { x =>
      x.status match {
        case OK | NO_CONTENT =>
          Ok
        case errorStatus => {
          Logger.error(s"session has not been cleared for $service. Status: $errorStatus, Error: ${x.body}")
          InternalServerError
        }
      }
    }
  }

  def getReviewDetails(service: String) = AuthAction(service).async { implicit bcContext =>
    dataCacheConnector.fetchAndGetBusinessDetailsForSession.map {
      case Some(businessDetails) =>
        Ok(Json.toJson(businessDetails))
      case _ =>
        Logger.warn(s"could not retrieve business details for $service")
        NotFound
    }
  }


}
