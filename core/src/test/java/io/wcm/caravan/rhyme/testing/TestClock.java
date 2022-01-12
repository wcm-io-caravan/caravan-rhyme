/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.testing;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

/**
 * Copied from https://github.com/Mercateo and adjusted to get rid of transient lombok dependency
 * @author Stephan Prätsch <stephan.praetsch@mercateo.com>
 */
public class TestClock extends Clock {

  private Instant instant;

  private final ZoneId zone;

  private TestClock(Instant instant, ZoneId zone) {
    this.instant = instant;
    this.zone = zone;
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new TestClock(instant, zone);
  }

  @Override
  public Instant instant() {
    return instant;
  }

  public void fastForward(TemporalAmount temporalAmount) {
    set(instant().plus(temporalAmount));
  }

  public void rewind(TemporalAmount temporalAmount) {
    set(instant().minus(temporalAmount));
  }

  public void set(Instant instant) {
    this.instant = instant;
  }

  public static TestClock fixed(Instant instant, ZoneId zone) {
    return new TestClock(instant, zone);
  }

  public static TestClock fixed(OffsetDateTime odt) {
    return fixed(odt.toInstant(), odt.getOffset());
  }
}
