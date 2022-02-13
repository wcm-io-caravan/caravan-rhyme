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
package io.wcm.caravan.rhyme.impl.metadata;

import java.time.Duration;
import java.util.function.Supplier;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * Minimal implementation of {@link RequestMetricsCollector} containing only the essential functionality required for
 * calculation of the max-age header to work
 */
public class MaxAgeOnlyCollector implements RequestMetricsCollector {

  private volatile Integer responseMaxAge;

  @Override
  public void onResponseRetrieved(String resourceUri, String resourceTitle, Integer maxAgeSeconds, long responseTimeMicros) {

    if (maxAgeSeconds != null) {
      updateMaxAgeLimit(maxAgeSeconds);
    }
  }

  @Override
  public void setResponseMaxAge(Duration duration) {

    updateMaxAgeLimit((int)Math.min(Integer.MAX_VALUE, duration.getSeconds()));
  }

  private synchronized void updateMaxAgeLimit(int maxAge) {
    if (responseMaxAge == null || maxAge < responseMaxAge) {
      responseMaxAge = maxAge;
    }
  }

  @Override
  public Integer getResponseMaxAge() {

    return responseMaxAge;
  }

  @Override
  public HalResource createMetadataResource(LinkableResource resourceImpl) {

    return null;
  }

  @Override
  public void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros) {
    // ignore all measurements
  }

  @Override
  public RequestMetricsStopwatch startStopwatch(Class measuringClass, Supplier<String> taskDescription) {
    return new RequestMetricsStopwatch() {

      @Override
      public void close() {
        // ignore all measurements
      }
    };
  }
}
