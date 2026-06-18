/*
 * Copyright 2026 HM Revenue & Customs
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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.auth.AuthActions
import controllers.nonUKReg.{BusinessRegController, NRLQuestionController}
import forms.BusinessVerificationForms.limitedCompanyUtrForm
import forms.{LimitedCompanyMatch, LimitedCompanyUtr}
import models.{Organisation, ReviewDetails, StandardAuthRetrievals}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.{CacheRegistrationDetails, CorporateBody}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CorporationTaxUtrController @Inject()(val config: ApplicationConfig,
                                               val authConnector: AuthConnector,
                                               template: views.html.corporation_tax_utr,
                                               businessRegCacheConnector: BusinessRegCacheConnector,
                                               val backLinkCacheConnector: BackLinkCacheConnector,
                                               val businessMatchingService: BusinessMatchingService,
                                               val businessRegUKController: BusinessRegUKController,
                                               val businessRegController: BusinessRegController,
                                               val nrlQuestionController: NRLQuestionController,
                                               val reviewDetailsController: ReviewDetailsController,
                                               val homeController: HomeController,
                                               val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with AuthActions with BackLinkController with I18nSupport {

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "BusinessVerificationController"
  val services: Seq[String] = Seq("awrs", "ated")

  def onPageLoad(service: String, businessType: String, companyName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url) // todo: lan change to new standalone company name page url
      processLimitedCompanyForm(businessType, service, companyName: String, backLink, authContext)
      }
    }

  private def processLimitedCompanyForm(businessType: String, service: String, companyName: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                       (implicit req: Request[AnyContent]): Future[Result] = {
    if (services.exists(_.equalsIgnoreCase(service))) {
      businessRegCacheConnector.fetchAndGetCachedDetails[LimitedCompanyMatch](s"$CacheRegistrationDetails${service}_$businessType")
        .map {
          case Some(cachedData) => Ok(template(limitedCompanyUtrForm.fill(LimitedCompanyUtr(cotaxUTR = cachedData.cotaxUTR)), authContext.isAgent, service, companyName, backLink = backLink))
          case None => Ok(template(limitedCompanyUtrForm, authContext.isAgent, service, companyName, backLink = backLink))
        }
    } else Future.successful(Ok(template(limitedCompanyUtrForm, authContext.isAgent, service, companyName, backLink = backLink)))
  }

  def submit(service: String, companyName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
        ltdFormHandling(limitedCompanyUtrForm, "LTD", service, companyName, backLink)
    }
  }

  private def ltdFormHandling(limitedCompanyUtrForm: Form[LimitedCompanyUtr],
                              businessType: String,
                              service: String,
                              companyName: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedCompanyUtrForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, authContext.isAgent, service, companyName, backLink))),
      limitedCompanyUtrFormData => {
        val organisation = Organisation(companyName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = limitedCompanyUtrFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails${service}_$businessType", limitedCompanyUtrFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
              )
            case None =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
          }
        }
      }
    )
  }

}
