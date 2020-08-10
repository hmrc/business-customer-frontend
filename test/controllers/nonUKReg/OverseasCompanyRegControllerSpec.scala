/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.ReviewDetailsController
import models.{Address, BusinessRegistration, OverseasCompany, ReviewDetails}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, Headers, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.BusinessRegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.binders.ContinueUrl

import scala.concurrent.Future


class OverseasCompanyRegControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val service = "ATED"
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService: BusinessRegistrationService = mock[BusinessRegistrationService]
  val mockBusinessRegistrationCache: BusinessRegCacheConnector = mock[BusinessRegCacheConnector]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val injectedViewInstance = app.injector.instanceOf[views.html.nonUkReg.overseas_company_registration]

  val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val mockReviewDetailsController: ReviewDetailsController = mock[ReviewDetailsController]

  object TestController extends OverseasCompanyRegController(
    mockAuthConnector,
    mockBackLinkCache,
    appConfig,
    injectedViewInstance,
    mockBusinessRegistrationService,
    mockBusinessRegistrationCache,
    mockReviewDetailsController,
    mcc
  ) {
    override val controllerId = "test"
  }

  val serviceName: String = "ATED"

  "OverseasCompanyRegController" must {

    "unauthorised users" must {
      "respond with a redirect for /view & be redirected to the unauthorised page" in {
        viewWithUnAuthorisedUser() { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/business-customer/unauthorised"))
        }
      }
    }

    "view" must {

      "return business registration view for a user for Non-UK" in {

        viewWithAuthorisedUser(serviceName) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number? - GOV.UK")
        }
      }

      "redirect url is invalid format" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        viewWithAuthorisedUser(serviceName, Some("http://website.com")) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "return business registration view for a user for Non-UK with saved data" in {
        val overseasDetails = OverseasCompany(Some(false), Some("1234"))
        viewWithAuthorisedAgentWithSomeData(serviceName, Some(overseasDetails)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Does your client have an overseas company registration number? - GOV.UK")
        }
      }

    }

    "send" must {

      val regAddress = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
      val businessReg = BusinessRegistration("ACME", regAddress)
      val overseasDetails = OverseasCompany(Some(true), Some("1234"))
      val reviewDetails = ReviewDetails("ACME", Some("Unincorporated body"), regAddress, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

      "validate form" must {

        def createJson(hasBusinessUniqueId: Boolean = true,
                       bUId: String = "some-id",
                       issuingInstitution: String = "some-institution",
                       issuingCountry: String = "FR") =
          Json.parse(
            s"""
               |{
               |  "hasBusinessUniqueId": $hasBusinessUniqueId,
               |  "businessUniqueId": "$bUId",
               |  "issuingInstitution": "$issuingInstitution",
               |  "issuingCountry": "$issuingCountry"
               |}
          """.stripMargin)

        type InputJson = JsValue
        type TestMessage = String
        type ErrorMessage = String

        "not be empty" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson(bUId = "", issuingInstitution = "", issuingCountry = "")

          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg), overseasDetails,reviewDetails) { result =>
            status(result) must be(BAD_REQUEST)
            contentAsString(result) must include("You must enter a country that issued the overseas company registration number")
            contentAsString(result) must include("You must enter an institution that issued the overseas company registration number")
            contentAsString(result) must include("You must enter an overseas company registration number")
          }
        }

        // inputJson , test message, error message
        val formValidationInputDataSet: Seq[(InputJson, TestMessage, ErrorMessage)] = Seq(
          (createJson(bUId = "a" * 61), "businessUniqueId must be maximum of 60 characters", "The overseas company registration number cannot be more than 60 characters"),
          (createJson(issuingInstitution = "a" * 41), "issuingInstitution must be maximum of 40 characters", "The institution that issued the overseas company registration number cannot be more than 40 characters"),
          (createJson(issuingCountry = "GB"), "show an error if issuing country is selected as GB", "You cannot select United Kingdom when entering an overseas address")
        )

        formValidationInputDataSet.foreach { data =>
          s"${data._2}" in {
            implicit val hc: HeaderCarrier = HeaderCarrier()
            registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1), "ATED", Some(businessReg),overseasDetails, reviewDetails) { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If we have no cache then an execption must be thrown" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", None,overseasDetails, reviewDetails) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("[OverseasCompanyRegController][send] - service :ATED. Error : No Cached BusinessRegistration")
          }
        }

        "If registration details entered are valid, continue button must redirect to the redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg),overseasDetails, reviewDetails) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/business-customer/review-details/ATED"))
          }
        }

        "If registration details entered are valid, continue button must redirect with to next page if no redirectUrl" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg),overseasDetails, reviewDetails, Some("/api/anywhere")) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/api/anywhere"))
          }
        }

        "redirect url is invalid format" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(businessReg),overseasDetails, reviewDetails, Some("http://website.com")) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }
      }
    }
  }

  def viewWithUnAuthorisedUser()(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestController.view(serviceName, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def viewWithAuthorisedAgent(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestController.view(service, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def viewWithAuthorisedAgentWithSomeData(service: String, overseasDetails : Option[OverseasCompany])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[OverseasCompany](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(OverseasCompany(Some((true))))))

    val result = TestController.view(service, true).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }


  def viewWithAuthorisedUser(service: String, redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[String](ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = TestController.view(service, true, redirectUrl).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }

  def registerWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson],
                                    service: String = service,
                                    busRegCache : Option[BusinessRegistration] = None,
                                    overseasSave : OverseasCompany,
                                    reviewDetails : ReviewDetails,
                                    redirectUrl: Option[String] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    when(mockBusinessRegistrationCache.fetchAndGetCachedDetails[BusinessRegistration](ArgumentMatchers.any())(ArgumentMatchers.any(),(ArgumentMatchers.any())))
      .thenReturn(Future.successful(busRegCache))

    when(mockBusinessRegistrationCache.cacheDetails[OverseasCompany](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),(ArgumentMatchers.any())))
      .thenReturn(Future.successful(overseasSave))

    when(mockBusinessRegistrationService.registerBusiness(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(reviewDetails))

    val result = TestController.register(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId)
      .withHeaders(Headers("Authorization" -> "value")))

    test(result)
  }
}
