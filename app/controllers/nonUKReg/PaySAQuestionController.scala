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

package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import forms.BusinessRegistrationForms._
import controllers.{BackLinkController, BaseController, BusinessVerificationController}
import models.PaySAQuestion
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.BusinessCustomerConstants.PaySaDetailsId

object PaySAQuestionController extends PaySAQuestionController {
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "PaySAQuestionController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
}

trait PaySAQuestionController extends BackLinkController {

  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String) = AuthAction(service).async { implicit bcContext =>
    if (bcContext.user.isAgent)
      ForwardBackLinkToNextPage(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"))
    else
      for{
          backLink <- currentBackLink
          savedPaySa <- businessRegistrationCache.fetchAndGetCachedDetails[PaySAQuestion](PaySaDetailsId)
        }yield
        Ok(views.html.nonUkReg.paySAQuestion(paySAQuestionForm.fill(savedPaySa.getOrElse(PaySAQuestion())), service, backLink))

  }




  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    paySAQuestionForm.bindFromRequest.fold(
      formWithErrors =>
        currentBackLink.map(backLink =>
          BadRequest(views.html.nonUkReg.paySAQuestion(formWithErrors, service, backLink))
        ),
      formData => {
        businessRegistrationCache.cacheDetails[PaySAQuestion](PaySaDetailsId, formData)
          val paysSa = formData.paySA.getOrElse(false)
          if (paysSa)
            RedirectWithBackLink(BusinessVerificationController.controllerId,
              controllers.routes.BusinessVerificationController.businessForm(service, "NRL"),
              Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)
            )
          else
            RedirectWithBackLink(BusinessRegController.controllerId,
              controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"),
              Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)
            )
        }
    )
      }

  }
