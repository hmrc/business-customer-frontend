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

package connectors

import config.BusinessCustomerSessionCache
import models.ReviewDetails
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataCacheConnector extends DataCacheConnector {
  val sessionCache: SessionCache = BusinessCustomerSessionCache
  val sourceId: String = "BC_Business_Details"
}

trait DataCacheConnector {

  def sessionCache: SessionCache

  def sourceId: String

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = sessionCache.fetchAndGetEntry[ReviewDetails](sourceId)

  def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier): Future[Option[ReviewDetails]] = {
    val result = sessionCache.cache[ReviewDetails](sourceId, reviewDetails)
    result flatMap {
      data => Future.successful(data.getEntry[ReviewDetails](sourceId))
    }
  }

  def clearCache(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    sessionCache.remove()
  }

}
