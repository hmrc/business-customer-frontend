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

package controllers

import config.FrontendAuthConnector
import connectors.BackLinkCacheConnector
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.RunMode

object ExternalLinkController extends ExternalLinkController {

  val authConnector = FrontendAuthConnector
  override val controllerId: String = "ExternalLinkController"
  override val backLinkCacheConnector = BackLinkCacheConnector
  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait ExternalLinkController extends BackLinkController with RunMode {

  def backLink(serviceName: String) = AuthAction(serviceName).async { implicit bcContext =>
    currentBackLink.map(backLink =>
      backLink match {
        case Some(x) => Redirect(x)
        case None => throw new RuntimeException(s"[ExternalLinkController][backLink] No Back Link found. Service: $serviceName")
      }
    )
  }
}
