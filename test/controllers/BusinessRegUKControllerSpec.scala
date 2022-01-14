/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Address, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Headers, MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.BusinessRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector

import java.util.UUID
import scala.concurrent.Future


class BusinessRegUKControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with Injecting {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]
  val mockBackLinkCache = mock[BackLinkCacheConnector]
  val mockReviewDetailController = mock[ReviewDetailsController]
  val injectedViewInstance = inject[views.html.business_group_registration]

  val appConfig = inject[ApplicationConfig]
  implicit val mcc = inject[MessagesControllerComponents]

  object TestBusinessRegController extends BusinessRegUKController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    injectedViewInstance,
    mockBusinessRegistrationService,
    mockReviewDetailController,
    mcc
  ) {
    override val authConnector = mockAuthConnector
    override val controllerId = "test"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  val serviceName: String = "ATED"

  "BusinessGBController" must {

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }


      "respond with a redirect for /send & be redirected to the unauthorised page" in {
        submitWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-customer/unauthorised")
        }
      }

    }

    "Authorised Users" must {

      "return business registration view for a user for Group" in {

        registerWithAuthorisedUser("awrs", "GROUP") {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Create AWRS group - GOV.UK")
            document.getElementById("business-verification-text").text() must be("This section is: AWRS registration")
            document.getElementById("business-registration.header").text() must be("Create AWRS group")

            document.getElementsByAttributeValue("for", "businessName").text() must be("Group representative name")
            document.getElementById("businessName-hint").text() must be("This is your registered company name")
            document.getElementsByAttributeValue("for", "businessAddress.line_1").text() must be("Address line 1")
            document.getElementsByAttributeValue("for", "businessAddress.line_2").text() must be("Address line 2")
            document.getElementsByAttributeValue("for", "businessAddress.line_3").text() must be("Address line 3 (Optional)")
            document.getElementsByAttributeValue("for", "businessAddress.line_4").text() must be("Address line 4 (Optional)")
            document.getElementById("submit").text() must be("Continue")
            document.getElementsByAttributeValue("for", "businessAddress.postcode").text() must be("Postcode")
            document.getElementById("businessAddress.country").attr("value") must be("GB")
        }
      }

      "return business registration view for a user for New Business" in {

        registerWithAuthorisedUser("awrs", "NEW") {
          result =>
            status(result) must be(OK)
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("Create AWRS group - GOV.UK")
            document.getElementById("business-verification-text").text() must be("This section is: AWRS registration")
            document.getElementById("business-registration.header").text() must be("New business details")
            document.getElementsByAttributeValue("for", "businessName").text() must be("Group representative name")
            document.getElementById("businessName-hint").text() must be("This is your registered company name")
            document.getElementsByAttributeValue("for", "businessAddress.line_1").text() must be("Address line 1")
            document.getElementsByAttributeValue("for", "businessAddress.line_2").text() must be("Address line 2")
            document.getElementsByAttributeValue("for", "businessAddress.line_3").text() must be("Address line 3 (Optional)")
            document.getElementsByAttributeValue("for", "businessAddress.line_4").text() must be("Address line 4 (Optional)")
            document.getElementById("submit").text() must be("Continue")
        }
      }
    }

    "send" must {

      "validate form" must {

        def createJson(businessName: String = "ACME",
                       line1: String = "line-1",
                       line2: String = "line-2",
                       line3: String = "",
                       line4: String = "",
                       postcode: String = "AA1 1AA",
                       country: String = "GB") =
          Json.parse(
            s"""
               |{
               |  "businessName": "$businessName",
               |  "businessAddress": {
               |    "line_1": "$line1",
               |    "line_2": "$line2",
               |    "line_3": "$line3",
               |    "line_4": "$line4",
               |    "postcode": "$postcode",
               |    "country": "$country"
               |  }
               |}
          """.stripMargin)

        type InputJson = JsValue
        type TestMessage = String
        type ErrorMessage = String

        "not be empty for a Group" in {
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", postcode = "")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Enter a business name")
              contentAsString(result) must include("Enter address line 1")
              contentAsString(result) must include("Enter address line 2")
              contentAsString(result) must include("Enter a valid postcode")
          }
        }

        "not contains special character(,) for a Group" in {
          val inputJson = createJson(businessName = "some name", line1 = "line 1", line2 = "line 2", postcode = "AA, AA1")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) {
            result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include("Enter a valid postcode")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(businessName = "a" * 106), "If entered, Business name must be maximum of 105 characters", "The business name cannot be more than 105 characters"),
          (createJson(line1 = "a" * 36), "If entered, Address line 1 must be maximum of 35 characters", "Address line 1 cannot be more than 35 characters"),
          (createJson(line2 = "a" * 36), "If entered, Address line 2 must be maximum of 35 characters", "Address line 2 cannot be more than 35 characters"),
          (createJson(line3 = "a" * 36), "Address line 3 is optional but if entered, must be maximum of 35 characters", "Address line 3 cannot be more than 35 characters"),
          (createJson(line4 = "a" * 36), "Address line 4 is optional but if entered, must be maximum of 35 characters", "Address line 4 cannot be more than 35 characters"),
          (createJson(postcode = "a" * 11), "If entered, Postcode must be maximum of 10 characters", "The postcode cannot be more than 10 characters"),
          (createJson(postcode = "1234567890"), "If entered, Postcode must be a valid postcode", "Enter a valid postcode")
        )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1)) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If registration details entered are valid, continue button must redirect to review details page" in {
          val inputJson = createJson()
          when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/review-details/$service")
          }
        }
      }
    }
  }

  def registerWithUnAuthorisedUser(businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestBusinessRegController.register(serviceName, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def registerWithAuthorisedUser(service: String, businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestBusinessRegController.register(service, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }


  def submitWithUnAuthorisedUser(businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestBusinessRegController.send(service, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], businessType: String = "GROUP")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.registerBusiness(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(successModel))

    val result = TestBusinessRegController.send(service, businessType).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }


}
