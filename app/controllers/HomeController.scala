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
import models.ReviewDetails
import play.api.libs.json.{JsError, JsSuccess}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BusinessMatchingService, BusinessRegCacheService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.UpdateNotRegisterId
import utils.RedirectUtils.redirectUrlGetRelativeOrDev

import javax.inject.{Inject, Provider}
import scala.concurrent.ExecutionContext

class HomeController @Inject() (val authConnector: AuthConnector,
                                val backLinkCacheService: BackLinkCacheService,
                                config: ApplicationConfig,
                                businessMatchService: BusinessMatchingService,
                                businessVerificationController: Provider[BusinessVerificationController],
                                reviewDetailsController: ReviewDetailsController,
                                businessRegCacheService: BusinessRegCacheService,
                                mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with BackLinkController
    with AuthActions {

  implicit val appConfig: ApplicationConfig       = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String                        = "HomeController"

  def homePage(service: String, backLinkUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      businessMatchService.matchBusinessWithUTR(isAnAgent = authContext.isAgent, service) match {
        case Some(futureJsValue) =>
          futureJsValue flatMap { jsValue =>
            jsValue.validate[ReviewDetails] match {
              case details: JsSuccess[ReviewDetails] =>
                val countryCode = details.get.businessAddress.country
                if (config.getSelectedCountry(countryCode) == countryCode && service == "ATED") {
                  businessRegCacheService.cacheDetails(UpdateNotRegisterId, true)
                  redirectWithBackLink(
                    businessVerificationController.get.controllerId,
                    controllers.routes.BusinessVerificationController.businessVerification(service),
                    backLinkUrl.map(redirectUrlGetRelativeOrDev(_).url)
                  )
                } else {
                  redirectWithBackLink(
                    reviewDetailsController.controllerId,
                    controllers.routes.ReviewDetailsController.businessDetails(service),
                    backLinkUrl.map(redirectUrlGetRelativeOrDev(_).url)
                  )
                }
              case _: JsError =>
                redirectWithBackLink(
                  businessVerificationController.get.controllerId,
                  controllers.routes.BusinessVerificationController.businessVerification(service),
                  backLinkUrl.map(redirectUrlGetRelativeOrDev(_).url)
                )
            }
          }
        case None =>
          redirectWithBackLink(
            businessVerificationController.get.controllerId,
            controllers.routes.BusinessVerificationController.businessVerification(service),
            backLinkUrl.map(redirectUrlGetRelativeOrDev(_).url)
          )
      }
    }
  }

}
