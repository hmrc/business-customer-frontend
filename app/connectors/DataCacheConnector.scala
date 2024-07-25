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
import models.ReviewDetails
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnector @Inject()(val http: HttpClientV2,
                                   config: ApplicationConfig)
                                  (implicit ec: ExecutionContext) extends SessionCache {

  val baseUri: String = config.baseUri
  val defaultSource: String = config.defaultSource
  val domain: String = config.domain

  val sourceId: String = "BC_Business_Details"

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ReviewDetails]] =
    fetchAndGetEntry[ReviewDetails](sourceId)

  def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ReviewDetails]] = {
    cache[ReviewDetails](sourceId, reviewDetails) map {
      _.getEntry[ReviewDetails](sourceId)
    }
  }

  def clearCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = remove()

  def httpClientV2: HttpClientV2 = http

}
