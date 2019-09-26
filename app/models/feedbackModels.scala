/*
 * Copyright 2019 HM Revenue & Customs
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

package models

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

case class FeedBack(easyToUse: Option[Int] = None,
                    satisfactionLevel: Option[Int] = None,
                    howCanWeImprove: Option[String] = None,
                    referer: Option[String] = None
                   )

object FeedBack {
  implicit val formats = Json.format[FeedBack]
}

object FeedbackForm {

  val maxStringLength = 1200
  val maxOptionIntSize = 4
  val feedbackForm = Form(mapping(
    "easyToUse" -> optional(number(min = 0, max = maxOptionIntSize)),
    "satisfactionLevel" -> optional(number(min = 0, max = maxOptionIntSize)),
    "howCanWeImprove" -> optional(text(maxLength = maxStringLength)),
    "referer" -> optional(text)
  )
  (FeedBack.apply)(FeedBack.unapply))

}
