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

package controllers.nonUKReg

import config.ApplicationConfig
import controllers.BackLinkController
import controllers.auth.AuthActions
import forms.BusinessRegistrationForms
import forms.BusinessRegistrationForms._
import models.{BusinessRegistration, BusinessRegistrationDisplayDetails}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{BackLinkCacheService, BusinessRegCacheService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.BusinessRegDetailsId
import utils.RedirectUtils.redirectUrlGetRelativeOrDev

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class AgentRegisterNonUKClientController @Inject() (val authConnector: AuthConnector,
                                                    val backLinkCacheService: BackLinkCacheService,
                                                    config: ApplicationConfig,
                                                    template: views.html.nonUkReg.nonuk_business_registration,
                                                    businessRegistrationCache: BusinessRegCacheService,
                                                    overseasCompanyRegController: OverseasCompanyRegController,
                                                    mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with AuthActions
    with BackLinkController {

  implicit val appConfig: ApplicationConfig       = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String                        = "AgentRegisterNonUKClientController"

  def view(service: String, backLinkUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      for {
        fetchedBackLink      <- currentBackLink
        businessRegistration <- businessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](BusinessRegDetailsId)
      } yield {
        val backLinkOption: Option[String] =
          if (backLinkUrl.isDefined) {
            handleDefinedBacklink(backLinkUrl, fetchedBackLink)
          } else {
            fetchedBackLink.orElse(config.backToInformHMRCNrlUrl)
          }

        businessRegistration match {
          case Some(businessReg) =>
            Ok(template(businessRegistrationForm.fill(businessReg), service, displayDetails, backLinkOption))
          case None =>
            Ok(template(businessRegistrationForm, service, displayDetails, backLinkOption))
        }
      }
    }
  }

  private def handleDefinedBacklink(backLinkUrl: Option[RedirectUrl], fetchedBackLink: Option[String])(implicit hc: HeaderCarrier) = {
    val resolvedBacklink = Try(backLinkUrl.map(redirectUrlGetRelativeOrDev(_).url)) match {
      case Success(url) => url
      case Failure(_)   => fetchedBackLink
    }
    setBackLink(controllerId, resolvedBacklink)
    resolvedBacklink
  }

  def submit(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit userContext =>
      BusinessRegistrationForms
        .validateCountryNonUKAndPostcode(businessRegistrationForm.bindFromRequest(), service, isAgent = true, appConfig)
        .fold(
          formWithErrors => {
            currentBackLink.map(backLink => BadRequest(template(formWithErrors, service, displayDetails, backLink)))
          },
          registerData => {
            businessRegistrationCache.cacheDetails[BusinessRegistration](BusinessRegDetailsId, registerData).flatMap { _ =>
              val redirectUrl: Option[String] = Some(appConfig.conf.getConfString(
                s"${service.toLowerCase}.serviceRedirectUrl", {
                  logger.warn(s"[ReviewDetailsController][submit] - No Service config found for = $service")
                  throw new RuntimeException(Messages("bc.business-review.error.no-service", service, service.toLowerCase))
                }
              ))
              redirectWithBackLink(
                overseasCompanyRegController.controllerId,
                controllers.nonUKReg.routes.OverseasCompanyRegController.view(service, addClient = true, redirectUrl.map(RedirectUrl(_))),
                Some(controllers.nonUKReg.routes.AgentRegisterNonUKClientController.view(service).url)
              )
            }
          }
        )
    }
  }

  private val displayDetails = BusinessRegistrationDisplayDetails(
    "NUK",
    "bc.non-uk-reg.header",
    "bc.non-uk-reg.sub-header",
    Some("bc.non-uk-reg.lede.text"),
    appConfig.getIsoCodeTupleList
  )

}
