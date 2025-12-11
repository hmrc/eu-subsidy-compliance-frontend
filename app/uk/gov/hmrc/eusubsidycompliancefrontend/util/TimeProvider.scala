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

package uk.gov.hmrc.eusubsidycompliancefrontend.util

import com.google.inject.{ImplementedBy, Singleton}

import java.time.{LocalDate, LocalDateTime, ZoneId}

@ImplementedBy(classOf[SystemTimeProvider])
trait TimeProvider {
  def today: LocalDate
  def zonedToday(z: ZoneId): LocalDate
  def now: LocalDateTime
  def getMinusDaysForVal(minusDays: Int): LocalDate
}

@Singleton
class SystemTimeProvider extends TimeProvider {
  override def today: LocalDate = LocalDate.now()
  override def zonedToday(z: ZoneId): LocalDate = LocalDate.now(z)
  override def now: LocalDateTime = LocalDateTime.now()
  override def getMinusDaysForVal(minusDays: Int): LocalDate = today.minusDays(minusDays)
}
