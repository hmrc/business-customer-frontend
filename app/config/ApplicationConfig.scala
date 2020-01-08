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

package config

import java.io.File

import javax.inject.{Inject, Named, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.util.Try

@Singleton
class ApplicationConfig @Inject()(val conf: ServicesConfig,
                                  val runMode: RunMode,
                                  val oldConfig: Configuration,
                                  val environment: Environment,
                                  @Named("appName") val appName: String) extends BCUtils {

  private val contactHost = conf.getConfString("contact-frontend.host", "")

  val serviceList: Seq[String] =  oldConfig.getStringSeq("microservice.services.names").getOrElse(
    throw new Exception("No services available in application configuration"))

  val contactFormServiceIdentifier = "BUSINESS-CUSTOMER"

  lazy val defaultBetaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  lazy val analyticsToken: Option[String] = Try(conf.getString("google-analytics.token")).toOption
  lazy val analyticsHost: String = conf.getConfString("google-analytics.host", "auto")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val defaultTimeoutSeconds: Int = conf.getInt("defaultTimeoutSeconds").toInt
  lazy val timeoutCountdown: Int = conf.getInt("timeoutCountdown").toInt
  lazy val logoutUrl: String = conf.getString("logout.url")
  lazy val businessCustomer: String = conf.baseUrl("business-customer")
  lazy val businessMatching: String = conf.baseUrl("business-matching")
  lazy val taxEnrolments: String = conf.baseUrl("tax-enrolments")
  lazy val companyAuthHost: String = conf.getString("microservice.services.auth.company-auth.host")
  lazy val addClientEmailPath: String = conf.getString(s"microservice.services.agent-client-mandate-frontend.select-service")

  lazy val loginCallback: String = conf.getString("microservice.services.auth.login-callback.url")
  lazy val loginPath: String = conf.getString("microservice.services.auth.login-path")
  lazy val loginURL = s"$companyAuthHost/gg/$loginPath"
  lazy val signOut = s"$companyAuthHost/gg/sign-out"

  lazy val baseUri: String = conf.baseUrl("cachable.session-cache")
  lazy val defaultSource: String = appName
  lazy val domain: String = conf.getConfString(
    "cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'")
  )

  def serviceSignOutUrl(service: String): String = conf.getConfString(s"delegated-service.${service.toLowerCase}.sign-out-url", logoutUrl)
  def continueURL(serviceName: String) = s"$loginCallback/$serviceName"
  def signIn(serviceName: String) = s"$companyAuthHost/gg/$loginPath?continue=${continueURL(serviceName)}"

  def agentConfirmationPath(service:String): String = {
    conf.getConfString(s"${service.toLowerCase}.agentConfirmationUrl", "/ated-subscription/agent-confirmation")
  }
  
  def validateNonUkCode(service: String): Boolean = {
      conf.getConfBool(s"${service.toLowerCase.trim}.validateNonUkClientPostCode", defBool = false)
  }

  def serviceWelcomePath(service: String): String = conf.getString(s"microservice.services.${service.toLowerCase}.serviceStartUrl")
  def serviceAccountPath(service: String): String = conf.getString(s"microservice.services.${service.toLowerCase}.accountSummaryUrl")
  def serviceRedirectUrl(service: String): String = conf.getString(s"microservice.services.${service.toLowerCase}.serviceRedirectUrl")

  private def env: String = runMode.env
  private def isRelativeUrl(url: String): Boolean = url.matches("^[/][^/].*")

  def isRelativeOrDev(url: String): Boolean = isRelativeUrl(url) || env == "Dev"

  def getFile(path: String): File = environment.getFile(path)
}
