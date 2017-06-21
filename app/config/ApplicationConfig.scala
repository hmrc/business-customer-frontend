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

package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

  val defaultBetaFeedbackUrl: String
  def betaFeedbackUrl(service: String, returnUri: String): String
  val betaFeedbackUnauthenticatedUrl: String
  val analyticsToken: Option[String]
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val defaultTimeoutSeconds: Int
  val timeoutCountdown: Int
  val logoutUrl: String

  def serviceSignOutUrl(service: String): String
  def validateNonUkClientPostCode(service: String): Boolean
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = s"${baseUrl("contact-frontend")}"

  val contactFormServiceIdentifier = "BUSINESS-CUSTOMER"

  override lazy val defaultBetaFeedbackUrl = s"$contactHost/contact/beta-feedback"
  override def betaFeedbackUrl(service: String, returnUri: String) = {
    val feedbackUrl = if (service != "") configuration.getString(s"delegated-service.${service.toLowerCase}.beta-feedback-url").getOrElse(defaultBetaFeedbackUrl) else defaultBetaFeedbackUrl
    feedbackUrl + "?return=" + returnUri
  }
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val analyticsToken: Option[String] = configuration.getString(s"google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString(s"google-analytics.host").getOrElse("auto")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt
  override lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  override lazy val logoutUrl = s"""${configuration.getString(s"logout.url").getOrElse("/gg/sign-out")}"""

  override def serviceSignOutUrl(service: String): String = {
    if (service != "") configuration.getString(s"delegated-service.${service.toLowerCase}.sign-out-url").getOrElse(logoutUrl) else logoutUrl
  }

  override def validateNonUkClientPostCode(service: String) = {
    configuration.getBoolean(s"microservice.services.${service.toLowerCase.trim}.validateNonUkClientPostCode").getOrElse(false)
  }
}
