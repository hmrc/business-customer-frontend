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

import builders.AuthBuilder
import connectors.{BusinessMatchingConnector, DataCacheConnector}
import models._
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BusinessMatchingServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val utr = "1234567890"
  val testAddress = Address("address line 1", "address line 2", Some("address line 3"), Some("address line 4"), Some("AA1 1AA"), "UK")
  val testReviewDetails = ReviewDetails(businessName = "ACME",
    businessType = Some("Limited"),
    businessAddress = testAddress,
    sapNumber = "1234567890",
    safeId = "EX0012345678909",
    isAGroup = false,
    directMatch = false,
    agentReferenceNumber = Some("01234567890"),
    utr = Some(utr))
  val matchFailureResponse = MatchFailureResponse(reason = "Sorry. Business details not found. Try with correct UTR and/or name.")
  val matchFailureResponseJson = Json.toJson(matchFailureResponse)
  val successOrgJson = Json.parse(
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "01234567890",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": false,
      |  "organisation": {
      |    "organisationName": "Real Business Inc",
      |    "isAGroup": true,
      |    "organisationType": "unincorporated body"
      |  },
      |  "address": {
      |    "addressLine1": "address line 1",
      |    "addressLine2": "address line 2",
      |    "addressLine3": "address line 3",
      |    "addressLine4": "address line 4",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "UK"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "1234567890"
      |  }
      | }
    """.stripMargin)

  val successOrgJsonNoOrgType: JsValue = Json.parse(
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "01234567890",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": false,
      |  "organisation": {
      |    "organisationName": "Real Business Inc",
      |    "isAGroup": true
      |  },
      |  "address": {
      |    "addressLine1": "address line 1",
      |    "addressLine2": "address line 2",
      |    "addressLine3": "address line 3",
      |    "addressLine4": "address line 4",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "UK"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "1234567890"
      |  }
      | }
    """.stripMargin)

  val successOrgReviewDetailsDirect = ReviewDetails("Real Business Inc", Some("unincorporated body"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = true, directMatch = true, Some("01234567890"), utr = Some(utr))

  val successOrgReviewDetails = ReviewDetails("Real Business Inc", Some("unincorporated body"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = true, directMatch = false, Some("01234567890"), utr = Some(utr))

  val successOrgReviewDetailsForNoOrgType: ReviewDetails = successOrgReviewDetails.copy(businessType = Some("org type"))

  val successOrgReviewDetailsJsonDirect = Json.toJson(successOrgReviewDetailsDirect)

  val successOrgReviewDetailsJson = Json.toJson(successOrgReviewDetails)

  val successIndividualJson = Json.parse(
    """
      |{
      |  "sapNumber": "1234567890",
      |  "safeId": "EX0012345678909",
      |  "agentReferenceNumber": "01234567890",
      |  "isEditable": true,
      |  "isAnAgent": false,
      |  "isAnIndividual": true,
      |  "individual": {
      |    "firstName": "first name",
      |    "lastName": "last name"
      |  },
      |  "address": {
      |    "addressLine1": "address line 1",
      |    "addressLine2": "address line 2",
      |    "addressLine3": "address line 3",
      |    "addressLine4": "address line 4",
      |    "postalCode": "AA1 1AA",
      |    "countryCode": "UK"
      |  },
      |  "contactDetails": {
      |    "phoneNumber": "1234567890"
      |  }
      |}
    """.stripMargin)

  val successIndReviewDetailsDirect = ReviewDetails("first name last name", Some("Sole Trader"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = false, directMatch = true, Some("01234567890"), Some("first name"), Some("last name"), Some(utr))

  val successIndReviewDetails = ReviewDetails("first name last name", Some("Sole Trader"), testAddress, "1234567890", "EX0012345678909",
    isAGroup = false, directMatch = false, Some("01234567890"), Some("first name"), Some("last name"), Some(utr))

  val successIndReviewDetailsJsonDirect = Json.toJson(successIndReviewDetailsDirect)

  val successIndReviewDetailsJson = Json.toJson(successIndReviewDetails)

  val errorJson = Json.parse( """{"error" : "Some Error"}""")

  val noMatchUtr = "9999999999"

  val testIndividual = Individual("firstName", "lastName", None)

  val testOrganisation = Organisation("org name", "org type")

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val service = "ated"

  val mockBusinessMatchingConnector = mock[BusinessMatchingConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]

  object TestBusinessMatchingService extends BusinessMatchingService(
    mockBusinessMatchingConnector,
    mockDataCacheConnector
  )

  override def beforeEach() = {
    reset(mockBusinessMatchingConnector)
    reset(mockDataCacheConnector)
  }

  "BusinessMatchingService" must {

    "matchBusinessWithUTR" must {

      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(successIndReviewDetailsJsonDirect)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser: StandardAuthRetrievals = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match Not found with SA user and invalid service, return Reasons as JsValue" in {
        implicit val saUser: StandardAuthRetrievals = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, "service")
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match found with CT user, return Review details as JsValue" in {
        implicit val saUser: StandardAuthRetrievals = AuthBuilder.createCtUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successOrgJson))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(successOrgReviewDetailsJsonDirect)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match Not found with CT user, return Reasons as JsValue" in {
        implicit val saUser: StandardAuthRetrievals = AuthBuilder.createCtUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        await(result.get) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for ORG user, return None as JsValue" in {
        implicit val saUse: StandardAuthRetrievals = AuthBuilder.createOrgUser()
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for user with Both SA & CT, return None as JsValue" in {
        implicit val saUser = AuthBuilder.mergeAuthRetrievals(AuthBuilder.createSaUser(), AuthBuilder.createCtUser())
        val result = TestBusinessMatchingService.matchBusinessWithUTR(false, service)
        result must be(None)
        verify(mockBusinessMatchingConnector, times(0)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

    }

    "matchBusinessWithIndividualName" must {

      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr, service)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithIndividualName(false, testIndividual, utr, service)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

    }

    "matchBusinessWithOrganisationName" must {

      "for match found with SA user, return Review details as JsValue" in {
        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successIndividualJson))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        await(result) must be(successIndReviewDetailsJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match Not found with SA user, return Reasons as JsValue" in {
        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(matchFailureResponseJson))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        await(result) must be(matchFailureResponseJson)
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(0)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

      "for match found with SA user, throw an exception when no Safe Id Number" in {
        val successNoSapNo = Json.parse(
          """
            |{
            |  "agentReferenceNumber": "01234567890",
            |  "isEditable": true,
            |  "isAnAgent": false,
            |  "isAnIndividual": true,
            |  "individual": {
            |    "firstName": "first name",
            |    "lastName": "last name"
            |  },
            |  "address": {
            |    "addressLine1": "address line 1",
            |    "addressLine2": "address line 2",
            |    "addressLine3": "address line 3",
            |    "addressLine4": "address line 4",
            |    "postalCode": "AA1 1AA",
            |    "countryCode": "UK"
            |  },
            |  "contactDetails": {
            |    "phoneNumber": "1234567890"
            |  }
            |}
          """.stripMargin)

        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successNoSapNo))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("No Safe Id returned from ETMP")

      }

      "for match found with SA user, throw an exception when no Address" in {
        val successNoSapNo = Json.parse(
          """
            |{
            |  "sapNumber": "1234567890",
            |  "safeId": "EX0012345678909",
            |  "agentReferenceNumber": "01234567890",
            |  "isEditable": true,
            |  "isAnAgent": false,
            |  "isAnIndividual": true,
            |  "individual": {
            |    "firstName": "first name",
            |    "lastName": "last name"
            |  },
            |  "contactDetails": {
            |    "phoneNumber": "1234567890"
            |  }
            |}
          """.stripMargin)

        implicit val saUser = AuthBuilder.createSaUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successNoSapNo))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        val thrown = the[RuntimeException] thrownBy await(result)
        thrown.getMessage must include("No Address returned from ETMP")

      }

      "for match found with CT user, return Review details as JsValue" in {
        implicit val ctUser = AuthBuilder.createCtUser()

        when(mockBusinessMatchingConnector.lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(successOrgJsonNoOrgType))
        when(mockDataCacheConnector.saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some(testReviewDetails)))
        val result = TestBusinessMatchingService.matchBusinessWithOrganisationName(false, testOrganisation, utr, service)
        await(result) must be(Json.toJson(successOrgReviewDetailsForNoOrgType))
        verify(mockBusinessMatchingConnector, times(1)).lookup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        verify(mockDataCacheConnector, times(1)).saveReviewDetails(ArgumentMatchers.any())(ArgumentMatchers.any())
      }

    }

  }

}
