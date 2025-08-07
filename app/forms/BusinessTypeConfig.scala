/*
 * Copyright 2025 HM Revenue & Customs
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

package forms

import forms.BusinessVerificationForms.{businessName}
import play.api.data.Form

object BusinessTypeConfig {

  case class BusinessFormConfig[T](form: Form[T],
                                 questionKey: String,
                                 utrQuestionKey: String
                               )

  val configs: Map[String, BusinessFormConfig[_]] = Map(
    "LTD" -> BusinessFormConfig[BusinessName](businessName, "bc.business-verification.businessNamefield", "bc.business-verification.coUTR"),
    "LLP" -> BusinessFormConfig[BusinessName](businessName, "bc.business-verification.partnerNameField", "bc.business-verification.psaUTR"),
    "LP" -> BusinessFormConfig[BusinessName](businessName, "bc.business-verification.partnerNameField", "bc.business-verification.psaUTR"),
    "OBP" -> BusinessFormConfig[BusinessName](businessName, "bc.business-verification.partnerNameField", "bc.business-verification.psaUTR"),
    "UIB" -> BusinessFormConfig[BusinessName](businessName, "bc.business-verification.businessNamefield", "bc.business-verification.psaUTR")
  )

}
