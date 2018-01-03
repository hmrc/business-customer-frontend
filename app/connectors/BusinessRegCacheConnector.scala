/*
 * Copyright 2018 HM Revenue & Customs
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

import config.BusinessCustomerSessionCache
import models.{BusinessRegistration, ReviewDetails}
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object BusinessRegCacheConnector extends BusinessRegCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_NonUK_Business_Details"
}

trait BusinessRegCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  def fetchAndGetCachedDetails[T](formId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    sessionCache.fetchAndGetEntry[T](key = formId)

  def cacheDetails[T](formId: String, formData: T)(implicit hc: HeaderCarrier, formats: Format[T]): Future[T] = {
    sessionCache.cache[T](formId, formData).map(cacheMap => formData)
  }
}
