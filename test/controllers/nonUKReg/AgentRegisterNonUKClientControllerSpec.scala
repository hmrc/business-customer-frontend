/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{Address, BusinessRegistration}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, Headers, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.Future


class AgentRegisterNonUKClientControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val service = "ATED"
  val invalidService = "scooby-doo"

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("fake")))
  private val mockAuthConnector: DefaultAuthConnector = mock[DefaultAuthConnector]
  private val mockBusinessRegistrationCache: BusinessRegCacheConnector = mock[BusinessRegCacheConnector]
  private val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  private val mockOverseasController: OverseasCompanyRegController = mock[OverseasCompanyRegController]
  private val mockMCC: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  private val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  class Setup {
    val controller = new AgentRegisterNonUKClientController(
      mockAuthConnector,
      mockBackLinkCache,
      mockAppConfig,
      mockBusinessRegistrationCache,
      mockOverseasController,
      mockMCC
    )
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessRegistrationCache)
    reset(mockBackLinkCache)
    reset(mockOverseasController)
  }

  "AgentRegisterNonUKClientController" must {
    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in new Setup {
        registerWithUnAuthorisedUser("NUK", controller = controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }

      "respond with a redirect for /send & be redirected to the unauthorised page" in new Setup {
        submitWithUnAuthorisedUser("NUK", controller = controller) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "Authorised Users" must {

      "return business registration view for a Non-UK based client by agent with no back link" in new Setup {
        viewWithAuthorisedUser(service, "NUK", None, Some("http://cachedBackLink"), controller) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your client’s overseas registered business name and address? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client’s overseas registered business name and address?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://cachedBackLink")
        }
      }

      "return business registration view for a Non-UK based client by agent with a back link" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("http://backLink")))
        viewWithAuthorisedUser(service, "NUK", Some("http://backLink"), Some("http://cachedBackLink"), controller) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("What is your client’s overseas registered business name and address? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client’s overseas registered business name and address?")
          document.getElementById("businessName_field").text() must be("Business name")
          document.getElementById("businessAddress.line_1_field").text() must be("Address line 1")
          document.getElementById("businessAddress.line_2_field").text() must be("Address line 2")
          document.getElementById("businessAddress.line_3_field").text() must be("Address line 3 (optional)")
          document.getElementById("businessAddress.line_4_field").text() must be("Address line 4 (optional)")
          document.getElementById("businessAddress.country_field").text() must include("Country")
          document.getElementById("submit").text() must be("Continue")

          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("http://backLink")
        }
      }


      "return business registration view for a Non-UK based client by agent with some saved data" in new Setup {
        val regAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
        val businessReg = BusinessRegistration("ACME", regAddress)
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("http://backLink")))
        viewWithAuthorisedUserWithSomeData(service, Some(businessReg), "NUK", Some("http://backLink"), Some("http://cachedBackLink"), controller) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))


          document.title() must be("What is your client’s overseas registered business name and address? - GOV.UK")
          document.getElementById("business-verification-text").text() must be("This section is: Add a client")
          document.getElementById("non-uk-reg-header").text() must be("What is your client’s overseas registered business name and address?")
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

        "not be empty" in new Setup {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(businessName = "", line1 = "", line2 = "", country = "")

          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), controller = controller) { result =>
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
          s"${data._2}" in new Setup {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1), controller = controller) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If registration details entered are valid, continue button must redirect to service specific redirect url" in new Setup {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some("http://localhost:9933/ated-subscription/registered-business-address"), controller) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/register/non-uk-client/overseas-company/ATED/true?redirectUrl=")
          }
        }

        "respond with NotFound when invalid service is in uri" in new Setup {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          submitWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), invalidService, None, controller = controller) { result =>
            status(result) must be(NOT_FOUND)
          }
        }
      }
    }
  }

  def registerWithUnAuthorisedUser(businessType: String = "NUK", backLink: Option[String] = None, controller: AgentRegisterNonUKClientController)
                                  (test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
    val result = controller.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def registerWithAuthorisedAgent(service: String, businessType: String, backLink: Option[String] = None, controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def viewWithAuthorisedUser(service: String, businessType: String, backLink: Option[String] = None, cachedBackLink: Option[String] = None, controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(cachedBackLink))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def viewWithAuthorisedUserWithSomeData(service: String, businessRegistration: Option[BusinessRegistration], businessType: String, backLink: Option[String] = None, cachedBackLink: Option[String] = None, controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"
    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)
    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(cachedBackLink))
    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(successModel)))

    val result = controller.view(service, backLink).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }


  def submitWithUnAuthorisedUser(businessType: String = "NUK", redirectUrl: Option[String] = Some("http://"), controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.submit(service).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def submitWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], service: String = service, redirectUrl: Option[String] = Some("http://"), controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = BusinessRegistration("ACME", address)

    when(mockBusinessRegistrationCache.cacheDetails[BusinessRegistration](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), (ArgumentMatchers.any())))
      .thenReturn(Future.successful(successModel))


    val result = controller.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def submitWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[String] = Some("http://"), controller: AgentRegisterNonUKClientController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = controller.submit(service).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

}
