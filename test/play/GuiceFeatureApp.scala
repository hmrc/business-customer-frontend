/*
 * Copyright 2023 HM Revenue & Customs
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

package play

import config.ApplicationConfig
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.Injecting
import repositories.SessionCacheRepository

trait GuiceFeatureApp extends AnyFeatureSpec with GuiceOneServerPerSuite with Injecting with MockitoSugar {

  lazy implicit val appConfig: ApplicationConfig      = inject[ApplicationConfig]
  implicit lazy val mcc: MessagesControllerComponents = inject[MessagesControllerComponents]

  private val sessionCacheRepositoryStub: SessionCacheRepository =
    mock[SessionCacheRepository]

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[SessionCacheRepository].toInstance(sessionCacheRepositoryStub)
      )
      .build()

}
