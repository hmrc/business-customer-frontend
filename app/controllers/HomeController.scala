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
import connectors.BackLinkCacheConnector
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import services.BusinessMatchingService

import scala.concurrent.Future

object HomeController extends HomeController {
  val businessMatchService: BusinessMatchingService = BusinessMatchingService
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "HomeController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait HomeController extends BackLinkController {

  def businessMatchService: BusinessMatchingService

  def homePage(service: String, backLinkUrl: Option[String]) = AuthAction(service).async { implicit bcContext =>

    businessMatchService.matchBusinessWithUTR(isAnAgent = bcContext.user.isAgent, service) match {
      case Some(futureJsValue) =>
        futureJsValue flatMap {
          jsValue => jsValue.validate[ReviewDetails] match {
            case success: JsSuccess[ReviewDetails] => RedirectWithBackLink(ReviewDetailsController.controllerId, controllers.routes.ReviewDetailsController.businessDetails(service), backLinkUrl)
            case failure: JsError => RedirectWithBackLink(BusinessVerificationController.controllerId, controllers.routes.BusinessVerificationController.businessVerification(service), backLinkUrl)
          }
        }
      case None => RedirectWithBackLink(BusinessVerificationController.controllerId, controllers.routes.BusinessVerificationController.businessVerification(service), backLinkUrl)
    }
  }

}
