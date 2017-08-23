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

import connectors.{BackLinkCacheConnector, BusinessRegCacheConnector}
import models.{BusinessRegistration, Address, ReviewDetails}
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
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future


class AgentRegisterNonUKClientControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationCache = mock[BusinessRegCacheConnector]
  val mockBackLinkCache = mock[BackLinkCacheConnector]


  object TestAgentRegisterNonUKClientController extends AgentRegisterNonUKClientController {
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

  val serviceName: String = "ATED"

  "AgentRegisterNonUKClientController" must {

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

      "return business registration view for a Non-UK based client by agent with no back link" in {
        viewWithAuthorisedUser(serviceName, "NUK", None, Some("http://cachedBackLink")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your client's overseas registered business name and address?")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client's overseas registered business name and address?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://cachedBackLink")
        }
      }

      "return business registration view for a Non-UK based client by agent with a back link" in {
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some("http://backLink")))
        viewWithAuthorisedUser(serviceName, "NUK", Some("http://backLink"), Some("http://cachedBackLink")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your client's overseas registered business name and address?")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client's overseas registered business name and address?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://backLink")
        }
      }


      "return business registration view for a Non-UK based client by agent with some saved data" in {
        val regAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
        val businessReg = BusinessRegistration("ACME", regAddress)
        when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some("http://backLink")))
        viewWithAuthorisedUserWithSomeData(serviceName,Some(businessReg), "NUK", Some("http://backLink"), Some("http://cachedBackLink")) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))


          document.title() must be("What is your client's overseas registered business name and address?")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client's overseas registered business name and address?")
          document.getElementById("businessName").`val`() must be("ACME")
          document.getElementById("businessAddress.line_1").`val`() must be("line 1")
          document.getElementById("businessAddress.line_2").`val`() must be("line 2")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://backLink")
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
               |    "country": "$country"
               |  }
               |}
          """.stripMargin)

        type InputJson = JsValue
        type TestMessage = String
        type ErrorMessage = String

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", country = "")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson)) { result =>
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
          (createJson(country = "GB"), "show an error if country is selected as GB", "You cannot select United Kingdom when entering an overseas address")
         )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1)) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If registration details entered are valid, continue button must redirect to service specific redirect url" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          when(mockBackLinkCache.saveBackLink(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some("http://localhost:9933/ated-subscription/registered-business-address")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/register/non-uk-client/overseas-company/ATED/true?redirectUrl=")
          }
        }


        "throw exception, if redirect url is not defined" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "undefined", None) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("Service does not exist for : undefined. This should be in the conf file against services.{1}.serviceRedirectUrl")
          }
        }

      }
    }
  }

  def registerWithUnAuthorisedUser(businessType: String = "NUK", backLink: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))
    val result = TestAgentRegisterNonUKClientController.view(serviceName, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedAgent(service: String, businessType: String, backLink: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val result = TestAgentRegisterNonUKClientController.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def viewWithAuthorisedUser(service: String, businessType: String, backLink: Option[String] = None, cachedBackLink: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(cachedBackLink))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val result = TestAgentRegisterNonUKClientController.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def viewWithAuthorisedUserWithSomeData(service: String, businessRegistration: Option[BusinessRegistration],businessType: String, backLink: Option[String] = None, cachedBackLink: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(cachedBackLink))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](Matchers.any())
      (Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(successModel)))

    val result = TestAgentRegisterNonUKClientController.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }


  def submitWithUnAuthorisedUser(businessType: String = "NUK", redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val result = TestAgentRegisterNonUKClientController.submit(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], service: String = service, redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(Matchers.any())(Matchers.any())).thenReturn(Future.successful(None))

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)

    when(mockBusinessRegistrationCache.cacheDetails[BusinessRegistration](Matchers.any(), Matchers.any())(Matchers.any(),(Matchers.any())))
      .thenReturn(Future.successful(successModel))


    val result = TestAgentRegisterNonUKClientController.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[String] = Some("http://"))(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestAgentRegisterNonUKClientController.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
