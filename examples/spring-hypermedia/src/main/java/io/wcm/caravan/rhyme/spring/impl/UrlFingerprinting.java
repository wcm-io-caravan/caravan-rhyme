package io.wcm.caravan.rhyme.spring.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

public final class UrlFingerprinting {

  private final HttpServletRequest request;
  private final SpringRhyme rhyme;

  private Duration mutableMaxAge;
  private Duration immutableMaxAge;

  private final Map<String, String> timestampParameters = new LinkedHashMap<>();
  private boolean allTimestampsPresentInRequest = true;

  UrlFingerprinting(HttpServletRequest request, SpringRhyme rhyme) {
    this.request = request;
    this.rhyme = rhyme;
  }

  public UrlFingerprinting withMutableMaxAge(Duration value) {
    mutableMaxAge = value;
    return this;
  }

  public UrlFingerprinting withImmutableMaxAge(Duration value) {
    immutableMaxAge = value;
    return this;
  }

  public UrlFingerprinting withTimestampParameter(String name, Supplier<Instant> source) {

    String timestampFromRequest = request.getParameter(name);

    if (StringUtils.isNotBlank(timestampFromRequest)) {
      timestampParameters.put(name, timestampFromRequest);
    }
    else {
      timestampParameters.put(name, source.get().toString());
      allTimestampsPresentInRequest = false;
    }

    return this;
  }

  public SpringRhymeLinkBuilder createLinkWith(WebMvcLinkBuilder linkBuilder) {

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

  public boolean isFingerprintPresentInRequest() {
    return allTimestampsPresentInRequest;
  }
}
