/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.UUID

import builders.AuthBuilder
import connectors.{BusinessCustomerConnector, DataCacheConnector}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRegistrationServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val user = AuthBuilder.createUserAuthContext("userId", "joe bloggs")
  val mockDataCacheConnector = mock[DataCacheConnector]
  val service = "ATED"

  val mockBusinessCustomerConnector = mock[BusinessCustomerConnector]

  object TestBusinessRegistrationService extends BusinessRegistrationService {
    val businessCustomerConnector: BusinessCustomerConnector = mockBusinessCustomerConnector
    val dataCacheConnector = mockDataCacheConnector
    val nonUKBusinessType = "Non UK-based Company"
  }
  
  override def beforeEach(): Unit = {
    reset(mockDataCacheConnector)
    reset(mockBusinessCustomerConnector)
  }

  "BusinessRegistrationService" must {

    "use the correct data cache connector" in {
      BusinessRegistrationService.dataCacheConnector must be(DataCacheConnector)
    }

    "use the correct business Customer Connector" in {
      BusinessRegistrationService.businessCustomerConnector must be(BusinessCustomerConnector)
    }
  }

  "getDetails" must {

    val failureResponse = Json.parse( """{"reason":"Agent not found!"}""")

    val identifier = "JARN1234567"
    val identifierType = "arn"
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    case class Identification(idNumber: String, issuingInstitution: String, issuingCountryCode: String)

    val reviewDetails = ReviewDetails(businessName = "ABC",
      businessType = Some("corporate body"),
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"),
      identification = Some(models.Identification(idNumber = "123345", issuingInstitution = "IssInst", issuingCountryCode = "FR"))
    )

    "for OK response status, return body as Some(BusinessRegistration) from json with a nonUKIdentification" in {

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(reviewDetails)))
      val result = TestBusinessRegistrationService.getDetails()

      val busReg = await(result)
      busReg.isDefined must be (true)
      busReg.get._1 must be ("corporate body")

      val busRegDetails = busReg.get._2

      busRegDetails.businessName must be ("ABC")
      busRegDetails.businessAddress.line_1 must be ("line1")
      busRegDetails.businessAddress.line_2 must be ("line2")
      busRegDetails.businessAddress.line_3 must be (None)
      busRegDetails.businessAddress.line_4 must be (None)
      busRegDetails.businessAddress.postcode must be (None)
      busRegDetails.businessAddress.country must be ("GB")

      val overseasCompany = busReg.get._3
      overseasCompany.hasBusinessUniqueId must be (Some(true))
      overseasCompany.businessUniqueId must be (Some("123345"))
      overseasCompany.issuingCountry must be ( Some("FR"))
      overseasCompany.issuingInstitution must be (Some("IssInst"))
    }

  }


  "registerBusiness" must {
    val nonUKResponse = BusinessRegistrationResponse(processingDate = "2015-01-01",
              sapNumber = "SAP123123",
              safeId = "SAFE123123",
              agentReferenceNumber = Some("AREF123123"))

    "save the response from the registration" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )

      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration, when an agent registers non-uk based client" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = true, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration when we have no businessUniqueId or issuingInstitution" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(false),
        businessUniqueId = None,
        issuingInstitution = None,
        issuingCountry = None
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))

      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)
      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "throw exception when registration fails" in {
      implicit val hc = new HeaderCarrier(sessionId = None)
      when(mockBusinessCustomerConnector.register(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future(nonUKResponse))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        hasBusinessUniqueId = Some(true),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = None
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))


      val regResult = TestBusinessRegistrationService.registerBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val thrown = the[InternalServerException] thrownBy await(regResult)
      thrown.getMessage must include("Registration Failed")
    }
  }

  "updateBusiness" must {
    val successResponse = Json.parse( """{"processingDate": "2014-12-17T09:30:47Z"}""")

    val cachedReviewDetails = ReviewDetails(businessName = "ABC",
      businessType = Some("corporate body"),
      businessAddress = Address(line_1 = "line1", line_2 = "line2", line_3 = None, line_4 = None, postcode = None, country = "GB"),
      sapNumber = "1234567890", safeId = "XW0001234567890",false, agentReferenceNumber = Some("JARN1234567"),
      identification = Some(models.Identification(idNumber = "123345", issuingInstitution = "IssInst", issuingCountryCode = "FR"))
    )

    "save the response from the update" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(cachedReviewDetails)))
      when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))

      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.updateRegisterBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration, when an agent registers non-uk based client" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(cachedReviewDetails)))
      when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(true),
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = Some("GB")
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))


      val regResult = TestBusinessRegistrationService.updateRegisterBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = true, service)

      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "save the response from the registration when we have no businessUniqueId or issuingInstitution" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(cachedReviewDetails)))
      when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        hasBusinessUniqueId = Some(false),
        businessUniqueId = None,
        issuingInstitution = None,
        issuingCountry = None
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedReviewDetails)))

      val regResult = TestBusinessRegistrationService.updateRegisterBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)
      val reviewDetails = await(regResult)

      reviewDetails.businessName must be(busRegData.businessName)
      reviewDetails.businessAddress.line_1 must be(busRegData.businessAddress.line_1)
    }

    "throw an exception if no data is found" in {
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(None))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        hasBusinessUniqueId = Some(true),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = None
      )
      val regResult = TestBusinessRegistrationService.updateRegisterBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val thrown = the[InternalServerException] thrownBy await(regResult)
      thrown.getMessage must include("Update Registration Failed")
    }

    "throw exception when registration fails" in {
      implicit val hc = new HeaderCarrier(sessionId = None)
      when(mockDataCacheConnector.fetchAndGetBusinessDetailsForSession(Matchers.any())).thenReturn(Future.successful(Some(cachedReviewDetails)))
      when(mockBusinessCustomerConnector.updateRegistrationDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(successResponse))))
      val busRegData = BusinessRegistration(businessName = "testName",
        businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
      )
      val overseasCompany = OverseasCompany(
        businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
        hasBusinessUniqueId = Some(true),
        issuingInstitution = Some("issuingInstitution"),
        issuingCountry = None
      )
      val returnedReviewDetails = new ReviewDetails(businessName = busRegData.businessName, businessType = None, businessAddress = busRegData.businessAddress,
        sapNumber = "sap123", safeId = "safe123", isAGroup = false, agentReferenceNumber = Some("agent123"))
      when(mockDataCacheConnector.saveReviewDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))


      val regResult = TestBusinessRegistrationService.updateRegisterBusiness(busRegData, overseasCompany, isGroup = true, isNonUKClientRegisteredByAgent = false, service)

      val thrown = the[InternalServerException] thrownBy await(regResult)
      thrown.getMessage must include("Update Registration Failed")
    }
  }
}
