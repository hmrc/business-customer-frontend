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

package controllers

import config.ApplicationConfig
import connectors.BackLinkCacheConnector
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
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.BusinessCustomerConstants._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessVerificationControllerImpl @Inject()(val config: ApplicationConfig,
                                                   val authConnector: AuthConnector,
                                                   val backLinkCacheConnector: BackLinkCacheConnector,
                                                   val businessMatchingService: BusinessMatchingService,
                                                   val businessRegUKController: BusinessRegUKController,
                                                   val businessRegController: BusinessRegController,
                                                   val nrlQuestionController: NRLQuestionController,
                                                   val reviewDetailsController: ReviewDetailsController,
                                                   val homeController: HomeController,
                                                   val mcc: MessagesControllerComponents) extends FrontendController(mcc) with BusinessVerificationController

trait BusinessVerificationController extends AuthActions with BackLinkController with I18nSupport {
  self: FrontendController =>

  val config: ApplicationConfig
  val mcc: MessagesControllerComponents

  val businessMatchingService: BusinessMatchingService
  val businessRegUKController: BusinessRegUKController
  val businessRegController: BusinessRegController
  val nrlQuestionController: NRLQuestionController
  val reviewDetailsController: ReviewDetailsController
  val homeController: HomeController

  implicit val appConfig: ApplicationConfig = config
  implicit val executionContext: ExecutionContext = mcc.executionContext
  val controllerId: String = "BusinessVerificationController"

  def businessVerification(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      currentBackLink map ( backLink =>
        Ok(views.html.business_verification(businessTypeForm, authContext.isAgent, service, authContext.isSa, authContext.isOrg, backLink))
      )
    }
  }

  def continue(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest, service).fold(
        formWithErrors =>
          currentBackLink map ( backLink =>
            BadRequest(views.html.business_verification(formWithErrors, authContext.isAgent, service, authContext.isSa, authContext.isOrg, backLink)
          )
        ),
        value => {
          val returnCall = Some(routes.BusinessVerificationController.businessVerification(service).url)
          value.businessType match {
            case Some("NUK") if service.equals("capital-gains-tax") =>
              redirectWithBackLink(businessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"), returnCall)
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
      Future.successful(
        businessType match {
          case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, authContext.isAgent, service, businessType, backLink))
          case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink))
          case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, authContext.isAgent, service, businessType, backLink))
          case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, authContext.isAgent, service, businessType, backLink))
          case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, authContext.isAgent, service, businessType, backLink))
          case "LP" => Ok(views.html.business_lookup_LP(limitedPartnershipForm, authContext.isAgent, service, businessType, backLink))
          case "UT" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink))
          case "ULTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, authContext.isAgent, service, businessType, backLink))
          case "NRL" => Ok(views.html.business_lookup_NRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service)))
        }
      )
    }
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
      Future.successful(Ok(
        views.html.details_not_found(authContext.isAgent, service, businessType,
          Some(routes.BusinessVerificationController.businessForm(service, businessType).url))
      ))
    }
  }

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String, service: String, backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UnincorporatedBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = authContext.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => currentBackLink.map(implicit backLink =>
        BadRequest(views.html.business_lookup_LLP(formWithErrors, authContext.isAgent, service, businessType, backLink))
      ),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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
    nrlForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_NRL(formWithErrors, authContext.isAgent, service, businessType, backLink))),
      nrlFormData => {
        val organisation = Organisation(nrlFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = nrlFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
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

  private def getNrlBackLink(service: String) = Some(controllers.nonUKReg.routes.PaySAQuestionController.view(service).url)

}
