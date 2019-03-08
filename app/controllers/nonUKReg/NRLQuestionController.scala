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

package controllers.nonUKReg

import config.FrontendAuthConnector
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.{BackLinkController, BaseController}
import forms.BusinessRegistrationForms._
import models.NRLQuestion
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import utils.BusinessCustomerConstants.NrlFormId

object NRLQuestionController extends NRLQuestionController {
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "NRLQuestionController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  override val businessRegistrationCache = BusinessRegCacheConnector
}

trait NRLQuestionController extends BackLinkController {
  def businessRegistrationCache: BusinessRegCacheConnector

  def view(service: String) = AuthAction(service).async { implicit bcContext =>
    if (bcContext.user.isAgent)
      ForwardBackLinkToNextPage(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"))
    else
      for{
        backLink <- currentBackLink
        savedNRL <- businessRegistrationCache.fetchAndGetCachedDetails[NRLQuestion](NrlFormId)
      }yield
        Ok(views.html.nonUkReg.nrl_question(nrlQuestionForm.fill(savedNRL.getOrElse(NRLQuestion())), service, backLink))

  }


  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    nrlQuestionForm.bindFromRequest.fold(
      formWithErrors =>
        currentBackLink.map(backLink => BadRequest(views.html.nonUkReg.nrl_question(formWithErrors, service, backLink))),
      formData => {
        businessRegistrationCache.cacheDetails[NRLQuestion](NrlFormId, formData)
        val paysSa = formData.paysSA.getOrElse(false)
        if (paysSa)
          RedirectWithBackLink(PaySAQuestionController.controllerId,
            controllers.nonUKReg.routes.PaySAQuestionController.view(service),
            Some(controllers.nonUKReg.routes.NRLQuestionController.view(service).url)
          )
        else
          RedirectWithBackLink(BusinessRegController.controllerId,
            controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"),
            Some(controllers.nonUKReg.routes.NRLQuestionController.view(service).url)
          )

      }
    )
  }

}
