/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import play.api.mvc.Request

import scala.util.Random

object SessionUtils {

  def getUniqueAckNo: String = {
    val length = 32
    val nanoTime = System.nanoTime()
    val restChars = length - nanoTime.toString.length
    val randomChars = Random.alphanumeric.take(restChars).mkString
    randomChars + nanoTime
  }

  def findServiceInRequest(request: Request[_])(implicit appConfig: ApplicationConfig): String = {
    val requestParts = request.uri.split("/")
    val serviceList = appConfig.serviceList

    requestParts.find(part => serviceList.contains(part.toLowerCase)).getOrElse("unknownservice")
  }

}
