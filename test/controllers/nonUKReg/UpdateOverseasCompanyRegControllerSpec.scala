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

import models.{Address, BusinessRegistration, OverseasCompany, ReviewDetails}
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
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }


class UpdateOverseasCompanyRegControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val request = FakeRequest()
  val service = "ATED"
  val mockAuthConnector = mock[AuthConnector]
  val mockBusinessRegistrationService = mock[BusinessRegistrationService]

  object TestNonUKController extends UpdateOverseasCompanyRegController {
    override val authConnector = mockAuthConnector
    override val businessRegistrationService = mockBusinessRegistrationService
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockBusinessRegistrationService)
  }

  val serviceName: String = "ATED"

  "UpdateOverseasCompanyRegController" must {

    "respond to /register" in {
      val result = route(FakeRequest(GET, s"/business-customer/register/$serviceName/NUK")).get
      status(result) must not be NOT_FOUND
    }

    "unauthorised users" must {
      "respond with a redirect for /register & be redirected to the unauthorised page" in {
        editWithUnAuthorisedUser() { result =>
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

    "edit client" must {

      "return business registration view for a Non-UK based client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
        )
        val overseasCompany = OverseasCompany(
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))

        editClientWithAuthorisedUser(serviceName) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number? - GOV.UK")
        }
      }

      "return business registration view for a Non-UK based agent creating a client with found data" in {
        val busRegData = BusinessRegistration(businessName = "testName",
          businessAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("postCode"), "country")
        )
        val overseasCompany = OverseasCompany(
          businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
          hasBusinessUniqueId = Some(true),
          issuingInstitution = Some("issuingInstitution"),
          issuingCountry = None
        )
        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))

        editClientWithAuthorisedAgent(serviceName, Some(ContinueUrl("/api/anywhere"))) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))

          document.title() must be("Do you have an overseas company registration number? - GOV.UK")

        }
      }

      "redirect url is invalid format" in {
        editClientWithAuthorisedAgent(serviceName, Some(ContinueUrl("http://website.com"))) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "throw an exception if we have no data" in {

        when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        editClientWithAuthorisedUser(serviceName) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must be("No Registration Details found")
        }
      }
    }

    "update" must {

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

          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED") { result =>
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
            registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(data._1), "ATED") { result =>
              status(result) must be(BAD_REQUEST)
              contentAsString(result) must include(data._3)
            }
          }
        }

        "If we have no cache then an exception must be thrown" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", None, false) { result =>
            val thrown = the[RuntimeException] thrownBy await(result)
            thrown.getMessage must be("No Registration Details found")
          }
        }

        "If registration details entered are valid, continue button must redirect to the next page" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED") { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/business-customer/review-details/ATED"))
          }
        }

        "If registration details entered are valid, continue button must redirect to redirectUrl when present" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(ContinueUrl("/api/anywhere"))) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some("/api/anywhere"))
          }
        }

        "redirect url is invalid format" in {
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val inputJson = createJson()
          registerWithAuthorisedUserSuccess(FakeRequest().withJsonBody(inputJson), "ATED", Some(ContinueUrl("http://website.com"))) { result =>
            status(result) must be(BAD_REQUEST)
          }
        }
      }
    }
  }

  def editWithUnAuthorisedUser()(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    val result = TestNonUKController.viewForUpdate(serviceName, false, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedAgent(service: String, redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)

    val result = TestNonUKController.viewForUpdate(serviceName, false, redirectUrl).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def editClientWithAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"


    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))


    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.viewForUpdate(serviceName, false, None).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }



  def submitWithUnAuthorisedUser(businessType: String = "NUK", redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, true, redirectUrl).apply(FakeRequest().withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def registerWithAuthorisedUserSuccess(fakeRequest: FakeRequest[AnyContentAsJson], service: String = service, redirectUrl: Option[ContinueUrl] = None, hasCache: Boolean = true)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val address = Address("line 1", "line 2", Some("line 3"), Some("line 4"), Some("AA1 1AA"), "UK")
    val busRegData = BusinessRegistration(businessName = "testName", businessAddress = address)
    val overseasCompany = OverseasCompany(
      businessUniqueId = Some(s"BUID-${UUID.randomUUID}"),
      hasBusinessUniqueId = Some(true),
      issuingInstitution = Some("issuingInstitution"),
      issuingCountry = None
    )
    if (hasCache)
      when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(("NUK", busRegData, overseasCompany))))
    else
      when(mockBusinessRegistrationService.getDetails()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

    val successModel = ReviewDetails("ACME", Some("Unincorporated body"), address, "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))

    when(mockBusinessRegistrationService.updateRegisterBusiness(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())
    (Matchers.any(), Matchers.any())).thenReturn(Future.successful(successModel))

    val result = TestNonUKController.update(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

  def submitWithAuthorisedUserFailure(fakeRequest: FakeRequest[AnyContentAsJson], redirectUrl: Option[ContinueUrl] = None)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    builders.AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)

    val result = TestNonUKController.update(service, true, redirectUrl).apply(fakeRequest.withSession(
      SessionKeys.sessionId -> sessionId,
      "token" -> "RANDOMTOKEN",
      SessionKeys.userId -> userId))

    test(result)
  }

}
