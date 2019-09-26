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

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode.Mode
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}

import scala.collection.JavaConversions._

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = BusinessCustomerFrontendAuditConnector
  override val loggingFilter = BusinessCustomerFrontendLoggingFilter
  override val frontendAuditFilter = BusinessCustomerFrontendAuditFilter

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(Play.current.configuration.underlying).verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html = {
    val url = request.path
    val serviceList =  configuration.getStringList(s"microservice.services.names").getOrElse(
      throw new Exception("No services available in application configuration"))
    serviceList.filter(url.toLowerCase.contains(_)).headOption match {
      case Some(x) => views.html.global_error(pageTitle, heading, message, service = x)(request, applicationMessages)
      case _ => views.html.global_error(pageTitle, heading, message)(request, applicationMessages)
    }
  }
  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object BusinessCustomerFrontendLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport  {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object BusinessCustomerFrontendAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport  {
  override lazy val maskedFormFields = Seq.empty
  override lazy val applicationPort = None
  override lazy val auditConnector = BusinessCustomerFrontendAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override protected def appNameConfiguration: Configuration = Play.current.configuration
}
