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
package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.FingerprintBuilder;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.UrlFingerprinting;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.aem.api.resources.ImmutableResource;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.queries.AemPageQueries;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@Model(adaptables = { SlingRhyme.class, SlingHttpServletRequest.class }, adapters = UrlFingerprinting.class, cache = true)
public class UrlFingerprintingImpl implements UrlFingerprinting {

  public static final String TIMESTAMP = "timestamp";

  @Self
  private AemPageQueries queries;

  @Self
  private SlingRhyme rhyme;

  @QueryParam(name = TIMESTAMP)
  private String timestampFromRequest;

  @PostConstruct
  void activate() {

    if (timestampFromRequest != null) {
      // if a lastModified query param was provided in the incoming request, this resource is considered to be immutable
      // (unless it is loading other resources with a lower max-age value)
      rhyme.setResponseMaxAge(Duration.ofDays(365));
    }
    else {
      // if no such param was present, the response should only be cached for a short duration...
      rhyme.setResponseMaxAge(Duration.ofSeconds(42));
    }
  }

  @Override
  public String appendIncomingFingerprintTo(String uri) {

    Map<String, Object> queryParams = getQueryParamsFromIncomingRequest();

    String queryString = queryParams.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("&"));

    if (StringUtils.isBlank(queryString)) {
      return uri;
    }
    if (uri.contains("?")) {
      return uri + "&" + queryString;
    }
    return uri + "?" + queryString;
  }

  public Map<String, Object> getQueryParamsFromIncomingRequest() {

    Map<String, Object> queryParams = new LinkedHashMap<>();

    if (timestampFromRequest != null) {
      queryParams.put(TIMESTAMP, timestampFromRequest);
    }

    return queryParams;
  }

  public Map<String, Object> getQueryParams(SlingLinkableResource slingModel) {

    FingerprintBuilderImpl fingerprint = new FingerprintBuilderImpl(slingModel.getClass());

    ((ImmutableResource)slingModel).buildFingerprint(fingerprint);

    Map<String, Object> queryParams = new LinkedHashMap<>();

    fingerprint.getTimestamp()
        .ifPresent(timestamp -> queryParams.put(TIMESTAMP, timestamp));

    return queryParams;
  }

  class FingerprintBuilderImpl implements FingerprintBuilder {

    private final Class<? extends SlingLinkableResource> modelClass;

    private boolean useFingerprintFromIncomingRequest;

    private Instant mostRecentModification;

    FingerprintBuilderImpl(Class<? extends SlingLinkableResource> modelClass) {
      this.modelClass = modelClass;
    }

    Optional<String> getTimestamp() {

      if (useFingerprintFromIncomingRequest) {
        return Optional.ofNullable(timestampFromRequest);
      }

      if (mostRecentModification != null) {
        return Optional.of(mostRecentModification.toString());
      }

      throw new HalApiDeveloperException(
          "Your implementation of #buildFingerprint() in " + modelClass + " must call at least one of the methods from the builder");
    }

    @Override
    public void useFingerprintFromIncomingRequest() {
      useFingerprintFromIncomingRequest = true;
    }

    @Override
    public void addLastModifiedOfContentBelow(String path) {

      Instant lastModified = queries.getLastModifiedDateBelow(path);

      if (mostRecentModification == null || lastModified.isAfter(mostRecentModification)) {
        mostRecentModification = lastModified;
      }
    }
  }


}
