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

package controllers.nonUKReg

import java.util.UUID

import config.ApplicationConfig
import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import models.{Address, BusinessRegistration, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }


class BusinessRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar  with BeforeAndAfterEach {

  val request = FakeRequest()
  val serviceName = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationCache = mock[BusinessRegCacheConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]

  object TestBusinessRegController extends BusinessRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationCache = mockBusinessRegistrationCache
    override val controllerId = "test"
    override val backLinkCacheConnector = mockBackLinkCache
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessRegistrationCache)
    reset(mockBackLinkCache)
  }

  "BusinessRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName/NUK")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        registerWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }

      "respond with a redirect for /send & be redirected to the unauthorised page" in {
        submitWithUnAuthorisedUser("NUK") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "Authorised Users" must {

      "return business registration view for a user for Non-UK" in {

        registerWithAuthorisedUser(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your overseas business registered name and address? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-registration-header").text() must be("What is your overseas business registered name and address?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Continue")
        }
      }


      "return business registration view for a user for Non-UK with saved data" in {
        val regAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
        val businessReg = BusinessRegistration("ACME", regAddress)
        registerWithAuthorisedUserWithSomeData(serviceName, "NUK", Some(businessReg)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your overseas business registered name and address? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-registration-header").text() must be("What is your overseas business registered name and address?")
          document.getElementById("businessName").`val`() must be("ACME")
          document.getElementById("businessAddress.line_1").`val`() must be("line 1")
          document.getElementById("businessAddress.line_2").`val`() must be("line 2")
          document.getElementById("submit").text() must be("Continue")
        }
      }


      "return business registration view for an agent" in {

        registerWithAuthorisedAgent(serviceName, "NUK") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is the registered business name and address of your overseas agency? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-registration-header").text() must be("What is the registered business name and address of your overseas agency?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Register your agency")
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
                       postcode: String = "12345678",
                       country: String = "FR") =
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

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", postcode = "", country = "")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

          submitWithAuthorisedUserSuccess(serviceName, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("You must enter a business name")
            contentAsString(result) must include("You must enter an address into Address line 1")
            contentAsString(result) must include("You must enter an address into Address line 2")
            contentAsString(result) mustNot include("Postcode must be entered")
            contentAsString(result) must include("You must enter a country")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(businessName = "a" * 106), "If entered, Business name must be maximum of 105 characters", "The business name cannot be more than 105 characters"),
          (createJson(line1 = "a" * 36), "If entered, Address line 1 must be maximum of 35 characters", "Address line 1 cannot be more than 35 characters"),
          (createJson(line2 = "a" * 36), "If entered, Address line 2 must be maximum of 35 characters", "Address line 2 cannot be more than 35 characters"),
          (createJson(line3 = "a" * 36), "Address line 3 is optional but if entered, must be maximum of 35 characters", "Address line 3 cannot be more than 35 characters"),
          (createJson(line4 = "a" * 36), "Address line 4 is optional but if entered, must be maximum of 35 characters", "Address line 4 cannot be more than 35 characters"),
          (createJson(postcode = "a" * 11), "Postcode is optional but if entered, must be maximum of 10 characters", "There is a problem with the postal code"),
          (createJson(country = "GB"), "show an error if country is selected as GB", "You cannot select United Kingdom when entering an overseas address")
        )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
            submitWithAuthorisedUserSuccess(serviceName, FakeRequest().withJsonBody(data._1)) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If registration details entered are valid, continue button must redirect to review details page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          val inputJson = createJson()
          submitWithAuthorisedUserSuccess(serviceName, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/register/non-uk-client/overseas-company/$serviceName/false")
          }
        }

        "If registration details entered are valid and business-identifier question is selected as No, continue button must redirect to review details page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess(serviceName, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include(s"/business-customer/register/non-uk-client/overseas-company/$serviceName/false")
          }
        }


        "fail if we are a client for ATED and have no PostCode" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(postcode = "")
          when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess(serviceName, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }

        "pass if we are a client for AWRS and have no PostCode" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(postcode = "")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess("AWRS", FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "pass if we are an agent for ATED and have no PostCode" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(postcode = "")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedAgent(serviceName, FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
          }
        }

        "pass if we are an agent for AWRS and have no PostCode" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(postcode = "")
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedAgent("AWRS", FakeRequest().withJsonBody(inputJson)) { result =>
            status(result) must be(SEE_OTHER)
          }
        }
      }
    }
  }

  def registerWithUnAuthorisedUser(businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestBusinessRegController.register(serviceName, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedAgent(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val result = TestBusinessRegController.register(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUser(service: String, businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val result = TestBusinessRegController.register(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUserWithSomeData(service: String, businessType: String, businessRegistration: Option[BusinessRegistration])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successModel)))

    val result = TestBusinessRegController.register(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


  def submitWithUnAuthorisedUser(service: String, businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.send(service, businessType).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(service: String, fakeRequest: FakeRequest[AnyContentAsJson], businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)

    when(mockBusinessRegistrationCache.cacheDetails[BusinessRegistration](Matchers.any(), Matchers.any())(Matchers.any(),(Matchers.any())))
      .thenReturn(Future.successful(successModel))

    val result = TestBusinessRegController.send(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedAgent(service: String, fakeRequest: FakeRequest[AnyContentAsJson], businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)
    when(mockBusinessRegistrationCache.cacheDetails[BusinessRegistration](Matchers.any(), Matchers.any())(Matchers.any(),(Matchers.any())))
      .thenReturn(Future.successful(successModel))

    val result = TestBusinessRegController.send(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(service: String, fakeRequest: FakeRequest[AnyContentAsJson], businessType: String = "NUK")(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestBusinessRegController.send(service, businessType).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
