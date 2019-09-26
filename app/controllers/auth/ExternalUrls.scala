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

package controllers.auth

import play.api.Mode.Mode
import play.api.Play.current
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.RunMode

object ExternalUrls extends RunMode {

  val companyAuthHost = s"${Play.configuration.getString("microservice.services.auth.company-auth.host").getOrElse("")}"

  val loginCallback = Play.configuration.getString(s"microservice.services.auth.login-callback.url").getOrElse("/business-customer")

  def continueURL(serviceName: String) = s"$loginCallback/$serviceName"

  val loginPath = s"${Play.configuration.getString("microservice.services.auth.login-path").getOrElse("sign-in")}"

  val loginURL = s"$companyAuthHost/gg/$loginPath"

  def signIn(serviceName: String) = s"$companyAuthHost/gg/$loginPath?continue=${continueURL(serviceName)}"

  val signOut = s"$companyAuthHost/gg/sign-out"

  def agentConfirmationPath(service:String) :String = {
    Play.configuration.getString(s"microservice.services.${service.toLowerCase}.agentConfirmationUrl")
      .getOrElse("/ated-subscription/agent-confirmation")
  }

  def serviceWelcomePath(service: String): String = {
    Play.configuration.getString(s"microservice.services.${service.toLowerCase}.serviceStartUrl").getOrElse("#")
  }

  def serviceAccountPath(service: String): String = {
    Play.configuration.getString(s"microservice.services.${service.toLowerCase}.accountSummaryUrl").getOrElse("#")
  }

  val addClientEmailPath = Play.configuration.getString(s"microservice.services.agent-client-mandate-frontend.select-service").getOrElse("#")
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
