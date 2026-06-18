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
import forms.BusinessVerificationForms.limitedPartnershipUtrForm
import forms.{LimitedPartnershipMatch, LimitedPartnershipUtr}
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
class PartnershipTaxUtrController @Inject()(val config: ApplicationConfig,
                                            val authConnector: AuthConnector,
                                            template: views.html.partnership_tax_utr,
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
  val controllerId: String = "PartnershipTaxUtrController"
  val services: Seq[String] = Seq("awrs", "ated")

  def onPageLoad(service: String, businessType: String, partnershipName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url) // todo: lan change to new standalone partnership name page url
      processLimitedpartnershipForm(businessType, service, partnershipName: String, backLink, authContext)
      }
    }

  private def processLimitedpartnershipForm(businessType: String, service: String, partnershipName: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                       (implicit req: Request[AnyContent]): Future[Result] = {
    if (services.exists(_.equalsIgnoreCase(service))) {
      businessRegCacheConnector.fetchAndGetCachedDetails[LimitedPartnershipMatch](s"$CacheRegistrationDetails${service}_$businessType")
        .map {
          case Some(cachedData) => Ok(template(limitedPartnershipUtrForm.fill(LimitedPartnershipUtr(psaUTR = cachedData.psaUTR)), authContext.isAgent, service, businessType, partnershipName, backLink = backLink))
          case None => Ok(template(limitedPartnershipUtrForm, authContext.isAgent, service, businessType, partnershipName, backLink = backLink))
        }
    } else Future.successful(Ok(template(limitedPartnershipUtrForm, authContext.isAgent, service, businessType, partnershipName, backLink = backLink)))
  }

  def submit(service: String, businessType: String, partnershipName: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      lpFormHandling(limitedPartnershipUtrForm, businessType, service, partnershipName, backLink)
    }
  }

  private def lpFormHandling(limitedPartnershipUtrForm: Form[LimitedPartnershipUtr],
                              businessType: String,
                              service: String,
                              partnershipName: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedPartnershipUtrForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(template(formWithErrors, authContext.isAgent, service, businessType, partnershipName, backLink))),
      limitedPartnershipUtrFormData => {
        val organisation = Organisation(partnershipName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = limitedPartnershipUtrFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails${service}_$businessType", limitedPartnershipUtrFormData)
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
