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
import forms.BusinessVerificationForms.{businessName, partnerships, soleTraderNameForm}
import forms.{BusinessName, SoleTraderName}
import jakarta.inject.Singleton
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.BusinessCustomerConstants.CacheBusinessNameDataRegistrationDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessNameController @Inject() (
  val config: ApplicationConfig,
  val authConnector: AuthConnector,
  genericBusinessName: views.html.generic_business_name,
  businessRegCacheConnector: BusinessRegCacheConnector,
  val backLinkCacheConnector: BackLinkCacheConnector,
  val mcc: MessagesControllerComponents
) extends FrontendController(mcc)
  with AuthActions
  with BackLinkController
  with I18nSupport {
  override implicit val appConfig: ApplicationConfig = config
  override val controllerId: String                  = "BusinessNameController"
  implicit val executionContext: ExecutionContext    = mcc.executionContext
  val services: Seq[String]                          = Seq("awrs", "ated", "amls", "fhdds")

  def onPageLoad(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessVerificationController.businessVerification(service).url)
      if (businessType == "SOP") {
        val questionKey = "bc.business-verification.SoleNameField"
        businessRegCacheConnector
          .fetchAndGetCachedDetails[SoleTraderName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
          .map {
            case Some(cached) =>
              Ok(
                genericBusinessName(
                  forms.BusinessVerificationForms.soleTraderNameForm.fill(cached),
                  questionKey,
                  authContext.isAgent,
                  service,
                  businessType,
                  backLink
                )
              )
            case None =>
              Ok(
                genericBusinessName(
                  forms.BusinessVerificationForms.soleTraderNameForm,
                  questionKey,
                  authContext.isAgent,
                  service,
                  businessType,
                  backLink
                )
              )
          }
      } else {
        val header = s"bc.business-verification.${getBusinessType(businessType)}NameField"
        businessRegCacheConnector
          .fetchAndGetCachedDetails[BusinessName](s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType")
          .map {
            case Some(cachedData) =>
              Ok(genericBusinessName(businessName(businessType).fill(cachedData), header, authContext.isAgent, service, businessType, backLink))
            case None =>
              Ok(genericBusinessName(businessName(businessType), header, authContext.isAgent, service, businessType, backLink))
          }
      }
    }
  }

  def submit(service: String, businessType: String): Action[AnyContent] = Action.async { implicit request =>
    authorisedFor(service) { implicit authContext =>
      val backLink = Some(routes.BusinessNameController.onPageLoad(service, businessType).url)
      if (businessType == "SOP") {
        val questionKey = "bc.business-verification.SoleNameField"
        soleTraderNameForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(
              BadRequest(genericBusinessName(formWithErrors, questionKey, authContext.isAgent, service, businessType, backLink))),
            nameData => {
              val cacheAction = businessRegCacheConnector.cacheDetails(
                s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType",
                nameData
              )
              cacheAction.map { _ =>
                Redirect(routes.BusinessUtrController.onPageLoad(service, businessType))
              }
            }
          )
      } else {
        val header = s"bc.business-verification.${getBusinessType(businessType)}NameField"
        businessName(businessType)
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(genericBusinessName(formWithErrors, header, authContext.isAgent, service, businessType, backLink))),
            businessNameData => {
              val cacheAction =
                businessRegCacheConnector.cacheDetails(s"$CacheBusinessNameDataRegistrationDetails${service}_$businessType", businessNameData)
              cacheAction.map { _ =>
                Redirect(routes.BusinessUtrController.onPageLoad(service, businessType))
              }
            }
          )
      }
    }
  }
  private def getBusinessType(businessType: String): String = if (partnerships.contains(businessType)) "partnership" else "business"

}
