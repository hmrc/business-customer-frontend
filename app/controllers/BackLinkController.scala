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

package controllers

import connectors.BackLinkCacheConnector
import models.{BackLinkModel, BusinessCustomerContext}
import play.api.mvc.{AnyContent, Request, Call, Result}
import play.mvc.Http.Response
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BackLinkController extends BaseController {
  val controllerId: String
  val backLinkCacheConnector: BackLinkCacheConnector

  def setBackLink(pageId: String, returnUrl: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier) : Future[Option[String]] = {
    backLinkCacheConnector.saveBackLink(pageId, returnUrl)
  }

  def currentBackLink(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier):Future[Option[String]] = {
    backLinkCacheConnector.fetchAndGetBackLink(controllerId)
  }

  def RedirectToExernal(redirectCall: String, returnUrl: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier) = {
    for {
      cache <- setBackLink(ExternalLinkController.controllerId, returnUrl)
    } yield{
      Redirect(redirectCall)
    }
  }

  def ForwardBackLinkToNextPage(nextPageId: String, redirectCall: Call)(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    for {
      currentBackLink <- currentBackLink
      cache <- setBackLink(nextPageId, currentBackLink)
    } yield{
      Redirect(redirectCall)
    }
  }

  def RedirectWithBackLink(nextPageId: String, redirectCall: Call, backCall: Option[String])(implicit bcContext: BusinessCustomerContext, hc: HeaderCarrier): Future[Result] = {
    for {
      cache <- setBackLink(nextPageId, backCall)
    } yield{
      Redirect(redirectCall)
    }
  }
}
