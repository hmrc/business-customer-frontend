/*
 * Copyright 2025 HM Revenue & Customs
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
import forms.BusinessName.formats
import forms.BusinessTypeConfig.configs
import forms.BusinessVerificationForms._
import forms._
import models._
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Format
import play.api.libs.json.Format.GenericFormat
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessVerificationController @Inject()(val config: ApplicationConfig,
                                               val authConnector: AuthConnector,
                                               template: views.html.business_verification,
                                               genericBusinessName: views.html.generic_business_name,
                                               genericBusinessUtr: views.html.generic_business_utr,
                                               templateSOP: views.html.business_lookup_SOP,
                                               templateUIB: views.html.business_lookup_UIB,
                                               templateOBP: views.html.business_lookup_OBP,
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
  val services: Seq[String] = Seq("awrs", "ated")

  def businessVerification(service: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      for {
        rawBackLink <- currentBackLink
        backLink: Option[String] = rawBackLink.orElse {
          service match {
            case "awrs" if appConfig.enrolmentJourneyFeature => Some(appConfig.haveYouRegisteredUrl)
            case _ => rawBackLink
          }
        }
        businessTypeDetails <- businessRegCacheConnector.fetchAndGetCachedDetails[BusinessType](s"$CacheRegistrationDetails$service")
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
          if (services.exists(_.equalsIgnoreCase(service))) businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails$service", value)
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
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, busType)))
            case _ =>
              redirectWithBackLink(homeController.controllerId, controllers.routes.HomeController.homePage(service), returnCall)
          }
        }
      )
    }
  }

  /*def viewBusinessNameForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)

      def getCachedFormAndRender[T](form: Form[T], question: String)(implicit formats: Format[T]): Future[Result] = {
        businessRegCacheConnector.fetchAndGetCachedDetails[T](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
          .map {
            case Some(cachedData) => Ok(genericBusinessName(form.fill(cachedData), question, authContext.isAgent, service, businessType, backLink))
            case None => Ok(genericBusinessName(form, question, authContext.isAgent, service, businessType, backLink))
          }
      }

      businessType match {
        case "SOP" => processSoleTraderForm(businessType, service, backLink, authContext)
        case "LTD" | "UT" | "ULTD" => getCachedFormAndRender(form = businessName, question = "bc.business-verification.businessNamefield")
        case "UIB" => processUnincorporatedForm(businessType, service, backLink, authContext)
        case "OBP" => processOrdinaryBusinessPartnershipForm(businessType, service, backLink, authContext)
        case "LLP" => getCachedFormAndRender(form = businessName, question = "bc.business-verification.partnerNameField")
        case "LP" => getCachedFormAndRender(form = businessName, question = "bc.business-verification.partnerNameField")
        case "NRL" => Future.successful(Ok(templateNRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service))))
      }
    }
  }*/

  def viewBusinessNameForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      configs.get(businessType).map { config =>
        val businessNameForm = config.form.asInstanceOf[Form[BusinessName]]
        businessRegCacheConnector.fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
          .map {
            case Some(cachedData) => Ok(genericBusinessName(businessNameForm.fill(cachedData),
              config.questionKey,
              authContext.isAgent,
              service,
              businessType,
              backLink))
            case None => Ok(genericBusinessName(businessNameForm, config.questionKey, authContext.isAgent, service, businessType, backLink))
          }
      }.getOrElse(Future.successful(BadRequest("Invalid business type")))
    }
  }

  /*def submitBusinessNameForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)

      def showErrorPage[T](form: Form[T], question: String)
                                    (implicit formats: Format[T]): Future[Result] = {
        Future.successful(BadRequest(genericBusinessName(form, question, authContext.isAgent, service, businessType, backLink)))
      }

      def redirectToBusinessFormUtr: Future[Result] = {
        Future.successful(Redirect(controllers.routes.BusinessVerificationController.businessFormUtr(service, businessType)))
      }

      businessName.bindFromRequest().fold(
        formWithErrors => {
          businessType match {
            case "SOP" => processSoleTraderForm(businessType, service, backLink, authContext)
            case "LTD" | "UT" | "ULTD" => showErrorPage(formWithErrors, "bc.business-verification.businessNamefield")
            case "UIB" => processUnincorporatedForm(businessType, service, backLink, authContext)
            case "OBP" => processOrdinaryBusinessPartnershipForm(businessType, service, backLink, authContext)
            case "LLP" => showErrorPage(formWithErrors, "bc.business-verification.partnerNameField")
            case "LP" => showErrorPage(formWithErrors, "bc.business-verification.partnerNameField")
            case "NRL" => Future.successful(Ok(templateNRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service))))
          }
        },
        businessNameData => {
          if (services.exists(_.equalsIgnoreCase(service))) {
            businessRegCacheConnector.cacheDetails(s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType", businessNameData)
          }
          businessType match {
            case "SOP" => processSoleTraderForm(businessType, service, backLink, authContext)
            case "LTD" | "UT" | "ULTD" =>
              redirectToBusinessFormUtr
            case "UIB" => processUnincorporatedForm(businessType, service, backLink, authContext)
            case "OBP" => processOrdinaryBusinessPartnershipForm(businessType, service, backLink, authContext)
            case "LLP" =>
              redirectToBusinessFormUtr
            case "LP" => redirectToBusinessFormUtr
            case "NRL" => Future.successful(Ok(templateNRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service))))
          }
        }
      )
    }
  }*/

  def submitBusinessNameForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
      configs.get(businessType) match {
        case Some(config: BusinessTypeConfig.BusinessFormConfig[BusinessName]) =>
          config.form.bindFromRequest().fold(
            formWithErrors => {
              Future.successful(BadRequest(genericBusinessName(formWithErrors, config.questionKey, authContext.isAgent, service, businessType, backLink)))
            },
          businessNameData => {
            val cacheAction =
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType", businessNameData)
              } else {
                Future.unit
              }
            cacheAction.flatMap { _ =>
              Future.successful(Redirect(routes.BusinessVerificationController.viewBusinessFormUtr(service, businessType)))
            }
          }
          )
        case None => Future.successful(BadRequest("Invalid business type"))
      }
    }
  }

  def viewBusinessFormUtr(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>

      /*val backLink = Some(routes.BusinessVerificationController.businessForm(service, businessType).url)

      val cachedBusinessNameFuture: Future[String] = businessRegCacheConnector
        .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
        .map {
          case Some(data) => data.businessName
          case None => throw new RuntimeException("Business name not found in cache")
        }

      def getCachedFormAndRender[T](form: Form[T], heading: String, question: String, businessName: String)(implicit formats: Format[T]): Future[Result] = {
        businessRegCacheConnector.fetchAndGetCachedDetails[T](s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType")
          .map {
            case Some(cachedData) => Ok(genericBusinessUtr(form.fill(cachedData),
              heading,
              question,
              businessName,
              authContext.isAgent,
              service,
              businessType,
              backLink))
            case None => Ok(genericBusinessUtr(form, heading, question, businessName, authContext.isAgent, service, businessType, backLink))
          }
      }

      businessType match {
        case "SOP" => processSoleTraderForm(businessType, service, backLink, authContext)
        case "LTD" | "UT" | "ULTD" => getCachedFormAndRender(utr, "bc.business-verification.coUTR", "bc.business-verification.businessNamefield")
        case "UIB" => processUnincorporatedForm(businessType, service, backLink, authContext)
        case "OBP" => processOrdinaryBusinessPartnershipForm(businessType, service, backLink, authContext)
        case "LLP" => getCachedFormAndRender(utr, "bc.business-verification.psaUTR", "bc.business-verification.psaUTR")
        case "LP" => getCachedFormAndRender(utr, "bc.business-verification.psaUTR", "bc.business-verification.businessNamefield")
        case "NRL" => Future.successful(Ok(templateNRL(nonResidentLandlordForm, authContext.isAgent, service, businessType, getNrlBackLink(service))))
      }
    }*/
      val backLink = Some(routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
      configs.get(businessType).map {
          case config: BusinessTypeConfig.BusinessFormConfig[BusinessName] =>
            for {
              cachedBusinessNameString <- businessRegCacheConnector
                .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
                .map(_.getOrElse(throw new RuntimeException("Business name not found in cache")))
              cachedUtrData <- businessRegCacheConnector
                .fetchAndGetCachedDetails[Utr](s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType")
            } yield {
              val utrForm = cachedUtrData.map(utr.fill).getOrElse(config.form)
              Ok(genericBusinessUtr(utrForm,
                config.utrQuestionKey,
                config.questionKey,
                cachedBusinessNameString.businessName,
                authContext.isAgent,
                service,
                businessType,
                backLink))
            }
          case _ => Future.successful(BadRequest("Invalid business type"))
        }
        .getOrElse(Future.successful(BadRequest("Invalid business type")))
        .recover {
          case _: RuntimeException => InternalServerError("An error occurred while retrieving business details.")
        }
    }
  }


  def submitBusinessUtrForm(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      /*val backLink = Some(routes.BusinessVerificationController.businessFormUtr(service, businessType).url)

      val cachedBusinessNameFuture: Future[String] = businessRegCacheConnector
        .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
        .map {
          case Some(data) => data.businessName
          case None => throw new RuntimeException("Business name not found in cache")
        }
      cachedBusinessNameFuture.flatMap { cachedBusinessNameString =>
        businessType match {
          case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service, backLink)
          case "SOP" => sopFormHandling(soleTraderForm, businessType, service, backLink)
          case "LLP" => llpFormHandling(utr, "bc.business-verification.psaUTR", "bc.business-verification.psaUTR", cachedBusinessNameString, businessType, service, backLink)
          case "LP" => lpFormHandling(utr, "bc.business-verification.psaUTR", "bc.business-verification.psaUTR", cachedBusinessNameString, businessType, service, backLink)
          case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service, backLink)
          case "LTD" => ltdFormHandling(utr, "bc.business-verification.coUTR", "bc.business-verification.businessNamefield", cachedBusinessNameString, businessType, service, backLink)
          case "UT" => ltdFormHandling(utr, "bc.business-verification.coUTR", "bc.business-verification.businessNamefield", cachedBusinessNameString, businessType, service, backLink)
          case "ULTD" => ltdFormHandling(utr, "bc.business-verification.coUTR", "bc.business-verification.businessNamefield", cachedBusinessNameString, businessType, service, backLink)
          case "NRL" => nrlFormHandling(nonResidentLandlordForm, businessType, service, getNrlBackLink(service))
        }
      }
      }*/

      val backLink = Some(routes.BusinessVerificationController.viewBusinessFormUtr(service, businessType).url)
      configs.get(businessType).map {
          case config: BusinessTypeConfig.BusinessFormConfig[BusinessName] =>
            for {
              cachedBusinessNameString <- businessRegCacheConnector
                .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
                .map(_.getOrElse(throw new RuntimeException("Business name not found in cache")))
              result <- handleSubmitUtrForm(config, cachedBusinessNameString.businessName, businessType, service, backLink)
            } yield result
          case _ =>
            Future.successful(BadRequest("Invalid business type"))
        }.getOrElse(Future.successful(BadRequest("Invalid business type")))
        .recover {
          case _: RuntimeException => InternalServerError("An error occurred while retrieving business details.")
        }
    }
  }

  private def handleSubmitUtrForm(
                             config: BusinessTypeConfig.BusinessFormConfig[BusinessName],
                             businessName: String,
                             businessType: String,
                             service: String,
                             backLink: Option[String]
                           )(implicit request: Request[AnyContent], authContext: StandardAuthRetrievals): Future[Result] = {
    businessType match {
      case "LLP" => llpFormHandling(utr, config.utrQuestionKey, config.questionKey, businessName, businessType, service, backLink)
      case "LP" => lpFormHandling(utr, config.utrQuestionKey, config.questionKey, businessName, businessType, service, backLink)
      case "LTD" => ltdFormHandling(utr, config.utrQuestionKey, config.questionKey, businessName, businessType, service, backLink)
      case _ => Future.successful(BadRequest("Invalid business type"))
    }
  }


  private def processSoleTraderForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                   (implicit req: Request[AnyContent]): Future[Result] = {
    if (services.exists(_.equalsIgnoreCase(service))) {
      businessRegCacheConnector.fetchAndGetCachedDetails[SoleTraderMatch](s"$CacheRegistrationDetails${service}_$businessType")
        .map {
          case Some(cachedData) => Ok(templateSOP(soleTraderForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateSOP(soleTraderForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateSOP(soleTraderForm, authContext.isAgent, service, businessType, backLink)))
  }

  private def processUnincorporatedForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                       (implicit req: Request[AnyContent]): Future[Result] = {

    def getCachedFormAndRender[T](form: Form[T], headingField: String)(implicit formats: Format[T]): Future[Result] = {
      businessRegCacheConnector.fetchAndGetCachedDetails[T](s"$CacheRegistrationDetails${service}_$businessType")
        .map {
          case Some(cachedData) => Ok(genericBusinessName(form.fill(cachedData), headingField, authContext.isAgent, service, businessType, backLink))
          case None => Ok(genericBusinessName(form, headingField, authContext.isAgent, service, businessType, backLink))
        }
    }

    import forms.UnincorporatedMatch.formats
    getCachedFormAndRender(unincorporatedBodyForm, Messages("bc.nrl-question"))
  }

  private def processOrdinaryBusinessPartnershipForm(businessType: String, service: String, backLink: Some[String], authContext: StandardAuthRetrievals)
                                                    (implicit req: Request[AnyContent]): Future[Result] = {
    if (services.exists(_.equalsIgnoreCase(service))) {
      businessRegCacheConnector.fetchAndGetCachedDetails[OrdinaryBusinessPartnershipMatch](s"$CacheRegistrationDetails${service}_$businessType")
        .map {
          case Some(cachedData) => Ok(templateOBP(ordinaryBusinessPartnershipForm.fill(cachedData), authContext.isAgent, service, businessType, backLink))
          case None => Ok(templateOBP(ordinaryBusinessPartnershipForm, authContext.isAgent, service, businessType, backLink))
        }
    } else Future.successful(Ok(templateOBP(ordinaryBusinessPartnershipForm, authContext.isAgent, service, businessType, backLink)))
  }

  def submit(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      businessType match {
        case "UIB" => uibFormHandling(unincorporatedBodyForm, businessType, service, backLink)
        case "SOP" => sopFormHandling(soleTraderForm, businessType, service, backLink)
        case "OBP" => obpFormHandling(ordinaryBusinessPartnershipForm, businessType, service, backLink)
        case "NRL" => nrlFormHandling(nonResidentLandlordForm, businessType, service, getNrlBackLink(service))
      }
    }
  }

  def detailsNotFound(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
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
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails${service}_$businessType", unincorporatedFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
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
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails${service}_$businessType", soleTraderFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
              )
            case None =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
          }
        }
      }
    )
  }

  private def llpFormHandling(limitedLiabilityPartnershipForm: Form[Utr],
                              heading: String,
                              question: String,
                              cachedBusinessNameString: String,
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedLiabilityPartnershipForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(genericBusinessUtr(formWithErrors,
        heading,
        question,
        cachedBusinessNameString,
        authContext.isAgent,
        service,
        businessType,
        backLink))),
      llpFormData => {
        val organisation = Organisation(cachedBusinessNameString, Llp)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = llpFormData.utr, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType", llpFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.viewBusinessFormUtr(service, businessType).url)
              )
            case None =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
          }
        }
      }
    )
  }

  private def lpFormHandling(limitedPartnershipForm: Form[Utr], heading: String,
                             question: String,
                             cachedBusinessNameString: String,
                             businessType: String, service: String, backLink: Option[String])
                            (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedPartnershipForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(genericBusinessUtr(formWithErrors,
        heading,
        question,
        cachedBusinessNameString,
        authContext.isAgent,
        service,
        businessType,
        backLink))),
      lpFormData => {
        val organisation = Organisation(cachedBusinessNameString, Partnership)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = lpFormData.utr, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (services.exists(_.equalsIgnoreCase(service))){
                businessRegCacheConnector.cacheDetails(s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType", lpFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.viewBusinessFormUtr(service, businessType).url)
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
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheRegistrationDetails${service}_$businessType", obpFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                Some(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
              )
            case None =>
              Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
          }
        }
      }
    )
  }

  private def ltdFormHandling(limitedCompanyForm: Form[Utr],
                              heading: String,
                              question: String,
                              cachedBusinessNameString: String,
                              businessType: String,
                              service: String,
                              backLink: Option[String])
                             (implicit authContext: StandardAuthRetrievals, req: Request[AnyContent]): Future[Result] = {
    limitedCompanyForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(genericBusinessUtr(formWithErrors,
        heading,
        question,
        cachedBusinessNameString,
        authContext.isAgent,
        service,
        businessType,
        Some(routes.BusinessVerificationController.viewBusinessFormUtr(service, businessType).url)))),
      limitedCompanyFormData => {
        val organisation = Organisation(cachedBusinessNameString, CorporateBody)
        businessMatchingService.matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent,
          organisation = organisation, utr = limitedCompanyFormData.utr, service = service) flatMap { returnedResponse =>
          val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
          validatedReviewDetails match {
            case Some(_) =>
              if (services.exists(_.equalsIgnoreCase(service))) {
                businessRegCacheConnector.cacheDetails(s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType", limitedCompanyFormData)
              }
              redirectWithBackLink(reviewDetailsController.controllerId,
                controllers.routes.ReviewDetailsController.businessDetails(service),
                backLink
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
                  Some(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
                )
              } else {
                redirectWithBackLink(reviewDetailsController.controllerId,
                  controllers.routes.ReviewDetailsController.businessDetails(service),
                  Some(controllers.routes.BusinessVerificationController.viewBusinessNameForm(service, businessType).url)
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
