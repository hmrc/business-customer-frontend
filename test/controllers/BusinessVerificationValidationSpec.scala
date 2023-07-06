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
import controllers.nonUKReg.{BusinessRegController, NRLQuestionController}
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Headers, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.{SaUtr, SaUtrGenerator}

import java.util.UUID
import views.html.{business_lookup_LLP, business_lookup_LP, business_lookup_LTD, business_lookup_NRL, business_lookup_OBP, business_lookup_SOP, business_lookup_UIB, business_verification, details_not_found}

import scala.concurrent.Future

class BusinessVerificationValidationSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with Injecting {

  val request: FakeRequest[_] = FakeRequest()
  val mockBusinessMatchingService: BusinessMatchingService = mock[BusinessMatchingService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val mockBusinessRegCacheConnector: BusinessRegCacheConnector = mock[BusinessRegCacheConnector]
  val service = "ATED"
  val matchUtr: SaUtr = new SaUtrGenerator().nextSaUtr
  val noMatchUtr: SaUtr = new SaUtrGenerator().nextSaUtr

  val mockBusinessRegUKController: BusinessRegUKController = mock[BusinessRegUKController]
  val mockBusinessRegController: BusinessRegController = mock[BusinessRegController]
  val mockNrlQuestionConnector: NRLQuestionController = mock[NRLQuestionController]
  val mockReviewDetailsController: ReviewDetailsController = mock[ReviewDetailsController]
  val mockHomeController: HomeController = mock[HomeController]
  val injectedViewInstance: business_verification = inject[views.html.business_verification]
  val injectedViewInstanceSOP: business_lookup_SOP = inject[views.html.business_lookup_SOP]
  val injectedViewInstanceLTD: business_lookup_LTD = inject[views.html.business_lookup_LTD]
  val injectedViewInstanceUIB: business_lookup_UIB = inject[views.html.business_lookup_UIB]
  val injectedViewInstanceOBP: business_lookup_OBP = inject[views.html.business_lookup_OBP]
  val injectedViewInstanceLLP: business_lookup_LLP = inject[views.html.business_lookup_LLP]
  val injectedViewInstanceLP: business_lookup_LP = inject[views.html.business_lookup_LP]
  val injectedViewInstanceNRL: business_lookup_NRL = inject[views.html.business_lookup_NRL]
  val injectedViewInstanceDetailsNotFound: details_not_found = inject[views.html.details_not_found]

  val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  class Setup {
    val controller: BusinessVerificationController = new BusinessVerificationController (
      appConfig,
      mockAuthConnector,
      injectedViewInstance,
      injectedViewInstanceSOP,
      injectedViewInstanceLTD,
      injectedViewInstanceUIB,
      injectedViewInstanceOBP,
      injectedViewInstanceLLP,
      injectedViewInstanceLP,
      injectedViewInstanceNRL,
      injectedViewInstanceDetailsNotFound,
      mockBusinessRegCacheConnector,
      mockBackLinkCache,
      mockBusinessMatchingService,
      mockBusinessRegUKController,
      mockBusinessRegController,
      mockNrlQuestionConnector,
      mockReviewDetailsController,
      mockHomeController,
      mcc
    ) {
      override val controllerId = "test"
    }
  }

  val matchSuccessResponseUIB: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLTD: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseNRL: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseSOP: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseOBP: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLLP: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": false,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchSuccessResponseLP: JsValue = Json.parse(
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
      |    "country": "GB"
      |  },
      |  "sapNumber": "sap123",
      |  "safeId": "safe123",
      |  "isAGroup": true,
      |  "directMatch" : false,
      |  "agentReferenceNumber": "agent123",
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin)

  val matchFailureResponse: JsValue = Json.parse( """{"reason":"Sorry. Business details not found. Try with correct UTR and/or name."}""")

  def submitWithUnAuthorisedUser(businessType: String, controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = controller.submit(service, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  "BusinessVerificationValidationController" must {

    type InputRequest = FakeRequest[AnyContentAsFormUrlEncoded]
    type MustTestMessage = String
    type InTestMessage = String
    type ErrorMessage = String
    type BusinessType = String

    def nrlUtrRequest(utr: String = matchUtr.utr, businessName: String = "ACME"): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/").withFormUrlEncodedBody("saUTR" -> s"$utr", "businessName" -> s"$businessName")
    def ctUtrRequest(ct: String = matchUtr.utr, businessName: String = "ACME"): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$ct", "businessName" -> s"$businessName")
    def psaUtrRequest(psa: String = matchUtr.utr, businessName: String = "ACME"): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/").withFormUrlEncodedBody("psaUTR" -> s"$psa", "businessName" -> s"$businessName")
    def saUtrRequest(sa: String = matchUtr.utr, firstName: String = "A", lastName: String = "B"): FakeRequest[AnyContentAsFormUrlEncoded] =
      FakeRequest("POST", "/").withFormUrlEncodedBody("saUTR" -> s"$sa", "firstName" -> s"$firstName", "lastName" -> s"$lastName")

    val formValidationInputDataSetOrg: Seq[(MustTestMessage, Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)])] =
      Seq(
        ("if the selection is Unincorporated body :",
          Seq(
            ("Business Name must not be empty", "UIB", ctUtrRequest(businessName = ""), "Enter a registered company name"),
            ("CO Tax UTR must not be empty", "UIB", ctUtrRequest(ct = ""), "Enter a Corporation Tax Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "UIB", ctUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "UIB", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "UIB", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "UIB", ctUtrRequest(ct = "1234567890"), "The Corporation Tax Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Company :",
          Seq(
            ("Business Name must not be empty", "LTD", ctUtrRequest(businessName = ""), "Enter a registered company name"),
            ("CO Tax UTR must not be empty", "LTD", ctUtrRequest(ct = ""), "Enter a Corporation Tax Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LTD", ctUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("CO Tax UTR must be 10 digits", "LTD", ctUtrRequest(ct = "1" * 11), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must contain only digits", "LTD", ctUtrRequest(ct = "12345678aa"), "Corporation Tax Unique Taxpayer Reference must be 10 digits"),
            ("CO Tax UTR must be valid", "LTD", ctUtrRequest(ct = "1234567890"), "The Corporation Tax Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Non Resident Landlord :",
          Seq(
            ("Business Name must not be empty", "NRL", nrlUtrRequest(businessName = ""), "Enter a registered company name"),
            ("SA UTR must not be empty", "NRL", nrlUtrRequest(utr = ""), "Enter a Self Assessment Unique Taxpayer Reference"),
            ("SA UTR must be 10 digits", "NRL", nrlUtrRequest(utr = "12345678901"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("SA UTR must contain only digits", "NRL", nrlUtrRequest(utr = "12345678aa"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("SA UTR must be valid", "NRL", nrlUtrRequest(utr = "1234567890"), "The Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Liability Partnership : ",
          Seq(
            ("Business Name must not be empty", "LLP", psaUtrRequest(businessName = ""), "Enter a registered company name"),
            ("Partnership Self Assessment UTR  must not be empty", "LLP", psaUtrRequest(psa = ""), "Enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LLP", psaUtrRequest(businessName = "a" * 106), "The registered company name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LLP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LLP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LLP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Limited Partnership : ",
          Seq(
            ("Business Name must not be empty", "LP", psaUtrRequest(businessName = ""), "Enter a registered partnership name"),
            ("Partnership Self Assessment UTR  must not be empty", "LP", psaUtrRequest(psa = ""), "Enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "LP", psaUtrRequest(businessName = "a" * 106), "The registered partnership name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "LP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "LP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "LP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          ),
        ("if the selection is Ordinary Business Partnership : ",
          Seq(
            ("Business Name must not be empty", "OBP", psaUtrRequest(businessName = ""), "Enter a registered partnership name"),
            ("Partnership Self Assessment UTR  must not be empty", "OBP", psaUtrRequest(psa = ""), "Enter a Partnership Self Assessment Unique Taxpayer Reference"),
            ("Registered Name must not be more than 105 characters", "OBP", psaUtrRequest(businessName = "a" * 106), "The registered partnership name cannot be more than 105 characters"),
            ("Partnership Self Assessment UTR  must be 10 digits", "OBP", psaUtrRequest(psa = "1" * 11), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must contain only digits", "OBP", psaUtrRequest(psa = "12345678aa"), "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"),
            ("Partnership Self Assessment UTR  must be valid", "OBP", psaUtrRequest(psa = "1234567890"), "The Partnership Self Assessment Unique Taxpayer Reference is not valid")
          )
          )
      )

    val formValidationInputDataSetInd: Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)] =
      Seq(
        ("First name must not be empty", "SOP", saUtrRequest(matchUtr.utr, "", "b"), "Enter a first name"),
        ("Last name must not be empty", "SOP", saUtrRequest(lastName = ""), "Enter a last name"),
        ("SA UTR must not be empty", "SOP", saUtrRequest(sa = ""), "Enter a Self Assessment Unique Taxpayer Reference"),
        ("First Name must not be more than 40 characters", "SOP", saUtrRequest(firstName = "a" * 41), "A first name cannot be more than 40 characters"),
        ("Last Name must not be more than 40 characters", "SOP", saUtrRequest(lastName = "a" * 41), "A last name cannot be more than 40 characters"),
        ("SA UTR must be 10 digits", "SOP", saUtrRequest(sa = "12345678901"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
        ("SA UTR must contain only digits", "SOP", saUtrRequest(sa = "12345678aa"), "Self Assessment Unique Taxpayer Reference must be 10 digits"),
        ("SA UTR must be valid", "SOP", saUtrRequest(sa = "1234567890"), "The Self Assessment Unique Taxpayer Reference is not valid")
      )

    "handle the user correctly" must {
      "if the selection is an Organisation" must {
        formValidationInputDataSetOrg foreach { dataSet =>
          s"${dataSet._1}" must {
            dataSet._2 foreach { inputData =>
              s"${inputData._1}" in new Setup {
                submitWithAuthorisedUserSuccessOrg(inputData._2, inputData._3, controller) { result =>
                  status(result) must be(BAD_REQUEST)
                  contentAsString(result) must include(s"${inputData._4}")
                }
              }
            }
          }
        }
      }


      "if the selection is Sole Trader" must {
        formValidationInputDataSetInd foreach { dataSet =>
          s"${dataSet._1}" in new Setup {
            submitWithAuthorisedUserSuccessIndividual(dataSet._2, dataSet._3, controller) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(s"${dataSet._4}")
            }
          }
        }
      }
    }

    "if the Ordinary Business Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("OBP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailure("OBP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/OBP")
        }
      }
    }

    "if the Limited Liability Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LLP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailure("LLP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LLP")
        }
      }
    }

    "if the Limited Partnership form is successfully validated:" must {
      "for successful match, status should be 303 and user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailure("LP", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "psaUTR" -> s"$noMatchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LP")
        }
      }
    }

    "if the Sole Trader form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessIndividual("SOP", FakeRequest("POST", "/").withFormUrlEncodedBody("firstName" -> "First Name", "lastName" -> "Last Name", "saUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailureIndividual("SOP",
          FakeRequest("POST", "/").withFormUrlEncodedBody(Map("firstName" -> "First Name", "lastName" -> "Last Name", "saUTR" -> s"$noMatchUtr").toSeq: _*),
          controller
        ) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/SOP")
        }
      }
    }

    "if the Non Resident Landlord is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("NRL", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "saUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }

      "for successful match with missing countryCode, status should be 303 and  user should be redirected to update overseas details reg" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrgNRLNoCountry(FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "saUTR" -> s"$matchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/register/$service/NRL")
            verify(mockBusinessRegCacheConnector, times(1)).cacheDetails(ArgumentMatchers.eq("Update_No_Register"), ArgumentMatchers.eq(true))(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "for unsuccessful match, status should be Redirect and  user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailure("NRL", FakeRequest("POST", "/").withFormUrlEncodedBody("businessName" -> "Business Name", "saUTR" -> s"$noMatchUtr"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/NRL")
        }
      }
    }

    "if the Unincorporated body form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("UIB", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and  user should be on same details not found page" in new Setup {
        submitWithAuthorisedUserFailure("UIB", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/UIB")
        }
      }
    }

    "if the Limited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserSuccessOrg("LTD", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        submitWithAuthorisedUserFailure("LTD", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/LTD")
        }
      }
    }

    "if the Unit Trust form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        submitWithAuthorisedUserSuccessOrg("UT", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserFailure("UT", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/UT")
        }
      }
    }

    "if the Unlimited Company form  is successfully validated:" must {
      "for successful match, status should be 303 and  user should be redirected to review details page" in new Setup {
        submitWithAuthorisedUserSuccessOrg("ULTD", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$matchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
        }
      }
      "for unsuccessful match, status should be Redirect and user should be on details not found page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        submitWithAuthorisedUserFailure("ULTD", FakeRequest("POST", "/").withFormUrlEncodedBody("cotaxUTR" -> s"$noMatchUtr", "businessName" -> "Business Name"), controller) {
          result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-verification/$service/detailsNotFound/ULTD")
        }
      }
    }
  }


  def submitWithAuthorisedUserSuccessOrg(businessType: String,
                                         fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded],
                                         controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

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
    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponse))

    val fullReq = fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value")
    )

    val result = controller.submit(service, businessType).apply(fullReq)

    test(result)
  }

  def submitWithAuthorisedUserSuccessOrgNRLNoCountry(fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded],
                                                  controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val matchSuccessResponse = Json.parse(
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
        |    "country": ""
        |  },
        |  "sapNumber": "sap123",
        |  "safeId": "safe123",
        |  "isAGroup": false,
        |  "directMatch" : false,
        |  "agentReferenceNumber": "agent123",
        |  "isBusinessDetailsEditable": false
        |}
    """.stripMargin)

    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(matchSuccessResponse))

    val fullReq = fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value")
      )

    val result = controller.submit(service, "NRL").apply(fullReq)

    test(result)
  }

  def submitWithAuthorisedUserSuccessIndividual(businessType: String,
                                                fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded],
                                                controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessMatchingService.matchBusinessWithIndividualName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(matchSuccessResponseSOP))

    val result = controller.submit(service, businessType).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def submitWithAuthorisedUserFailure(businessType: String,
                                      fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded],
                                      controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessMatchingService.matchBusinessWithOrganisationName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = controller.submit(service, businessType).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def submitWithAuthorisedUserFailureIndividual(businessType: String,
                                                fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded],
                                                controller: BusinessVerificationController)(test: Future[Result] => Any): Unit = {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    when(mockBusinessMatchingService.matchBusinessWithIndividualName(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(matchFailureResponse))

    val result = controller.submit(service, businessType).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

}
