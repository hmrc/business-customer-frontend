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
    bind(classOf[ApplicationConfig]).toSelf.eagerly(),
    bind(classOf[BCHandler]).to(classOf[BCHandlerImpl]),
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector]),
    bind(classOf[AgentRegisterNonUKClientController]).toSelf,
    bind(classOf[BusinessRegController]).toSelf,
    bind(classOf[NRLQuestionController]).toSelf,
    bind(classOf[OverseasCompanyRegController]).toSelf,
    bind(classOf[PaySAQuestionController]).toSelf,
    bind(classOf[UpdateNonUKBusinessRegistrationController]).toSelf,
    bind(classOf[ApplicationController]).toSelf,
    bind(classOf[BusinessCustomerController]).toSelf,
    bind(classOf[BusinessVerificationController]).toSelf,
    bind(classOf[ExternalLinkController]).toSelf,
    bind(classOf[HomeController]).toSelf,
    bind(classOf[ReviewDetailsController]).toSelf
  )
}
