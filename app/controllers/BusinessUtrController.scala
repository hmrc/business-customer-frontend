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
import forms.BusinessVerificationForms._
import forms._
import jakarta.inject.Singleton
import models.{Individual, Organisation, ReviewDetails, StandardAuthRetrievals}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessUtrController @Inject() (
  val config: ApplicationConfig,
  val authConnector: AuthConnector,
  genericBusinessUtr: views.html.generic_business_utr,
  businessRegCacheConnector: BusinessRegCacheConnector,
  val backLinkCacheConnector: BackLinkCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val reviewDetailsController: ReviewDetailsController,
  val mcc: MessagesControllerComponents
) extends FrontendController(mcc)
  with AuthActions
  with BackLinkController
  with I18nSupport {
  override implicit val appConfig: ApplicationConfig = config
  override val controllerId: String                  = "BusinessNameController"
  implicit val executionContext: ExecutionContext    = mcc.executionContext
  val services: Seq[String]                          = Seq("awrs", "ated", "amls")

  def onPageLoad(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessNameController.onPageLoad(service, businessType).url)
      if (selfAssessment.contains(businessType)) {
        for {
          cachedUtr <- businessRegCacheConnector
            .fetchAndGetCachedDetails[Utr](s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType")
        } yield {
          val utrFormFilled: Form[Utr] = cachedUtr.map(saUtr.fill).getOrElse(saUtr)
          val headingKey               = "bc.business-verification.saUTR"
          Ok(
            genericBusinessUtr(
              utrFormFilled,
              headingKey,
              authContext.isAgent,
              service,
              businessType,
              backLink
            )
          )
        }
      } else {
          val heading = s"bc.business-verification.${getBusinessType(businessType)}UTR"
          val utrForm: Form[Utr] =
            if (partnerships.contains(businessType)) psaUtr else cotaxUtr
          for {
            cachedUtr <- businessRegCacheConnector
              .fetchAndGetCachedDetails[Utr](s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType")
          } yield {
            val utrFormFilled: Form[Utr] = cachedUtr.map(utrForm.fill).getOrElse(utrForm)
            Ok(
              genericBusinessUtr(
                utrFormFilled,
                heading,
                authContext.isAgent,
                service,
                businessType,
                backLink
              )
            )
          }
      }
    }
  }

  def submit(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val utrForm: Form[Utr] = if (selfAssessment.contains(businessType)) { saUtr }
      else if (corporations.contains(businessType)) {
        cotaxUtr
      } else { psaUtr }
      val backLink = Some(routes.BusinessUtrController.onPageLoad(service, businessType).url)
      if (businessType == "SOP") {
        for {
          name <- businessRegCacheConnector
            .fetchAndGetCachedDetails[SoleTraderName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
            .map(_.getOrElse(throw new RuntimeException("Sole Trader name not found in cache")))
          result <- handleSubmitUtrFormSoleTrader( SoleTraderName(name.firstName, name.lastName), service, businessType, backLink)
        } yield result
      }.recover { case _: RuntimeException =>
        logger.error("[BusinessUtrController][submit] An error occurred while retrieving name details")
        InternalServerError("[BusinessUtrController][submit] An error occurred while retrieving name details")
      } else {
          for {
            cachedBusinessName <- businessRegCacheConnector
              .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
              .map(_.getOrElse(throw new RuntimeException("Business name not found in cache")))
            result <- handleSubmitUtrForm(utrForm, cachedBusinessName.businessName, businessType, service, backLink)
          } yield result
        }.recover { case _: RuntimeException =>
            logger.error("[BusinessUtrController][submit] An error occurred while retrieving name details")
            InternalServerError("[BusinessUtrController][submit] An error occurred while retrieving name details")}
    }
  }


  private def handleSubmitUtrForm(
    utrForm: Form[Utr],
    businessName: String,
    businessType: String,
    service: String,
    backLink: Option[String]
  )(implicit request: Request[AnyContent], authContext: StandardAuthRetrievals): Future[Result] = {
    val heading = s"bc.business-verification.${getBusinessType(businessType)}UTR"
    utrForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              genericBusinessUtr(
                formWithErrors,
                heading,
                authContext.isAgent,
                service,
                businessType,
                Some(routes.BusinessUtrController.onPageLoad(service, businessType).url)
              )
            )
          ),
        utrFormData => {
          val organisation = Organisation(businessName, CorporateBody)
          businessMatchingService
            .matchBusinessWithOrganisationName(isAnAgent = authContext.isAgent, organisation = organisation, utr = utrFormData.utr, service = service) flatMap { returnedResponse =>
            val validatedReviewDetails = returnedResponse.validate[ReviewDetails].asOpt
            validatedReviewDetails match {
              case Some(details) if businessType.equalsIgnoreCase("NRL") => handleNrlRedirect(details, service, businessType, utrFormData)
              case Some(_)                                               =>
                if (services.exists(_.equalsIgnoreCase(service))) {
                  businessRegCacheConnector.cacheDetails(s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType", utrFormData)
                }
                redirectWithBackLink(reviewDetailsController.controllerId, controllers.routes.ReviewDetailsController.businessDetails(service), backLink)
              case None =>
                Future.successful(Redirect(controllers.routes.BusinessVerificationController.detailsNotFound(service, businessType)))
            }
          }
        }
      )
  }

  private def handleNrlRedirect(
    reviewDetails: ReviewDetails,
    service: String,
    businessType: String,
    utrFormData: Utr
  )(implicit request: Request[AnyContent]): Future[Result] = {
    val countryCode = reviewDetails.businessAddress.country
    if (config.getSelectedCountry(countryCode) == countryCode && service == "ATED") {
      businessRegCacheConnector.cacheDetails(s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType", utrFormData)
      businessRegCacheConnector.cacheDetails(UpdateNotRegisterId, true)
      redirectWithBackLink(
        reviewDetailsController.controllerId,
        controllers.nonUKReg.routes.BusinessRegController.register(service, businessType),
        Some(controllers.routes.BusinessUtrController.onPageLoad(service, businessType).url)
      )
    } else {
      redirectWithBackLink(
        reviewDetailsController.controllerId,
        controllers.routes.ReviewDetailsController.businessDetails(service),
        Some(controllers.routes.BusinessUtrController.onPageLoad(service, businessType).url)
      )
    }
  }

  private def handleSubmitUtrFormSoleTrader(
    soleName: SoleTraderName,
    service: String,
    businessType: String,
    backLink: Option[String]
  )(implicit request: Request[AnyContent], authContext: StandardAuthRetrievals): Future[Result] = {
    val headingKey = "bc.business-verification.saUTR"
    saUtr.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(
          BadRequest(
            genericBusinessUtr(
              formWithErrors,
              headingKey,
              authContext.isAgent,
              service,
              businessType,
              backLink
            )
          )
        ),
      utrData => {
        val individual = Individual(soleName.firstName, soleName.lastName, None)
        val utrValue   = utrData.utr.trim
        businessMatchingService
          .matchBusinessWithIndividualName(authContext.isAgent, individual, utrValue, service)
          .flatMap { returnedJson =>
            val validated = returnedJson.validate[ReviewDetails].asOpt
            validated match {
              case Some(_)  =>
                if (services.exists(_.equalsIgnoreCase(service))) {
                  businessRegCacheConnector.cacheDetails(
                    s"$CacheBusinessUtrDataRegistrationDetails${service}_$businessType",
                    utrData
                  )
                }
                redirectWithBackLink(
                  reviewDetailsController.controllerId,
                  controllers.routes.ReviewDetailsController.businessDetails(service),
                  Some(routes.BusinessUtrController.onPageLoad(service, businessType).url)
                )
              case None =>
                Future.successful(
                  Redirect(routes.BusinessVerificationController.detailsNotFound(service, businessType))
                )
            }
          }
      }
    )
  }
  private def getBusinessType(businessType: String): String = if (partnerships.contains(businessType)) "psa" else "co"

}
