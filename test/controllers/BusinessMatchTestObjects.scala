/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.{SaUtr, SaUtrGenerator}

trait BusinessMatchTestObjects {

  val matchUtr: SaUtr   = new SaUtrGenerator().nextSaUtr
  val noMatchUtr: SaUtr = new SaUtrGenerator().nextSaUtr

  val formValidationNameInputDataSetOrg: Seq[
    (
      MustTestMessage,
      Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)]
    )
  ] =
    Seq(
      (
        "if the selection is Unincorporated body :",
        Seq(
          (
            "Business Name must not be empty",
            "UIB",
            ctUtrRequest(businessName = ""),
            "Enter a registered company name"
          ),
          (
            "Registered Name must not be more than 105 characters",
            "UIB",
            ctUtrRequest(businessName = "a" * 106),
            "The registered company name cannot be more than 105 characters"
          )
        )
      ),
      (
        "if the selection is Limited Company :",
        Seq(
          (
            "Business Name must not be empty",
            "LTD",
            ctUtrRequest(businessName = ""),
            "Enter a registered company name"
          ),
          (
            "Registered Name must not be more than 105 characters",
            "LTD",
            ctUtrRequest(businessName = "a" * 106),
            "The registered company name cannot be more than 105 characters"
          )
        )
      ),
      (
        "if the selection is Non Resident Landlord :",
        Seq(
          (
            "Business Name must not be empty",
            "NRL",
            nrlUtrRequest(businessName = ""),
            "Enter a registered company name"
          )
        )
      ),
      (
        "if the selection is Limited Liability Partnership : ",
        Seq(
          (
            "Business Name must not be empty",
            "LLP",
            psaUtrRequest(businessName = ""),
            "Enter a registered company name"
          ),
          (
            "Registered Name must not be more than 105 characters",
            "LLP",
            psaUtrRequest(businessName = "a" * 106),
            "The registered company name cannot be more than 105 characters"
          )
        )
      ),
      (
        "if the selection is Limited Partnership : ",
        Seq(
          (
            "Business Name must not be empty",
            "LP",
            psaUtrRequest(businessName = ""),
            "Enter a registered partnership name"
          ),
          (
            "Registered Name must not be more than 105 characters",
            "LP",
            psaUtrRequest(businessName = "a" * 106),
            "The registered partnership name cannot be more than 105 characters"
          )
        )
      ),
      (
        "if the selection is Ordinary Business Partnership : ",
        Seq(
          (
            "Business Name must not be empty",
            "OBP",
            psaUtrRequest(businessName = ""),
            "Enter a registered partnership name"
          ),
          (
            "Registered Name must not be more than 105 characters",
            "OBP",
            psaUtrRequest(businessName = "a" * 106),
            "The registered partnership name cannot be more than 105 characters"
          )
        )
      )
    )
  type InputRequest    = FakeRequest[AnyContentAsFormUrlEncoded]
  type MustTestMessage = String
  type InTestMessage   = String
  type ErrorMessage    = String
  type BusinessType    = String

  def nrlUtrRequest(
    utr: String = matchUtr.utr,
    businessName: String = "ACME"
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest("POST", "/").withFormUrlEncodedBody(
      "utr"        -> s"$utr",
    )
  def ctUtrRequest(
    ct: String = matchUtr.utr,
    businessName: String = "ACME"
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest("POST", "/").withFormUrlEncodedBody(
      "utr"     -> s"$ct",
    )
  def psaUtrRequest(
    psa: String = matchUtr.utr,
    businessName: String = "ACME"
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest("POST", "/").withFormUrlEncodedBody(
      "utr"       -> s"$psa",
    )
  def saUtrRequest(
    sa: String = matchUtr.utr,
    firstName: String = "A",
    lastName: String = "B"
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest("POST", "/").withFormUrlEncodedBody(
      "utr"     -> s"$sa",
    )

  val formValidationInputDataSetOrg: Seq[
    (
      MustTestMessage,
      Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)]
    )
  ] =
    Seq(
      (
        "if the selection is Unincorporated body :",
        Seq(
          (
            "CO Tax UTR must not be empty",
            "UIB",
            ctUtrRequest(ct = ""),
            "Enter a Corporation Tax Unique Taxpayer Reference"
          ),
          (
            "CO Tax UTR must be 10 digits",
            "UIB",
            ctUtrRequest(ct = "1" * 11),
            "Enter a 10-digit Unique Taxpayer Reference (UTR). If your UTR is 13 digits, enter only the last 10 digits"
          ),
          (
            "CO Tax UTR must contain only digits",
            "UIB",
            ctUtrRequest(ct = "12345678aa"),
            "Enter a 10-digit Unique Taxpayer Reference (UTR). If your UTR is 13 digits, enter only the last 10 digits"
          ),
          (
            "CO Tax UTR must be valid",
            "UIB",
            ctUtrRequest(ct = "1234567890"),
            "The Corporation Tax Unique Taxpayer Reference is not valid"
          )
        )
      ),
      (
        "if the selection is Limited Company :",
        Seq(
          (
            "CO Tax UTR must not be empty",
            "LTD",
            ctUtrRequest(ct = ""),
            "Enter a Corporation Tax Unique Taxpayer Reference"
          ),
          (
            "CO Tax UTR must be 10 digits",
            "LTD",
            ctUtrRequest(ct = "1" * 11),
            "Enter a 10-digit Unique Taxpayer Reference (UTR). If your UTR is 13 digits, enter only the last 10 digits"
          ),
          (
            "CO Tax UTR must contain only digits",
            "LTD",
            ctUtrRequest(ct = "12345678aa"),
            "Enter a 10-digit Unique Taxpayer Reference (UTR). If your UTR is 13 digits, enter only the last 10 digits"
          ),
          (
            "CO Tax UTR must be valid",
            "LTD",
            ctUtrRequest(ct = "1234567890"),
            "The Corporation Tax Unique Taxpayer Reference is not valid"
          )
        )
      ),
      (
        "if the selection is Non Resident Landlord :",
        Seq(
          (
            "SA UTR must not be empty",
            "NRL",
            nrlUtrRequest(utr = ""),
            "Enter a Self Assessment Unique Taxpayer Reference"
          ),
          (
            "SA UTR must be 10 digits",
            "NRL",
            nrlUtrRequest(utr = "12345678901"),
            "Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "SA UTR must contain only digits",
            "NRL",
            nrlUtrRequest(utr = "12345678aa"),
            "Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "SA UTR must be valid",
            "NRL",
            nrlUtrRequest(utr = "1234567890"),
            "The Self Assessment Unique Taxpayer Reference is not valid"
          )
        )
      ),
      (
        "if the selection is Limited Liability Partnership : ",
        Seq(
          (
            "Partnership Self Assessment UTR  must not be empty",
            "LLP",
            psaUtrRequest(psa = ""),
            "Enter a Partnership Self Assessment Unique Taxpayer Reference"
          ),
          (
            "Partnership Self Assessment UTR  must be 10 digits",
            "LLP",
            psaUtrRequest(psa = "1" * 11),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must contain only digits",
            "LLP",
            psaUtrRequest(psa = "12345678aa"),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must be valid",
            "LLP",
            psaUtrRequest(psa = "1234567890"),
            "The Partnership Self Assessment Unique Taxpayer Reference is not valid"
          )
        )
      ),
      (
        "if the selection is Limited Partnership : ",
        Seq(
          (
            "Partnership Self Assessment UTR  must not be empty",
            "LP",
            psaUtrRequest(psa = ""),
            "Enter a Partnership Self Assessment Unique Taxpayer Reference"
          ),
          (
            "Partnership Self Assessment UTR  must be 10 digits",
            "LP",
            psaUtrRequest(psa = "1" * 11),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must contain only digits",
            "LP",
            psaUtrRequest(psa = "12345678aa"),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must be valid",
            "LP",
            psaUtrRequest(psa = "1234567890"),
            "The Partnership Self Assessment Unique Taxpayer Reference is not valid"
          )
        )
      ),
      (
        "if the selection is Ordinary Business Partnership : ",
        Seq(
          (
            "Partnership Self Assessment UTR  must not be empty",
            "OBP",
            psaUtrRequest(psa = ""),
            "Enter a Partnership Self Assessment Unique Taxpayer Reference"
          ),
          (
            "Partnership Self Assessment UTR  must be 10 digits",
            "OBP",
            psaUtrRequest(psa = "1" * 11),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must contain only digits",
            "OBP",
            psaUtrRequest(psa = "12345678aa"),
            "Partnership Self Assessment Unique Taxpayer Reference must be 10 digits"
          ),
          (
            "Partnership Self Assessment UTR  must be valid",
            "OBP",
            psaUtrRequest(psa = "1234567890"),
            "The Partnership Self Assessment Unique Taxpayer Reference is not valid"
          )
        )
      )
    )

  val formValidationNameInputDataSetInd: Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)] =
    Seq(
      (
        "First name must not be empty",
        "SOP",
        saUtrRequest(matchUtr.utr, "", "b"),
        "Enter a first name"
      ),
      (
        "Last name must not be empty",
        "SOP",
        saUtrRequest(lastName = ""),
        "Enter a last name"
      ),
      (
        "First Name must not be more than 40 characters",
        "SOP",
        saUtrRequest(firstName = "a" * 41),
        "A first name cannot be more than 40 characters"
      ),
      (
        "Last Name must not be more than 40 characters",
        "SOP",
        saUtrRequest(lastName = "a" * 41),
        "A last name cannot be more than 40 characters"
      ),
      (
        "SA UTR must be valid",
        "SOP",
        saUtrRequest(sa = "1234567890"),
        "The Self Assessment Unique Taxpayer Reference is not valid"
      )
    )

  val formValidationInputDataSetInd: Seq[(InTestMessage, BusinessType, InputRequest, ErrorMessage)] = {
    Seq(
      (
        "SA UTR must not be empty",
        "SOP",
        saUtrRequest(sa = ""),
        "Enter a Self Assessment Unique Taxpayer Reference"
      ),
      (
        "SA UTR must be 10 digits",
        "SOP",
        saUtrRequest(sa = "12345678901"),
        "Self Assessment Unique Taxpayer Reference must be 10 digits"
      ),
      (
        "SA UTR must contain only digits",
        "SOP",
        saUtrRequest(sa = "12345678aa"),
        "Self Assessment Unique Taxpayer Reference must be 10 digits"
      ),
      (
        "SA UTR must be valid",
        "SOP",
        saUtrRequest(sa = "1234567890"),
        "The Self Assessment Unique Taxpayer Reference is not valid"
      )
    )

  }

}
