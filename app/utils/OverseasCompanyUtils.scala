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

package utils

import models.OverseasCompanyDisplayDetails
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._

object OverseasCompanyUtils {
  def displayDetails(isAgent: Boolean, addClient: Boolean, service: String) = {

    (isAgent, addClient) match {
      case (true, true) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.agent.add-client.title"),
          Messages("bc.nonuk.overseas.agent.add-client.header"),
          Messages("bc.nonuk.overseas.agent.add-client.subheader", service.toUpperCase),
          addClient)
      case (true, false) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.agent.title"),
          Messages("bc.nonuk.overseas.agent.header"),
          Messages("bc.nonuk.overseas.agent.subheader", service.toUpperCase),
          addClient)
      case (_, _) =>
        OverseasCompanyDisplayDetails(
          Messages("bc.nonuk.overseas.client.title"),
          Messages("bc.nonuk.overseas.client.header"),
          Messages("bc.nonuk.overseas.client.subheader", service.toUpperCase),
          addClient)
    }

  }
}
