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

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.FingerprintBuilder;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.UrlFingerprinting;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.aem.api.resources.ImmutableResource;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.queries.AemPageQueries;

@Model(adaptables = { SlingRhyme.class, SlingHttpServletRequest.class }, adapters = UrlFingerprinting.class, cache = true)
public class UrlFingerprintingImpl implements UrlFingerprinting {

  private static final String LAST_MODIFIED = "lastModified";

  @Self
  private AemPageQueries queries;

  @Self
  private SlingRhyme rhyme;

  @QueryParam(name = LAST_MODIFIED)
  private String lastModifiedFromRequest;

  @PostConstruct
  void activate() {

    if (lastModifiedFromRequest != null) {
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

    UriTemplateBuilder builder = UriTemplate.buildFromTemplate(uri);

    Map<String, Object> queryParams = getQueryParamsFromIncomingRequest();
    queryParams.keySet().forEach(builder::query);

    UriTemplate template = builder.build();
    queryParams.forEach(template::set);

    return template.expandPartial();
  }

  public Map<String, Object> getQueryParamsFromIncomingRequest() {

    Map<String, Object> queryParams = new LinkedHashMap<>();

    if (lastModifiedFromRequest != null) {
      queryParams.put(LAST_MODIFIED, lastModifiedFromRequest);
    }

    return queryParams;
  }

  public Map<String, Object> getQueryParams(SlingLinkableResource slingModel) {

    FingerprintBuilderImpl fingerprint = new FingerprintBuilderImpl();

    ((ImmutableResource)slingModel).buildFingerprint(fingerprint);

    Map<String, Object> queryParams = new LinkedHashMap<>();

    fingerprint.getLastModified()
        .ifPresent(lastModified -> queryParams.put(LAST_MODIFIED, lastModified));

    return queryParams;
  }

  class FingerprintBuilderImpl implements FingerprintBuilder {

    private boolean useFingerprintFromIncomingRequest;

    private Instant mostRecentModification;

    Optional<String> getLastModified() {

      if (useFingerprintFromIncomingRequest) {
        return Optional.ofNullable(lastModifiedFromRequest);
      }

      if (mostRecentModification != null) {
        return Optional.of(mostRecentModification.toString());
      }

      return Optional.empty();
    }

    @Override
    public void useFingerprintFromIncomingRequest() {
      useFingerprintFromIncomingRequest = true;
    }

    @Override
    public void addLastModifiedOfPagesBelow(Resource resource) {

      Instant lastModified = queries.getLastModifiedDateBelow(resource.getPath());

      if (mostRecentModification == null || lastModified.isAfter(mostRecentModification)) {
        mostRecentModification = lastModified;
      }
    }
  }


}
