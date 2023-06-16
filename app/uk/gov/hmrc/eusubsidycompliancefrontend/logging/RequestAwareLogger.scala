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

package uk.gov.hmrc.eusubsidycompliancefrontend.logging

import org.slf4j.MDC
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, RequestId}

class RequestAwareLogger(clazz: Class[_]) {

  private val underlying = Logger(clazz)

  def trace(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.trace(msg))
  def debug(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.debug(msg))
  def info(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.info(msg))
  def warn(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.warn(msg))
  def error(msg: => String)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(underlying.error(msg))
  def error(msg: => String, error: => Throwable)(implicit hc: HeaderCarrier): Unit = withRequestIDsInMDC(
    underlying.error(msg, error)
  )

  private val correlationIdKey: String = "correlationId"

  private def withRequestIDsInMDC(f: => Unit)(implicit hc: HeaderCarrier): Unit = {
    val requestId = hc.requestId.getOrElse(RequestId("Undefined"))
    val correlationId = hc.otherHeaders
      .collectFirst { case (key, value) if key.equalsIgnoreCase(correlationIdKey) => value }

    MDC.put(HeaderNames.xRequestId, requestId.value)
    correlationId.foreach(MDC.put(correlationIdKey, _))
    f
    MDC.remove(HeaderNames.xRequestId)
    correlationId.foreach(_ => MDC.remove(correlationIdKey))
  }

}
