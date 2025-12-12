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

import models.{BusinessRegistration, ReviewDetails}
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnector @Inject()(sessionCache: SessionCacheRepository){

  import sessionCache._
  private val sourceId: String = "BC_Business_Details"
  private val sourceIdForFormPayload: String = "BC_Business_Details_form_payload"

  def fetchAndGetBusinessDetailsForSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ReviewDetails]] =
   getFromSession[ReviewDetails](DataKey(sourceId))

  def fetchAndGetBusinessRegistrationDetailsForSession(implicit hc: HeaderCarrier): Future[Option[BusinessRegistration]] =
   getFromSession[BusinessRegistration](DataKey(sourceIdForFormPayload))

  def saveReviewDetails(reviewDetails: ReviewDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ReviewDetails]] =
    putSession[ReviewDetails](DataKey(sourceId), reviewDetails).map(Some(_))

  def saveBusinessRegistrationDetails(businessRegistration: BusinessRegistration)
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[BusinessRegistration]] =
    putSession[BusinessRegistration](DataKey(sourceIdForFormPayload), businessRegistration).map(Some(_))

  def clearCache(implicit hc: HeaderCarrier): Future[Unit] = deleteFromSession

}
