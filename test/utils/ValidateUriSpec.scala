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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ValidateUriSpec extends AnyWordSpecLike with Matchers {

  "ValidateUriSpec" should {

    "Accept an array of strings" in {
      ValidateUri.isValid(Seq("value 1", "value 2", "value 3"), "value 1")
    }

    "Return false if an empty array is passed" in {
      ValidateUri.isValid(Seq.empty, "value 1") shouldBe false
    }

    "Return true when value exists" in {
      ValidateUri.isValid(Seq("value 1", "value 2"), "value 2") shouldBe true
    }

    "Return false when value doesn't exist" in {
      ValidateUri.isValid(Seq("value 1", "value 2"), "value 3") shouldBe false
    }

  }

}
