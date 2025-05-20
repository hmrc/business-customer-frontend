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

package config

import javax.inject.{Inject, Named, Singleton}
import play.api.{Configuration, Environment, Logging}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.util.Try

@Singleton
class ApplicationConfig @Inject()(val conf: ServicesConfig,
                                  val oldConfig: Configuration,
                                  val environment: Environment,
                                  @Named("appName") val appName: String) extends BCUtils with Logging {

  val serviceList: Seq[String] =  oldConfig.getOptional[Seq[String]]("microservice.services.names").getOrElse(
    throw new Exception("No services available in application configuration"))

  lazy val logoutUrl: String = conf.getString("logout.url")
  lazy val businessCustomer: String = conf.baseUrl("business-customer")
  lazy val businessMatching: String = conf.baseUrl("business-matching")
  lazy val taxEnrolments: String = conf.baseUrl("tax-enrolments")
  lazy val basGatewayHost: String = conf.getString("microservice.services.auth.bas-gateway-frontend.host")
  lazy val accessibilityStatementFrontendHost: String = conf.getString(s"microservice.services.accessibility-statement-frontend.host")
  lazy val accessibilityStatementFrontendUrl: String = conf.getString(s"microservice.services.accessibility-statement-frontend.url")
  lazy val platformHost: String = Try(conf.getString("platform.frontend.host")).getOrElse("")
  lazy val loginCallback: String = conf.getString("microservice.services.auth.login-callback.url")
  lazy val loginURL = s"$basGatewayHost/bas-gateway/sign-in"
  lazy val signOut = s"$basGatewayHost/bas-gateway/sign-out-without-state"

  lazy val baseUri: String = conf.baseUrl("cachable.session-cache")
  lazy val defaultSource: String = appName
  lazy val domain: String = conf.getConfString(
    "cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'")
  )

  def accessibilityStatementFrontendUrl(service: String, referrerUrl: String): String = {
    val statement = service.toUpperCase() match {
      case "AMLS" => "anti-money-laundering"
      case "ATED" => "ated-subscription"
      case "AWRS" => "alcohol-wholesale-scheme"
      case "FHDDS" => "fhdds"
      case _ => logger.info(s"[ApplicationConfig][accessibilityStatementFrontendUrl] - Invalid service: '$service'")
    }
    s"$accessibilityStatementFrontendHost$accessibilityStatementFrontendUrl/$statement?referrerUrl=$referrerUrl"
  }

  def continueURL(serviceName: String) = s"$loginCallback/$serviceName"

  def agentConfirmationPath(service:String): String = {
    conf.getConfString(s"${service.toLowerCase}.agentConfirmationUrl", "/ated-subscription/agent-confirmation")
  }

  def validateNonUkCode(service: String): Boolean = {
      conf.getConfBool(s"${service.toLowerCase.trim}.validateNonUkClientPostCode", defBool = false)
  }

  def serviceRedirectUrl(service: String): String = conf.getString(s"microservice.services.${service.toLowerCase}.serviceRedirectUrl")

  lazy val allowedHosts = oldConfig.get[Seq[String]]("allowedHosts")

  lazy val backToInformHMRCNrlUrl: Option[String] = Option(conf.getString("microservice.services.agent-client-mandate-frontend.informHMRCNrlUrl"))

  def haveYouRegisteredUrl: String = conf.getString(s"microservice.services.awrs.haveYouRegisteredUrl")
  def enrolmentJourneyFeature: Boolean = Try(conf.getBoolean("feature.enrolmentJourney")).getOrElse(false)
}
