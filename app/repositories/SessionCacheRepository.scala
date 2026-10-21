/*
 * Copyright 2026 HM Revenue & Customs
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

package repositories

import config.ApplicationConfig
import crypto.MongoCrypto
import play.api.libs.json.{JsValue, Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheRepository @Inject()(
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport,
    mongoCrypto: MongoCrypto
)(implicit
    ec: ExecutionContext,
    appConfig: ApplicationConfig
) {

  private val cacheRepo = new MongoCacheRepository[HeaderCarrier](
    mongoComponent = mongoComponent,
    collectionName = "sessions",
    ttl = appConfig.mongoSessionTtl,
    timestampSupport = timestampSupport,
    cacheIdType = SessionCacheId
  )

  def putSession[T: Writes](
      dataKey: DataKey[T],
      data: T
  )(implicit hc: HeaderCarrier): Future[T] =
    cacheRepo
      .put[JsValue](hc)(DataKey[JsValue](dataKey.unwrap), mongoCrypto.encrypt(data))
      .map(_ => data)

  def getFromSession[T: Reads](dataKey: DataKey[T])(implicit hc: HeaderCarrier): Future[Option[T]] =
    cacheRepo.get[JsValue](hc)(DataKey[JsValue](dataKey.unwrap)).map(_.map(mongoCrypto.decrypt(_).as[T]))

  def deleteFromSession(implicit hc: HeaderCarrier): Future[Unit] =
    cacheRepo.deleteEntity(hc)

}
