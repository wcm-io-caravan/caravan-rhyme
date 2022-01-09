/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.spring.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.spring.api.RhymeLinkBuilder;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;
import io.wcm.caravan.rhyme.spring.api.UrlFingerprinting;

/**
 * Implemention of {@link UrlFingerprinting} that will be created when {@link SpringRhyme#enableUrlFingerprinting()} is
 * called.
 */
class UrlFingerprintingImpl implements UrlFingerprinting {

  private final HttpServletRequest request;
  private final Rhyme rhyme;

  private final Map<String, String> timestampParameters = new LinkedHashMap<>();
  private boolean allTimestampsPresentInRequest = true;

  private final Map<String, Object> additionalQueryParameters = new LinkedHashMap<>();

  private Duration mutableMaxAge;
  private Duration immutableMaxAge;

  UrlFingerprintingImpl(HttpServletRequest request, Rhyme rhyme) {
    this.request = request;
    this.rhyme = rhyme;
  }

  @Override
  public UrlFingerprinting withTimestampParameter(String name, Supplier<Instant> valueSupplier) {

    String value = request.getParameter(name);

    if (StringUtils.isBlank(value)) {
      value = valueSupplier.get().toString();
      allTimestampsPresentInRequest = false;
    }

    timestampParameters.put(name, value);

    return this;
  }

  @Override
  public UrlFingerprinting withQueryParameter(String name, Object value) {

    additionalQueryParameters.put(name, value);
    return this;
  }

  @Override
  public UrlFingerprinting withConditionalMaxAge(Duration mutableMaxAgeValue, Duration immutableMaxAgeValue) {

    this.mutableMaxAge = mutableMaxAgeValue;
    this.immutableMaxAge = immutableMaxAgeValue;

    return this;
  }

  @Override
  public boolean isUsedInIncomingRequest() {

    return allTimestampsPresentInRequest;
  }

  @Override
  public RhymeLinkBuilder createLinkWith(WebMvcLinkBuilder linkBuilder) {

    if (!timestampParameters.isEmpty()) {
      applyMaxAge();
    }

    return new SpringRhymeLinkBuilder(linkBuilder, timestampParameters, additionalQueryParameters);
  }

  private void applyMaxAge() {

    if (allTimestampsPresentInRequest && immutableMaxAge != null) {
      rhyme.setResponseMaxAge(immutableMaxAge);
    }
    else if (mutableMaxAge != null) {
      rhyme.setResponseMaxAge(mutableMaxAge);
    }
  }
}
