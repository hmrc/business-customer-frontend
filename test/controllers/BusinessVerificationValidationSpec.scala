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

import java.util.UUID

import connectors.BackLinkCacheConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessMatchingService
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.SessionKeys

import scala.concurrent.Future


class BusinessVerificationValidationSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val request = FakeRequest()
  val mockBusinessMatchingService = mock[BusinessMatchingService]
  val mockAuthConnector = mock[AuthConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val service = "ATED"
  val matchUtr = new SaUtrGenerator().nextSaUtr
  val noMatchUtr = new SaUtrGenerator().nextSaUtr

  val matchSuccessResponseUIB = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Unincorporated body",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLTD = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited company",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseNRL = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited company",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseSOP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Sole trader",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseOBP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Ordinary business partnership",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLLP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited liability partnership",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLP = Json.parse(
    """
      |{
      |  "businessName": "ACME",
      |  "businessType": "Limited partnership",
      |  "businessAddress": {
      |    "line_1": "line 1",
      |    "line_2": "line 2",
      |    "line_3": "line 3",
      |    "line_4": "line 4",
      |    "postcode": "AA1 1AA",
      |    "country": "UK"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchFailureResponse = Json.parse( """{"reason":"Sorry. Business details not found. Try with correct UTR and/or name."}""")

  def submitWithUnAuthorisedUser(businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessVerificationController.submit(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  "BusinessVerificationController" must {

    type InputRequest = FakeRequest[AnyContentAsFormUrlEncoded]
    type MustTestMessage = String
    type InTestMessage = String
    type ErrorMessage = String
    type BusinessType = String

    def nrlUtrRequest(utr: String = matchUtr.utr, businessName: String = "ACME") = request.withFormUrlEncodedBody("saUTR" -> s"$utr", "businessName" -> s"$businessName")
    def ctUtrRequest(ct: String = matchUtr.utr, businessName: String = "ACME") = request.withFormUrlEncodedBody("cotaxUTR" -> s"$ct", "businessName" -> s"$businessName")
    def psaUtrRequest(psa: String = matchUtr.utr, businessName: String = "ACME") = request.withFormUrlEncodedBody("psaUTR" -> s"$psa", "businessName" -> s"$businessName")
    def saUtrRequest(sa: String = matchUtr.utr, firstName: String = "A", lastName: String = "B") = request.withFormUrlEncodedBody("saUTR" -> s"$sa", "firstName" -> s"$firstName", "lastName" -> s"$lastName")

    val formValidationInputDataSetOrg: Seq[(MustTestMessage, Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)])] =
      Seq(
        ("if the selection is Unincorporated body :",
          Seq(
            ("Business Name must not be empty", "UIB", ctUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("CO Tax UTR must not be empty", "UIB", ctUtrRequest(ct = ""), "You must enter a Corporation Tax Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "UIB", ctUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "UIB", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "UIB", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "UIB", ctUtrRequest(ct = "1234567890"), "The Corporation Tax Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Company :",
          Seq(
            ("Business Name must not be empty", "LTD", ctUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("CO Tax UTR must not be empty", "LTD", ctUtrRequest(ct = ""), "You must enter a Corporation Tax Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LTD", ctUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "LTD", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "LTD", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "LTD", ctUtrRequest(ct = "1234567890"), "The Corporation Tax Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Non Resident Landlord :",
          Seq(
            ("Business Name must not be empty", "NRL", nrlUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("SA UTR must not be empty", "NRL", nrlUtrRequest(utr = ""), "You must enter a Self Assessment Unique Taxpayer Reference"),
            ("SA UTR must be 10 digits", "NRL", nrlUtrRequest(utr = "12345678901"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("SA UTR must contain only digits", "NRL", nrlUtrRequest(utr = "12345678aa"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("SA UTR must be valid", "NRL", nrlUtrRequest(utr = "1234567890"), "The Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Liability Partnership : ",
          Seq(
            ("Business Name must not be empty", "LLP", psaUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("Partnership Self Assessment UTR  must not be empty", "LLP", psaUtrRequest(psa = ""), "You must enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LLP", psaUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LLP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LLP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LLP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Partnership : ",
          Seq(
            ("Business Name must not be empty", "LP", psaUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("Partnership Self Assessment UTR  must not be empty", "LP", psaUtrRequest(psa = ""), "You must enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LP", psaUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Ordinary Business Partnership : ",
          Seq(
            ("Business Name must not be empty", "OBP", psaUtrRequest(businessName = ""), "You must enter a registered company name"),
            ("Partnership Self Assessment UTR  must not be empty", "OBP", psaUtrRequest(psa = ""), "You must enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "OBP", psaUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "OBP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "OBP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "OBP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          )
      )

    formValidationInputDataSetOrg foreach { dataSet =>
      s"${dataSet._1}" must {
        dataSet._2 foreach { inputData =>
          s"${inputData._1}" in {
            submitWithAuthorisedUserSuccessOrg(inputData._2, inputData._3) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(s"${inputData._4}")
            }
          }
        }
      }
    }

    val formValidationInputDataSetInd: Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)] =
      Seq(
        ("First name must not be empty", "SOP", saUtrRequest(matchUtr.utr, "", "b"), "You must enter a first name"),
        ("Last name must not be empty", "SOP", saUtrRequest(lastName = ""), "You must enter a last name"),
        ("SA UTR must not be empty", "SOP", saUtrRequest(sa = ""), "You must enter a Self Assessment Unique Taxpayer Reference"),
        ("First Name must not be more than 40 characters", "SOP", saUtrRequest(firstName = "a" * 41), "A first name cannot be more than 40 characters"),
        ("Last Name must not be more than 40 characters", "SOP", saUtrRequest(lastName = "a" * 41), "A last name cannot be more than 40 characters"),
        ("SA UTR must be 10 digits", "SOP", saUtrRequest(sa = "12345678901"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
        ("SA UTR must contain only digits", "SOP", saUtrRequest(sa = "12345678aa"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
        ("SA UTR must be valid", "SOP", saUtrRequest(sa = "1234567890"), "The Self Assessment Unique Taxpayer Reference is not valid")
      )

    "if the selection is Sole Trader:" must {

      formValidationInputDataSetInd foreach { dataSet =>
        s"${dataSet._1}" in {
          submitWithAuthorisedUserSuccessIndividual(dataSet._2, dataSet._3) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include(s"${dataSet._4}")
          }
        }
      }

    }


    "if the Ordinary Business Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("OBP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        submitWithAuthorisedUserFailure("OBP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/OBP")
        }
      }
    }

    "if the Limited Liability Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LLP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        submitWithAuthorisedUserFailure("LLP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LLP")
        }
      }
    }

    "if the Limited Partnership form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        submitWithAuthorisedUserFailure("LP", request.withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LP")
        }
      }
    }

    "if the Sole Trader form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "First Name", "lastName" -> "Last Name", "saUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        submitWithAuthorisedUserFailureIndividual("SOP", request.withFormUrlEncodedBody("firstName" -> "First Name", "lastName" -> "Last Name", "saUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/SOP")
        }
      }
    }

    "if the Non Resident Landlord is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("NRL", request.withFormUrlEncodedBody("businessName" -> "Business Name", "saUTR" -> s"$matchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and  user should be on details not found page" in {
        submitWithAuthorisedUserFailure("NRL", request.withFormUrlEncodedBody("businessName" -> "Business Name", "saUTR" -> s"$noMatchUtr")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/NRL")
        }
      }
    }


    "if the Unincorporated body form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and  user should be on same details not found page" in {
        submitWithAuthorisedUserFailure("UIB", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/UIB")
        }
      }
    }

    "if the Limited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        submitWithAuthorisedUserFailure("LTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LTD")
        }
      }
    }

    "if the Unit Trust form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("UT", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserFailure("UT", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/UT")
        }
      }
    }

    "if the Unlimited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in {
        submitWithAuthorisedUserSuccessOrg("ULTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserFailure("ULTD", request.withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name")) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/ULTD")
        }
      }
    }

  }

  def submitWithAuthorisedUserSuccessOrg(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val matchSuccessResponse = businessType match {
      case "UIB" => matchSuccessResponseUIB
      case "LLP" => matchSuccessResponseLLP
      case "OBP" => matchSuccessResponseOBP
      case "NRL" => matchSuccessResponseNRL
      case "LTD" => matchSuccessResponseLTD
      case "LP" => matchSuccessResponseLP
      case "UT" => matchSuccessResponseLTD
      case "ULTD" => matchSuccessResponseLTD
    }
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccessIndividual(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchSuccessResponseSOP))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailureIndividual(businessType: String, fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessMatchingService.matchBusinessWithIndividualName(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = TestBusinessVerificationController.submit(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  object TestBusinessVerificationController extends BusinessVerificationController {
    override val businessMatchingService = mockBusinessMatchingService
    val authConnector = mockAuthConnector
    override val controllerId = "test"
    override val backLinkCacheConnector = mockBackLinkCache
  }

}
