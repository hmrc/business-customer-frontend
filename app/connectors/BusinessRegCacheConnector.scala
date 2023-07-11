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

package connectors

import config.ApplicationConfig

import javax.inject.Inject
import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class BusinessRegCacheConnector @Inject()(val http: DefaultHttpClient,
                                          config: ApplicationConfig) extends SessionCache {

  val baseUri: String = config.baseUri
  val defaultSource: String = config.defaultSource
  val domain: String = config.domain

  val sourceId: String = "BC_NonUK_Business_Details"

  def fetchAndGetCachedDetails[T](formId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: Format[T]): Future[Option[T]] =
    fetchAndGetEntry[T](key = formId)

  def cacheDetails[T](formId: String, formData: T)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: Format[T]): Future[T] = {
    cache[T](formId, formData).map(_ => formData)
  }
}
