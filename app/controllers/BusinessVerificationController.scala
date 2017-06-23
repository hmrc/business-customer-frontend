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
import controllers.nonUKReg.{BusinessRegController, NRLQuestionController}
import forms.BusinessVerificationForms._
import forms._
import models._
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.BusinessCustomerConstants._

import scala.concurrent.Future

object BusinessVerificationController extends BusinessVerificationController {
  val businessMatchingService: BusinessMatchingService = BusinessMatchingService
  val authConnector = FrontendAuthConnector
  override val controllerId: String = "BusinessVerificationController"
  override val backLinkCacheConnector = BackLinkCacheConnector
}

trait BusinessVerificationController extends BackLinkController {

  def businessMatchingService: BusinessMatchingService

  def businessVerification(service: String) = AuthAction(service).async {
    implicit bcContext =>
      currentBackLink.map(backLink => Ok(views.html.business_verification(businessTypeForm, bcContext.user.isAgent, service, bcContext.user.isSa, bcContext.user.isOrg, backLink)))
  }

  def continue(service: String) = AuthAction(service).async { implicit bcContext =>
    BusinessVerificationForms.validateBusinessType(businessTypeForm.bindFromRequest, service).fold(
      formWithErrors =>
        currentBackLink.map(backLink => BadRequest(views.html.business_verification(formWithErrors, bcContext.user.isAgent, service, bcContext.user.isSa, bcContext.user.isOrg, backLink))
        ),
      value => {
        val returnCall = Some(routes.BusinessVerificationController.businessVerification(service).url)
        value.businessType match {
          case Some("NUK") if service.equals("capital-gains-tax") =>
            RedirectWithBackLink(BusinessRegController.controllerId, controllers.nonUKReg.routes.BusinessRegController.register(service, "NUK"), returnCall)
          case Some("NUK") =>
            RedirectWithBackLink(NRLQuestionController.controllerId, controllers.nonUKReg.routes.NRLQuestionController.view(service), returnCall)
          case Some("NEW") =>
            RedirectWithBackLink(BusinessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "NEW"), returnCall)
          case Some("GROUP") =>
            RedirectWithBackLink(BusinessRegUKController.controllerId, controllers.routes.BusinessRegUKController.register(service, "GROUP"), returnCall)
          case Some("SOP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "SOP")))
          case Some("UIB") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UIB")))
          case Some("LTD") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LTD")))
          case Some("OBP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "OBP")))
          case Some("LLP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LLP")))
          case Some("LP") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "LP")))
          case Some("UT") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "UT")))
          case Some("ULTD") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "ULTD")))
          case Some("NRL") =>
            Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessForm(service, "NRL")))
          case _ =>
            RedirectWithBackLink(HomeController.controllerId, controllers.routes.HomeController.homePage(service), returnCall)
        }
      }
    )
  }

  def businessForm(service: String, businessType: String) = AuthAction(service) { implicit bcContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      businessType match {
        case "SOP" => Ok(views.html.business_lookup_SOP(soleTraderForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "UIB" => Ok(views.html.business_lookup_UIB(unincorporatedBodyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "OBP" => Ok(views.html.business_lookup_OBP(ordinaryBusinessPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LLP" => Ok(views.html.business_lookup_LLP(limitedLiabilityPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "LP" => Ok(views.html.business_lookup_LP(limitedPartnershipForm, bcContext.user.isAgent, service, businessType, backLink))
        case "UT" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "ULTD" => Ok(views.html.business_lookup_LTD(limitedCompanyForm, bcContext.user.isAgent, service, businessType, backLink))
        case "NRL" => Ok(views.html.business_lookup_NRL(nonResidentLandlordForm, bcContext.user.isAgent, service, businessType, getNrlBackLink(service)))
      }
  }

  def submit(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
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

  // $COVERAGE-OFF$
  def detailsNotFound(service: String, businessType: String) = AuthAction(service).async { implicit bcContext =>
    Future.successful(Ok(views.html.details_not_found(bcContext.user.isAgent, service, businessType, Some(routes.BusinessVerificationController.businessForm(service, businessType).url))))
  }
  // $COVERAGE-ON$

  private def uibFormHandling(unincorporatedBodyForm: Form[UnincorporatedMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    unincorporatedBodyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_UIB(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      unincorporatedFormData => {
        val organisation = Organisation(unincorporatedFormData.businessName, UnincorporatedBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = unincorporatedFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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
                             (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    soleTraderForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_SOP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      soleTraderFormData => {
        val individual = Individual(soleTraderFormData.firstName, soleTraderFormData.lastName, None)
        businessMatchingService.matchBusinessWithIndividualName(isAnAgent = bcContext.user.isAgent,
          individual = individual, saUTR = soleTraderFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[LimitedLiabilityPartnershipMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest.fold(
      formWithErrors => currentBackLink.map(implicit backLink => BadRequest(views.html.business_lookup_LLP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      llpFormData => {
        val organisation = Organisation(llpFormData.businessName, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = llpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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
                            (implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    limitedPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      lpFormData => {
        val organisation = Organisation(lpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = lpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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

  private def obpFormHandling(ordinaryBusinessPartnershipForm: Form[OrdinaryBusinessPartnershipMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    ordinaryBusinessPartnershipForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_OBP(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      obpFormData => {
        val organisation = Organisation(obpFormData.businessName, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = obpFormData.psaUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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

  private def ltdFormHandling(limitedCompanyForm: Form[LimitedCompanyMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {

    limitedCompanyForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_LTD(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      limitedCompanyFormData => {
        val organisation = Organisation(limitedCompanyFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.cotaxUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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

  private def nrlFormHandling(nrlForm: Form[NonResidentLandlordMatch], businessType: String,
                              service: String, backLink: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {

    nrlForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.business_lookup_NRL(formWithErrors, bcContext.user.isAgent, service, businessType, backLink))),
      nrlFormData => {
        val organisation = Organisation(nrlFormData.businessName, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = bcContext.user.isAgent,
          organisation = organisation, utr = nrlFormData.saUTR, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(reviewDetailsValidated) =>
              RedirectWithBackLink(ReviewDetailsController.controllerId,
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
