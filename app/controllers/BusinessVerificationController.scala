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
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import controllers.auth.AuthActions
import controllers.nonUKReg.{BusinessRegController, NRLQuestionController}
import forms.BusinessVerificationForms._
import forms._

import javax.inject.{Inject, Singleton}
import models._
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessVerificationController @Inject()(val config: ApplicationConfig,
                                               val authConnector: AuthConnector,
                                               template: views.html.business_verification,
                                               templateSOP: views.html.business_lookup_SOP,
                                               templateLTD: views.html.business_lookup_LTD,
                                               templateUIB: views.html.business_lookup_UIB,
                                               templateOBP: views.html.business_lookup_OBP,
                                               templateLLP: views.html.business_lookup_LLP,
                                               templateLP: views.html.business_lookup_LP,
                                               templateNRL: views.html.business_lookup_NRL,
                                               templateDetailsNotFound: views.html.details_not_found,
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
  private val businessTypeCacheKey: String = s"${AWRSRegistrationDetails}businessType"

  def businessVerification(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      for {
        backLink <- currentBackLink
        businessTypeDetails <- businessRegCacheConnector.fetchAndGetCachedDetails[BusinessType](businessTypeCacheKey)
      } yield {
        businessTypeDetails match {
          case Some(businessTypeData) =>
            Ok(template(businessTypeForm.fill(businessTypeData), authContext.isAgent, service, authContext.isSa, authContext.isOrg, backLink))
          case None =>
            Ok(template(businessTypeForm, authContext.isAgent, service, authContext.isSa, authContext.isOrg, backLink))
        }
      }
    }
  }

  def continue(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest(), service).fold(
        formWithErrors =>
          currentBackLink map ( backLink =>
            BadRequest(template(formWithErrors, authContext.isAgent, service, authContext.isSa, authContext.isOrg, backLink)
            )
          ),
        value => {
          if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(businessTypeCacheKey, value)
          val returnCall = Some(routes.BusinessVerificationController.businessVerification(service).url)
          value.businessType match {
            case Some("NUK") if service.equals("capital-gains-tax") =>
              redirectWithBackLink(businessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"), returnCall)
            case Some("NUK") if service.equals("ATED") && !authContext.isAgent =>
              redirectToExternal(appConfig.conf.getConfString(s"ated.overseasSameAccountUrl", throw new Exception("")),
                Some(controllers.routes.BusinessVerificationController.businessVerification(service).url))
            case Some("NUK") =>
              redirectWithBackLink(nrlQuestionController.controllerId, controllers.nonUKReg.routes.NRLQuestionController.view(service), returnCall)
            case Some("NEW") =>
              redirectWithBackLink(businessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "NEW"), returnCall)
            case Some("GROUP") =>
              redirectWithBackLink(businessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "GROUP"), returnCall)
            case Some(busType @ ("SOP" | "UIB" | "LTD" | "OBP" | "LLP" | "LP" | "UT" | "ULTD" | "NRL")) =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, busType)))
            case _ =>
              redirectWithBackLink(homeController.controllerId, controllers.routes.HomeController.homePage(service), returnCall)
          }
        }
      )
    }
  }

  def businessForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
        businessType match {
          case "SOP" => processSoleTraderForm(businessType, service, backLink, authContext)
          case "LTD" => processLimitedCompanyForm(businessType, service, backLink, authContext)
          case "UIB" => processUnincorporatedForm(businessType, service, backLink, authContext)
          case "OBP" => processOrdinaryBusinessPartnershipForm(businessType, service, backLink, authContext)
          case "LLP" => processLimitedLiabilityPartnershipForm(businessType, service, backLink, authContext)
          case "LP" => processLimitedPartnershipForm(businessType, service, backLink, authContext)
          case "UT" => Future.successful(Ok(templateLTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink)))
          case "ULTD" => Future.successful(Ok(templateLTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink)))
          case "NRL" => Future.successful(Ok(templateNRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service))))
        }
    }
  }

  private def processSoleTraderForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                   (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[SoleTraderMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateSOP(soleTraderForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateSOP(soleTraderForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateSOP(soleTraderForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processLimitedCompanyForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                       (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[LimitedCompanyMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateLTD(limitedCompanyForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateLTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateLTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processUnincorporatedForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                       (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[UnincorporatedMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateUIB(unincorporatedBodyForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateUIB(unincorporatedBodyForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateUIB(unincorporatedBodyForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processOrdinaryBusinessPartnershipForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                                    (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[OrdinaryBusinessPartnershipMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateOBP(ordinaryBusinessPartnershipForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateOBP(ordinaryBusinessPartnershipForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateOBP(ordinaryBusinessPartnershipForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processLimitedLiabilityPartnershipForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                                    (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[LimitedLiabilityPartnershipMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateLLP(limitedLiabilityPartnershipForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateLLP(limitedLiabilityPartnershipForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateLLP(limitedLiabilityPartnershipForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processLimitedPartnershipForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                           (implicit req: Request[AnyContent]): Future[Result] = {
    if (service.equalsIgnoreCase("awrs")) {
      businessRegCacheConnector.fetchAndGetCachedDetails[LimitedPartnershipMatch](s"$AWRSRegistrationDetails$businessType")
        .map {
          case Some(cachedData) => Ok(templateLP(limitedPartnershipForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateLP(limitedPartnershipForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateLP(limitedPartnershipForm, authContext.isAgent, service, businessType, backLink)))
  }

  def submit(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      businessType match {
        case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service, backLink)
        case "SOP" => sopFormHandling(soleTraderForm, businessType, service, backLink)
        case "LLP" => llpFormHandling(limitedLiabilityPartnershipForm, businessType, service, backLink)
        case "LP" => lpFormHandling(limitedPartnershipForm, businessType, service, backLink)
        case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service, backLink)
        case "LTD" => ltdFormHandling(limitedCompanyForm, businessType, service, backLink)
        case "UT" => ltdFormHandling(limitedCompanyForm, businessType, service, backLink)
        case "ULTD" => ltdFormHandling(limitedCompanyForm, businessType, service, backLink)
        case "NRL" => nrlFormHandling(nonResidentLandlordForm, businessType, service, getNrlBackLink(service))
      }
    }
  }

  def detailsNotFound(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessForm(service, businessType).url)
      Future.successful(Ok(
        templateDetailsNotFound(authContext.isAgent, service, businessType,
          backLink)
      ))
    }
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String, service: String, backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateUIB(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UnincorporatedBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", unincorporatedFormData)
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

  private def sopFormHandling(soleTraderForm: Form[SoleTraderMatch], businessType: String, service: String, backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    soleTraderForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateSOP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = authContext.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", soleTraderFormData)
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

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch],
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateLLP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", llpFormData)
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

  private def lpFormHandling(limitedPartnershipForm: Form[LimitedPartnershipMatch], businessType: String, service: String, backLink: Option[String])
                            (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedPartnershipForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateLP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", lpFormData)
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

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch],
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateOBP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", obpFormData)
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

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch],
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedCompanyForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateLTD(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (service.equalsIgnoreCase("awrs")) businessRegCacheConnector.cacheDetails(s"$AWRSRegistrationDetails$businessType", limitedCompanyFormData)
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

  private def nrlFormHandling(nrlForm: Form[NonResidentLandlordMatch],
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    nrlForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(templateNRL(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      nrlFormData => {
        val organisation = Organisation(nrlFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = nrlFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(details) =>
              val countryCode = details.businessAddress.country
              if(config.getSelectedCountry(countryCode) == countryCode && service == "ATED") {
                businessRegCacheConnector.cacheDetails(UpdateNotRegisterId, true)
                redirectWithBackLink(reviewDetailsController.controllerId,
                  controllers.nonUKReg.routes.BusinessRegController.register(service, businessType),
                  Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
                )
              } else {
                redirectWithBackLink(reviewDetailsController.controllerId,
                  controllers.routes.ReviewDetailsController.businessDetails(service),
                  Some(controllers.routes.BusinessVerificationController.businessForm(service, businessType).url)
                )
              }
            case None =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
          }
        }
      }
    )
  }

  private def getNrlBackLink(service: String) = Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)

}
