/*
 * Copyright 2022 HM Revenue & Customs
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

package config

import controllers._
import controllers.nonUKReg._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

class Wiring extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    bindControllers
  }

  private def bindControllers: Seq[Binding[_]] = Seq(
    bind[ApplicationConfig].toSelf.eagerly(),
    bind[BCHandler].to(classOf[BCHandlerImpl]),
    bind[AuthConnector].to(classOf[DefaultAuthConnector]),
    bind[AgentRegisterNonUKClientController].toSelf,
    bind[BusinessRegController].toSelf,
    bind[NRLQuestionController].toSelf,
    bind[OverseasCompanyRegController].toSelf,
    bind[PaySAQuestionController].toSelf,
    bind[UpdateNonUKBusinessRegistrationController].toSelf,
    bind[ApplicationController].toSelf,
    bind[BusinessCustomerController].toSelf,
    bind[BusinessVerificationController].toSelf,
    bind[ExternalLinkController].toSelf,
    bind[HomeController].toSelf,
    bind[ReviewDetailsController].toSelf
  )
}
