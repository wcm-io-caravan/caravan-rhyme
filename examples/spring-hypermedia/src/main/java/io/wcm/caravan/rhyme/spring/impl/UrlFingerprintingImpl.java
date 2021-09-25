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
  public UrlFingerprinting withConditionalMaxAge(Duration mutableMaxAge, Duration immutableMaxAge) {

    this.mutableMaxAge = mutableMaxAge;
    this.immutableMaxAge = immutableMaxAge;

    return this;
  }

  @Override
  public boolean isUsedInIncomingRequest() {

    return allTimestampsPresentInRequest;
  }

  @Override
  public RhymeLinkBuilder createLinkWith(WebMvcLinkBuilder linkBuilder) {

    if (!timestampParameters.isEmpty()) {
      if (allTimestampsPresentInRequest && immutableMaxAge != null) {
        rhyme.setResponseMaxAge(immutableMaxAge);
      }
      else if (mutableMaxAge != null) {
        rhyme.setResponseMaxAge(mutableMaxAge);
      }
    }

    return new SpringRhymeLinkBuilder(linkBuilder, timestampParameters);
  }
}
