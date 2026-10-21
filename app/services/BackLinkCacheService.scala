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

package services

import models.BackLinkModel
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheService @Inject()(sessionCacheRepository: SessionCacheRepository) {

  val sourceId: String = "BC_Back_Link"

  private def getKey(pageId: String): DataKey[BackLinkModel] = DataKey[BackLinkModel](s"$sourceId:$pageId")

  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    sessionCacheRepository.getFromSession[BackLinkModel](getKey(pageId)).map(_.flatMap(_.backLink))
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    sessionCacheRepository.putSession[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(_ => returnUrl)
  }
}
