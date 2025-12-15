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

import connectors.ConnectorTest
import models.{Address, BusinessRegistration, ReviewDetails}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DataCacheServiceSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]

  class Setup extends ConnectorTest {
    val connector: DataCacheService = new DataCacheService(mockSessionCacheRepo)
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {

      "fetch saved BusinessDetails from SessionCache" in new Setup {
        val reviewDetails: ReviewDetails =
          ReviewDetails(
            "ACME",
            Some("UIB"),
            Address("line1", "line2", None, None, None, "country"),
            "sap123",
            "safe123",
            isAGroup = false,
            directMatch = false,
            Some("agent123")
          )

        when(
          mockSessionCacheRepo
            .getFromSession[ReviewDetails](
              DataKey(ArgumentMatchers.any())
            )(any(), any())
        ).thenReturn(Future.successful(Some(reviewDetails)))

        val result: Future[Option[ReviewDetails]] = connector.fetchAndGetBusinessDetailsForSession
        await(result) must be(Some(reviewDetails))
      }
    }

    "saveAndReturnBusinessDetails" must {

      "save the fetched business details" in new Setup {
        val reviewDetails: ReviewDetails =
          ReviewDetails(
            "ACME",
            Some("UIB"),
            Address("line1", "line2", None, None, None, "country"),
            "sap123",
            "safe123",
            isAGroup = false,
            directMatch = false,
            Some("agent123")
          )

        when(
          mockSessionCacheRepo
            .putSession[ReviewDetails](
              DataKey(ArgumentMatchers.any()),
              ArgumentMatchers.eq(reviewDetails)
            )(any(), any(), any())
        ).thenReturn(Future.successful(reviewDetails))

        val result: Future[Option[ReviewDetails]] = connector.saveReviewDetails(reviewDetails)
        await(result).get must be(reviewDetails)
      }

    }

    "clearCache" must {

      "clear the cache for the session" in new Setup {

        when(
          mockSessionCacheRepo
            .deleteFromSession(any())
        ).thenReturn(Future.successful(()))

        val result: Future[Unit] = connector.clearCache
        await(result) must be(())
      }
    }

    "fetchAndGetBusinessRegistrationDetailsForSession" must {

      "fetch saved BusinessRegistration from SessionCache" in new Setup {
        val businessRegistration = BusinessRegistration(
          "Test Business",
          Address("line1", "line2", None, None, Some("AA1 1AA"), "GB")
        )

        when(
          mockSessionCacheRepo
            .getFromSession[BusinessRegistration](
              DataKey(ArgumentMatchers.any())
            )(any(), any())
        ).thenReturn(Future.successful(Some(businessRegistration)))

        val result: Future[Option[BusinessRegistration]] = connector.fetchAndGetBusinessRegistrationDetailsForSession
        await(result) must be(Some(businessRegistration))
      }
    }

    "saveBusinessRegistrationDetails" must {

      "save the business registration details" in new Setup {
        val businessRegistration = BusinessRegistration(
          "Test Business",
          Address("line1", "line2", None, None, Some("AA1 1AA"), "GB")
        )

        when(
          mockSessionCacheRepo
            .putSession[BusinessRegistration](
              DataKey(ArgumentMatchers.any()),
              ArgumentMatchers.eq(businessRegistration)
            )(any(), any(), any())
        ).thenReturn(Future.successful(businessRegistration))

        val result: Future[Option[BusinessRegistration]] = connector.saveBusinessRegistrationDetails(businessRegistration)
        await(result).get must be(businessRegistration)
      }
    }
  }

}
