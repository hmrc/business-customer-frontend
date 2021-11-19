/*
 * Copyright 2021 HM Revenue & Customs
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

import builders.AuthBuilder
import config.ApplicationConfig
import connectors.BackLinkCacheConnector
import controllers.nonUKReg.{BusinessRegController, NRLQuestionController}
import org.jsoup.Jsoup
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.BusinessMatchingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.NotFoundException
import views.html._

import java.util.UUID
import scala.concurrent.Future

class BusinessVerificationControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with Injecting {

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockBusinessMatchingService: BusinessMatchingService = mock[BusinessMatchingService]
  val mockBackLinkCache: BackLinkCacheConnector = mock[BackLinkCacheConnector]
  val service = "ATED"
  val invalidService = "scooby-doo"

  val appConfig: ApplicationConfig = inject[ApplicationConfig]
  implicit val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang.defaultLang))

  val businessRegUKController: BusinessRegUKController = mock[BusinessRegUKController]
  val busRegController: BusinessRegController = mock[BusinessRegController]
  val nrlQuestionController: NRLQuestionController = mock[NRLQuestionController]
  val reviewDetailsController: ReviewDetailsController = mock[ReviewDetailsController]
  val homeController: HomeController = mock[HomeController]
  val injectedViewInstance: business_verification = inject[views.html.business_verification]
  val injectedViewInstanceSOP: business_lookup_SOP = inject[views.html.business_lookup_SOP]
  val injectedViewInstanceLTD: business_lookup_LTD = inject[views.html.business_lookup_LTD]
  val injectedViewInstanceUIB: business_lookup_UIB = inject[views.html.business_lookup_UIB]
  val injectedViewInstanceOBP: business_lookup_OBP = inject[views.html.business_lookup_OBP]
  val injectedViewInstanceLLP: business_lookup_LLP = inject[views.html.business_lookup_LLP]
  val injectedViewInstanceLP: business_lookup_LP = inject[views.html.business_lookup_LP]
  val injectedViewInstanceNRL: business_lookup_NRL = inject[views.html.business_lookup_NRL]
  val injectedViewInstanceDetailsNotFound: details_not_found = inject[views.html.details_not_found]

  class Setup {
    val controller: BusinessVerificationController = new BusinessVerificationController(
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
      mockBackLinkCache,
      mockBusinessMatchingService,
      businessRegUKController,
      busRegController,
      nrlQuestionController,
      reviewDetailsController,
      homeController,
      mcc
    ) {
      override val controllerId = "test"
    }
  }

  "BusinessVerificationController" must {

    "businessVerification" must {

      "authorised users" must {

        "respond with OK" in new Setup {
          businessVerificationWithAuthorisedUser(controller) ( result =>
            status(result) must be(OK)
          )
        }

        "respond with NotFound when invalid service is in uri" in new Setup {
          intercept[NotFoundException] {

            businessVerificationWithAuthorisedUser(controller)(result =>
              status(result) must be(NOT_FOUND), serviceName = invalidService)
          }
        }

        "return Business Verification view for a user" in new Setup {

          businessVerificationWithAuthorisedUser(controller) { result =>
            val document = Jsoup.parse(contentAsString(result))


            document.title() must be("What is your business type? - GOV.UK")
            document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
            document.getElementById("business-verification-header").text() must be("What is your business type?")
            document.select(".govuk-radios__item").text() must include("Limited company")
            document.select(".govuk-radios__item").text() must include("Limited liability partnership")
            document.select(".govuk-radios__item").text() must include("partnership")
            document.select(".govuk-radios__item").text() must include("I have an overseas company")
            document.select(".govuk-radios__item").text() must include("Unit trust or collective investment vehicle")
            document.select(".govuk-radios__item").text() must include("Limited partnership")
            document.select("button").text() must be("Continue")
            document.getElementById("backLinkHref").text() must be("Back")
            document.getElementById("backLinkHref").attr("href") must be("/ated-subscription/appoint-agent")
          }
        }

        "return Business Verification view for an agent" in new Setup {

          businessVerificationWithAuthorisedAgent(controller) { result =>
            val document = Jsoup.parse(contentAsString(result))

            document.title() must be("What is the business type for your agency? - GOV.UK")
            document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
            document.getElementById("business-verification-agent-header").text() must be("What is the business type for your agency?")
            document.select(".govuk-radios__item").text() must include("Limited company")
            document.select(".govuk-radios__item").text() must include("Sole trader self-employed")
            document.select(".govuk-radios__item").text() must include("Limited liability partnership")
            document.select(".govuk-radios__item").text() must include("partnership")
            document.select(".govuk-radios__item").text() must include("I have an overseas company without a UK Unique Taxpayer Reference")
            document.select(".govuk-radios__item").text() must include("Limited partnership")
            document.select(".govuk-radios__item").text() must include("Unlimited company")
            document.select("button").text() must be("Continue")
          }
        }
      }

      "unauthorised users" must {
        "respond with a redirect & be redirected to the unauthorised page" in new Setup {
          businessVerificationWithUnAuthorisedUser(controller) { result =>
            status(result) must be(SEE_OTHER)
            redirectLocation(result).get must include("/business-customer/unauthorised")
          }
        }

      }
    }

    "continue" must {

      "selecting continue with no business type selected must display error message" in new Setup {
        continueWithAuthorisedUserJson(controller, "", FakeRequest().withJsonBody(Json.parse( """{"businessType" : ""}"""))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("Please select a type of business")
        }
      }

      "if non-uk with capital-gains-tax service, continue to registration page" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUserJson(controller, "NUK", FakeRequest()
          .withJsonBody(Json.parse( """{"businessType" : "NUK"}""")), "capital-gains-tax") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/register/capital-gains-tax/NUK")
        }
      }

      "if non-uk, continue to registration page" in new Setup {
        willSaveBackLink("/business-customer/business-verification/ATED")
        continueWithAuthorisedUserJson(controller, "NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NUK"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/ated-subscription/previous")
        }
      }

      "if non-uk Agent, continue to ATED NRL page" in new Setup {
        willSaveBackLink("/business-customer/business-verification/ATED")
        continueWithAuthorisedAgentJson(controller, "NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NUK"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/nrl/ATED")
        }
      }

      "if new, continue to NEW registration page" in new Setup {
        willSaveBackLink(s"/business-customer/business-verification/$service")
        continueWithAuthorisedUserJson(controller, "NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NEW"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/register-gb/$service/NEW")
        }
      }

      "if group, continue to GROUP registration page" in new Setup {
        willSaveBackLink(s"/business-customer/business-verification/$service")
        continueWithAuthorisedUserJson(controller, "NUK", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "GROUP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include(s"/business-customer/register-gb/$service/GROUP")
        }
      }

      "for any other option, redirect to home page again" in new Setup {
        when(mockBackLinkCache.saveBackLink(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))
        continueWithAuthorisedUserJson(controller, "XYZ", FakeRequest().withJsonBody(Json.parse("""{"businessType" : "XYZ"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/business-customer/agent/$service"))
        }
      }
    }

    "when selecting Sole Trader option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedSaUserJson(controller, "SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "SOP",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when SOP is selected for an Org user" in new Setup {
        continueWithAuthorisedUserJson(controller, "SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType" : "SOP",
            |  "isSaAccount": "false",
            |  "isOrgAccount": "true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an organisation with your Government Gateway ID. You cannot select sole trader or self-employed as your business type. You need to have an individual Government Gateway ID and enrol for Self Assessment")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects SOP" in new Setup {
        continueWithAuthorisedSaOrgUserJson(controller, "SOP", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "SOP",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "SOP", "AWRS") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: AWRS registration")
          document.getElementById("business-type-header").text() must be("What are your Self Assessment details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "firstName").text() must be("First name")
          document.getElementsByAttributeValue("for", "lastName").text() must be("Last name")
          document.getElementsByAttributeValue("for", "saUTR").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must include("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").attr("href") must include("/business-customer/business-verification/AWRS")
        }
      }

      "display correct heading for AGENT selecting Sole Trader" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "SOP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "firstName").text() must be("First name")
          document.getElementsByAttributeValue("for", "lastName").text() must be("Last name")
          document.getElementsByAttributeValue("for", "saUTR").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }
    }

    "when selecting None Resident Landord option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "NRL", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "NRL"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when NRL is selected for an Sa user" in new Setup {
        continueWithAuthorisedSaUserJson(controller, "NRL", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "NRL",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select limited company or partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects LTD" in new Setup {
        continueWithAuthorisedSaOrgUserJson(controller, "LTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "NRL",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "display the correct fields for a client" in new Setup {
        businessLookupWithAuthorisedUser(controller, "NRL", "ATED") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your Self Assessment details?")
          document.getElementById("business-type-paragraph").text() must be("Enter your Self Assessment details and we will attempt to match them against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "saUTR").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/register/non-uk-client/paySA/ATED")

        }
      }

      "display correct heading for AGENT selecting None Resident Landlord" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "NRL") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "saUTR").text() must include("Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("saUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")

        }
      }
    }

    "when selecting Limited Company option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "LTD", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LTD"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when LTD is selected for an Sa user" in new Setup {
        continueWithAuthorisedSaUserJson(controller, "LTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "LTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select limited company or partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects LTD" in new Setup {
        continueWithAuthorisedSaOrgUserJson(controller, "LTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "LTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "LTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")
        }
      }

      "display correct heading for AGENT selecting Limited Company" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "LTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Unit Trust option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "UT", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UT"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when UT is selected for an Sa user" in new Setup {
        continueWithAuthorisedSaUserJson(controller, "UT", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "UT",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select limited company or partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects UT" in new Setup {
        continueWithAuthorisedSaOrgUserJson(controller, "UT", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "UT",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "UT") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")

        }
      }

    }

    "when selecting Unincorporated Body option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "UIB", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "UIB"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "UIB", "AWRS") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: AWRS registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/AWRS")
        }
      }

      "display correct heading for AGENT selecting Unincorporated Association option" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "UIB") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Ordinary business partnership" must {
      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "OBP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "OBP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "OBP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Partnership name")
          document.select("#businessName-hint").text() must be("This is the name that you registered with HMRC")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")
        }
      }

      "display correct heading for AGENT selecting Ordinary Business Partnership option" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "OBP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Partnership name")
          document.select("#businessName-hint").text() must be("This is the name that you registered with HMRC")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Limited Liability Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "LLP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LLP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "LLP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")
        }
      }

      "display correct heading for AGENT selecting Limited Liability Partnership option" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "LLP") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Limited Partnership option" must {
      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "LP", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "LP"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }


      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "LP") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Partnership name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")
        }
      }

      "display correct heading for AGENT selecting Limited Partnership option" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "LP") { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Partnership name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "psaUTR").text() must include("Partnership Self Assessment Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("psaUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }

    "when selecting Unlimited Company option" must {

      "redirect to next screen to allow additional form fields to be entered" in new Setup {
        continueWithAuthorisedUserJson(controller, "ULTD", FakeRequest().withJsonBody(Json.parse( """{"businessType" : "ULTD"}"""))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "fail with a bad request when ULTD is selected for an Sa user" in new Setup {
        continueWithAuthorisedSaUserJson(controller, "ULTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "ULTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount": "false"
            |}
          """.stripMargin))) { result =>
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include("You are logged in as an individual with your Government Gateway ID. You cannot select limited company or partnership as your business type. You need to have an organisation Government Gateway ID.")
        }
      }

      "redirect to next screen to allow additional form fields to be entered when user has both Sa and Org and selects ULTD" in new Setup {
        continueWithAuthorisedSaOrgUserJson(controller, "ULTD", FakeRequest().withJsonBody(Json.parse(
          """
            |{
            |  "businessType": "ULTD",
            |  "isSaAccount": "true",
            |  "isOrgAccount":"true"
            |}
          """.stripMargin))) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/business-verification/ATED/businessForm")
        }
      }

      "add additional form fields to the screen for entry" in new Setup {
        businessLookupWithAuthorisedUser(controller, "ULTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-text").text() must be("This section is: ATED registration")
          document.getElementById("business-type-header").text() must be("What are your business details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10. Your accountant or tax manager would normally have your UTR.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
          document.getElementById("backLinkHref").text() must be("Back")
          document.getElementById("backLinkHref").attr("href") must be("/business-customer/business-verification/ATED")

        }
      }

      "display correct heading for AGENT selecting Unlimited Company" in new Setup {
        businessLookupWithAuthorisedAgent(controller, "ULTD") { result =>
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("business-verification-agent-text").text() must be("This section is: ATED agency set up")
          document.getElementById("business-type-agent-header").text() must be("What are your agency details?")
          document.getElementById("business-type-paragraph").text() must be("We will attempt to match your details against information we currently hold.")
          document.getElementsByAttributeValue("for", "businessName").text() must include("Registered company name")
          document.select("#businessName-hint").text() must be("This is the registered name on your incorporation certificate.")
          document.getElementsByAttributeValue("for", "cotaxUTR").text() must include("Corporation Tax Unique Taxpayer Reference (UTR)")
          document.select(".govuk-details__summary-text").text() must include(Messages("bc.business-verification.utr.help.question"))
          document.select(".govuk-details__text").text() must include("It is issued by HMRC when you register your business or for Self Assessment. Your UTR number is made up of 10 or 13 digits. If it is 13 digits only enter the last 10.")
          document.select("div.govuk-hint:nth-child(1)").text() must be("It can usually be found in the header of any letter issued by HMRC next to headings such as ‘Tax Reference’, ‘UTR’ or ‘Official Use’.")
          document.getElementById("cotaxUTR").attr("type") must be("text")
          document.getElementById("submit").text() must include("Continue")
        }
      }
    }
  }

  "detailsNotFound" should {
    "display the details not found page" when {
      "called" in new Setup {
        AuthBuilder.mockAuthorisedUser("test", mockAuthConnector)

        status(controller.detailsNotFound("ated", "businessType").apply(FakeRequest().withSession(
          "sessionId" -> "test",
          "token" -> "RANDOMTOKEN",
          "userId" -> "userId")
          .withHeaders(Headers("Authorization" -> "value"))
        )) mustBe OK
      }
    }
  }

  private def willSaveBackLink(backLink : String) =
    when(mockBackLinkCache.saveBackLink(
      ArgumentMatchers.eq(null),
      ArgumentMatchers.eq(Some(backLink)))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))


  def businessVerificationWithAuthorisedUser(controller: BusinessVerificationController)
                                            (test: Future[Result] => Any,
                                             serviceName: String = service) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Some("/ated-subscription/appoint-agent")))

    val result = controller.businessVerification(serviceName).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def businessVerificationWithAuthorisedAgent(controller: BusinessVerificationController)
                                             (test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.businessVerification(service).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def businessLookupWithAuthorisedUser(controller: BusinessVerificationController,
                                       businessType: String,
                                       serviceName: String = service)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.businessForm(serviceName, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def businessVerificationWithUnAuthorisedUser(controller: BusinessVerificationController)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockUnAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.businessVerification(service).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def continueWithAuthorisedUserJson(controller: BusinessVerificationController,
                                     businessType: String,
                                     fakeRequest: FakeRequest[AnyContentAsJson],
                                     service: String = service)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.continue(service).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def continueWithAuthorisedAgentJson(controller: BusinessVerificationController,
                                     businessType: String,
                                     fakeRequest: FakeRequest[AnyContentAsJson],
                                     service: String = service)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.continue(service).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def continueWithAuthorisedSaUserJson(controller: BusinessVerificationController,
                                       businessType: String,
                                       fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedSaUser(userId, mockAuthConnector)

    val result = controller.continue(service).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def continueWithAuthorisedSaOrgUserJson(controller: BusinessVerificationController,
                                          businessType: String,
                                          fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedSaOrgUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.continue(service).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def continueWithAuthorisedUser(controller: BusinessVerificationController,
                                 businessType: String,
                                 fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedUser(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.continue(service).apply(fakeRequest.withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

  def businessLookupWithAuthorisedAgent(controller: BusinessVerificationController,
                                        businessType: String)(test: Future[Result] => Any) {
    val sessionId = s"session-${UUID.randomUUID}"
    val userId = s"user-${UUID.randomUUID}"

    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockBackLinkCache.fetchAndGetBackLink(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(None))

    val result = controller.businessForm(service, businessType).apply(FakeRequest().withSession(
      "sessionId" -> sessionId,
      "token" -> "RANDOMTOKEN",
      "userId" -> userId)
      .withHeaders(Headers("Authorization" -> "value"))
    )

    test(result)
  }

}
