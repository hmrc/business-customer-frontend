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

package services

import connectors.{BusinessCustomerConnector, DataCacheConnector}

import javax.inject.Inject
import models._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.SessionUtils

import scala.concurrent.{ExecutionContext, Future}


class BusinessRegistrationService @Inject()(val businessCustomerConnector: BusinessCustomerConnector,
                                            val dataCacheConnector: DataCacheConnector){

  val nonUKBusinessType = "Non UK-based Company"
  def registerBusiness(registerData: BusinessRegistration,
                       overseasCompany: OverseasCompany,
                       isGroup: Boolean,
                       isNonUKClientRegisteredByAgent: Boolean = false,
                       service: String,
                       isBusinessDetailsEditable: Boolean = false)
                      (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[ReviewDetails] = {

    val businessRegisterDetails = createBusinessRegistrationRequest(registerData, overseasCompany, isGroup, isNonUKClientRegisteredByAgent)

    for {
      registerResponse <- businessCustomerConnector.register(businessRegisterDetails, service, isNonUKClientRegisteredByAgent)
      reviewDetailsCache <- {
        val reviewDetails = createReviewDetails(registerResponse.sapNumber,
          registerResponse.safeId, registerResponse.agentReferenceNumber, isGroup, registerData, overseasCompany, isBusinessDetailsEditable)
        dataCacheConnector.saveReviewDetails(reviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException("Registration failed"))
    }
  }


  def updateRegisterBusiness(registerData: BusinessRegistration,
                             overseasCompany: OverseasCompany,
                             isGroup: Boolean,
                             isNonUKClientRegisteredByAgent: Boolean = false,
                             service: String,
                             isBusinessDetailsEditable: Boolean = false)
                            (implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[ReviewDetails] = {

    val updateRegisterDetails = createUpdateBusinessRegistrationRequest(registerData, overseasCompany, isGroup, isNonUKClientRegisteredByAgent)

    for {
      oldReviewDetailsLookup <- dataCacheConnector.fetchAndGetBusinessDetailsForSession
      oldReviewDetails       <- oldReviewDetailsLookup match {
        case Some(reviewDetails) => Future.successful(reviewDetails)
        case _ => throw new InternalServerException("Update registration failed")}
      _ <- businessCustomerConnector.updateRegistrationDetails(oldReviewDetails.safeId, updateRegisterDetails)
      reviewDetailsCache <- {
        val updatedReviewDetails = createReviewDetails(oldReviewDetails.sapNumber,
          oldReviewDetails.safeId,
          oldReviewDetails.agentReferenceNumber,
          isGroup,
          registerData,
          overseasCompany,
          isBusinessDetailsEditable)
        dataCacheConnector.saveReviewDetails(updatedReviewDetails)
      }
    } yield {
      reviewDetailsCache.getOrElse(throw new InternalServerException("Registration failed"))
    }
  }


  def getDetails()(implicit authContext: StandardAuthRetrievals, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[(String, BusinessRegistration, OverseasCompany)]] = {

    def createBusinessRegistration(reviewDetailsOpt: Option[ReviewDetails]) : Option[(String, BusinessRegistration, OverseasCompany)] = {
      reviewDetailsOpt.flatMap( details =>
        details.businessType.map{ busType =>

          val overseasCompany = OverseasCompany(Some(details.identification.isDefined),
            details.identification.map(_.idNumber),
            details.identification.map(_.issuingInstitution),
            details.identification.map(_.issuingCountryCode))

          (busType, BusinessRegistration(details.businessName, details.businessAddress), overseasCompany)
        }
      )
    }
    dataCacheConnector.fetchAndGetBusinessDetailsForSession map createBusinessRegistration
  }




  private def createUpdateBusinessRegistrationRequest(registerData: BusinessRegistration,
                                                      overseasCompany: OverseasCompany,
                                                      isGroup: Boolean,
                                                      isNonUKClientRegisteredByAgent: Boolean)
                                                     (implicit authContext: StandardAuthRetrievals): UpdateRegistrationDetailsRequest = {

    UpdateRegistrationDetailsRequest(
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      isAnIndividual = false,
      individual = None,
      organisation = Some(EtmpOrganisation(organisationName = registerData.businessName)),
      address = getEtmpBusinessAddress(registerData.businessAddress),
      contactDetails = EtmpContactDetails(),
      isAnAgent = if (isNonUKClientRegisteredByAgent) false else authContext.isAgent,
      isAGroup = isGroup,
      identification = getEtmpIdentification(overseasCompany, registerData.businessAddress)
    )
  }

  private def createBusinessRegistrationRequest(registerData: BusinessRegistration,
                                                overseasCompany: OverseasCompany,
                                                isGroup: Boolean,
                                                isNonUKClientRegisteredByAgent: Boolean)
                                               (implicit authContext: StandardAuthRetrievals): BusinessRegistrationRequest = {

    BusinessRegistrationRequest(
      acknowledgementReference = SessionUtils.getUniqueAckNo,
      organisation = EtmpOrganisation(organisationName = registerData.businessName),
      address = getEtmpBusinessAddress(registerData.businessAddress),
      isAnAgent = if (isNonUKClientRegisteredByAgent) false else authContext.isAgent,
      isAGroup = isGroup,
      identification = getEtmpIdentification(overseasCompany, registerData.businessAddress),
      contactDetails = EtmpContactDetails()
    )
  }


  private def createReviewDetails(sapNumber: String, safeId: String,
                                  agentReferenceNumber: Option[String],
                                  isGroup: Boolean,
                                  registerData: BusinessRegistration,
                                  overseasCompany: OverseasCompany,
                                  isBusinessDetailsEditable: Boolean): ReviewDetails = {

    val identification = overseasCompany.businessUniqueId.map( busUniqueId =>
      Identification(busUniqueId,
        overseasCompany.issuingInstitution.getOrElse(""),
        overseasCompany.issuingCountry.getOrElse("")
      )
    )

    val updatedAddress = registerData.businessAddress
    ReviewDetails(businessName = registerData.businessName,
      businessType = Some(nonUKBusinessType),
      businessAddress = updatedAddress.copy(postcode = updatedAddress.postcode.map(_.toUpperCase)),
      sapNumber = sapNumber,
      safeId = safeId,
      isAGroup = isGroup,
      agentReferenceNumber = agentReferenceNumber,
      identification = identification,
      isBusinessDetailsEditable = isBusinessDetailsEditable
    )
  }


  private def getEtmpBusinessAddress(businessAddress: Address) = {
    EtmpAddress(addressLine1 = businessAddress.line_1,
      addressLine2 = businessAddress.line_2,
      addressLine3 = businessAddress.line_3,
      addressLine4 = businessAddress.line_4,
      postalCode = businessAddress.postcode.map(_.toUpperCase),
      countryCode = businessAddress.country)
  }

  private def getEtmpIdentification(overseasCompany: OverseasCompany, businessAddress: Address): Option[EtmpIdentification] = {
    if (overseasCompany.businessUniqueId.isDefined || overseasCompany.issuingInstitution.isDefined) {
      Some(
        EtmpIdentification(
          idNumber = overseasCompany.businessUniqueId.getOrElse(""),
          issuingInstitution = overseasCompany.issuingInstitution.getOrElse(""),
          issuingCountryCode = overseasCompany.issuingCountry.getOrElse(businessAddress.country)
        )
      )
    } else None
  }
}
