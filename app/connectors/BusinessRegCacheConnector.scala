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

import play.api.libs.json.Format
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRegCacheConnector @Inject()(sessionCacheRepository: SessionCacheRepository) {

  def fetchAndGetCachedDetails[T](formId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: Format[T]): Future[Option[T]] =
    sessionCacheRepository.getFromSession[T](DataKey[T](formId))

  def cacheDetails[T](formId: String, formData: T)(implicit hc: HeaderCarrier, ec: ExecutionContext, formats: Format[T]): Future[T] =
    sessionCacheRepository.putSession[T](DataKey[T](formId), formData)
}
