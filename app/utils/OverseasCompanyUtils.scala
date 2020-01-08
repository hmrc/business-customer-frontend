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

package utils

import models.OverseasCompanyDisplayDetails

trait OverseasCompanyUtils {

  def displayDetails(isAgent: Boolean, addClient: Boolean, service: String): OverseasCompanyDisplayDetails = {
    (isAgent, addClient) match {
      case (true, true) =>
        OverseasCompanyDisplayDetails(
          "bc.nonuk.overseas.agent.add-client.title",
          "bc.nonuk.overseas.agent.add-client.header",
          "bc.nonuk.overseas.agent.add-client.subheader",
          addClient
        )
      case (true, false) =>
        OverseasCompanyDisplayDetails(
          "bc.nonuk.overseas.agent.title",
          "bc.nonuk.overseas.agent.header",
          "bc.nonuk.overseas.agent.subheader",
          addClient
        )
      case (_, _) =>
        OverseasCompanyDisplayDetails(
          "bc.nonuk.overseas.client.title",
          "bc.nonuk.overseas.client.header",
          "bc.nonuk.overseas.client.subheader",
          addClient
        )
    }
  }
}
