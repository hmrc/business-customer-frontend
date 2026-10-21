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

package connectors

import models.{Address, BusinessRegistration, ReviewDetails}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import repositories.SessionCacheRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.mongo.cache.DataKey

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DataCacheConnectorSpec extends PlaySpec with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val reviewDetails: ReviewDetails =
    ReviewDetails("ACME", Some("UIB"), Address("line1", "line2", None, None, None, "country"), "sap123", "safe123", isAGroup = false, directMatch = false, Some("agent123"))
  val registrationDetails: BusinessRegistration = BusinessRegistration("ACME", Address("line1", "line2", None, None, None, "country"))

  class Setup {
    val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]
    val connector: DataCacheConnector = new DataCacheConnector(mockSessionCacheRepository)
  }

  "DataCacheConnector" must {

    "fetchAndGetBusinessDetailsForSession" must {
      "fetch saved BusinessDetails from the session cache" in new Setup {
        when(mockSessionCacheRepository.getFromSession[ReviewDetails](DataKey[ReviewDetails](eqTo("BC_Business_Details")))(any(), any()))
          .thenReturn(Future.successful(Some(reviewDetails)))

        await(connector.fetchAndGetBusinessDetailsForSession) must be(Some(reviewDetails))
      }
    }

    "fetchAndGetBusinessRegistrationDetailsForSession" must {
      "fetch saved RegistrationDetails from the session cache" in new Setup {
        when(mockSessionCacheRepository.getFromSession[BusinessRegistration](DataKey[BusinessRegistration](eqTo("BC_Business_Details_form_payload")))(any(), any()))
          .thenReturn(Future.successful(Some(registrationDetails)))

        await(connector.fetchAndGetBusinessRegistrationDetailsForSession) must be(Some(registrationDetails))
      }
    }

    "saveReviewDetails" must {
      "save the fetched business details" in new Setup {
        when(mockSessionCacheRepository.putSession[ReviewDetails](DataKey[ReviewDetails](eqTo("BC_Business_Details")), eqTo(reviewDetails))(any(), any()))
          .thenReturn(Future.successful(reviewDetails))

        await(connector.saveReviewDetails(reviewDetails)) must be(Some(reviewDetails))
      }
    }

    "saveBusinessRegistrationDetails" must {
      "save the business registration details" in new Setup {
        when(mockSessionCacheRepository.putSession[BusinessRegistration](DataKey[BusinessRegistration](eqTo("BC_Business_Details_form_payload")), eqTo(registrationDetails))(any(), any()))
          .thenReturn(Future.successful(registrationDetails))

        await(connector.saveBusinessRegistrationDetails(registrationDetails)) must be(Some(registrationDetails))
      }
    }

    "clearCache" must {
      "clear the cache for the session" in new Setup {
        when(mockSessionCacheRepository.deleteFromSession(any())).thenReturn(Future.successful(()))

        await(connector.clearCache) must be(())
        verify(mockSessionCacheRepository).deleteFromSession(any())
      }
    }
  }
}
