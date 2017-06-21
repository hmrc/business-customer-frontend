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

package utils

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.HttpResponse
import utils.ErrorMessageUtils._

class ErrorMessageUtilsSpec extends PlaySpec {

  "ErrorMessageUtils" must {
    "return true" when {
      "error number is 9001 and message is duplicate agent error" in {
        val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>9001</ErrorNumber><Message>The service HMRC-AGENT-AGENT requires unique identifiers</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

        matchErrorResponse(HttpResponse(502, Some(badGatewayResponse))) must be (true)
      }
    }

    "return true" when {
      "error number is 10004 and message is multiple enrolment error" in {
        val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>10004</ErrorNumber><Message>Multiple Enrolments are not Valid for this service</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

        matchErrorResponse(HttpResponse(502, Some(badGatewayResponse))) must be (true)
      }
    }

    "return false" when {
      "error number is 9002 and message is duplicate agent error" in {
        val badGatewayResponse = Json.parse( """{"statusCode":502,"message":"<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2004/03/addressing/fault</wsa:Action><wsa:MessageID>uuid:199814d0-9758-49d1-a2c0-d24300f67e2c</wsa:MessageID><wsa:RelatesTo>uuid:d1894fa0-b97d-4707-a814-e0c5ea79a01a</wsa:RelatesTo><wsa:To>http://schemas.xmlsoap.org/ws/2004/03/addressing/role/anonymous</wsa:To><wsse:Security><wsu:Timestamp wsu:Id=\"Timestamp-0fdb513d-1da4-4804-80b5-d04530653fac\"><wsu:Created>2017-03-22T14:23:00Z</wsu:Created><wsu:Expires>2017-03-22T14:28:00Z</wsu:Expires></wsu:Timestamp></wsse:Security></soap:Header><soap:Body><soap:Fault><faultcode>soap:Client</faultcode><faultstring>Business Rule Error</faultstring><faultactor>http://www.gateway.gov.uk/soap/2007/02/portal</faultactor><detail><GatewayDetails xmlns=\"urn:GSO-System-Services:external:SoapException\"><ErrorNumber>9002</ErrorNumber><Message>The service HMRC-AGENT-AGENT requires unique identifiers</Message><RequestID>0753B23CA0C14D23A4BBFC129795C42E</RequestID></GatewayDetails></detail></soap:Fault></soap:Body></soap:Envelope>"}	""")

        matchErrorResponse(HttpResponse(502, Some(badGatewayResponse))) must be (false)
      }
    }
  }
}
