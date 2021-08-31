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
package io.wcm.caravan.rhyme.impl.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;

/**
 * A class that rewrites all relative link URLs in a {@link HalResponse} to absolute URLs, using protocol, hostname port
 * etc. from the context URL from which that resource was retrieved.
 * If that context resource also wasn't fetched with an absolute URL then the links remain unchanged.
 */
public class LinkRewriting {

  private static final String PATH_PLACEHOLDER = "/lets/assume/this/technically/valid/path/isnt/actually/used/anywhere";

  private static final Logger log = LoggerFactory.getLogger(LinkRewriting.class);

  private URI contextUri;

  /**
   * @param contextUrl the URI from which the resource to be processed was retrieved
   */
  LinkRewriting(String contextUrl) {
    try {
      this.contextUri = new URI(contextUrl);
    }
    catch (URISyntaxException ex) {
      log.warn("Failed to parse the context URI {}, so any links from that resource cannot be rewritten", contextUrl);
    }
  }

  /**
   * Rewrites all relative link URLs in the given to become absolute URLs, using protocal, hostname port
   * etc. from the context URL given in the constructor.
   * If that context URL isn't absolute, then the links remain unchanged.
   * @param response from which the links in the body will be rewritten
   * @return a new instance with rewritten links
   */
  HalResponse resolveRelativeLinks(HalResponse response) {

    if (contextUri == null || !contextUri.isAbsolute()) {
      return response;
    }

    HalResource body = response.getBody();

    rewriteLinksRecursively(body);

    return response.withBody(body);
  }

  private void rewriteLinksRecursively(HalResource resource) {

    resource.getLinks().values().forEach(this::rewriteLink);

    resource.getEmbedded().values().forEach(this::rewriteLinksRecursively);
  }

  void rewriteLink(Link link) {

    String href = link.getHref();

    if (href.startsWith("/") || href.startsWith("{")) {

      // URI templates cannot be parsed by the URI class, so we need a workaround for them
      String newHref = link.isTemplated() ? resolvePathTemplate(href) : resolvePath(href);

      link.setHref(newHref);
    }
  }

  private String resolvePath(String path) {

    URI linkTarget;
    try {
      linkTarget = new URI(path);
    }
    catch (URISyntaxException ex) {
      log.warn("Failed to parse the URI {} found in a link within the resource at {}", path, contextUri);
      return path;
    }

    URI uri = contextUri.resolve(linkTarget);

    return uri.toString();
  }

  private String resolvePathTemplate(String pathTemplate) {

    URI uriWithPlaceholder = contextUri.resolve(PATH_PLACEHOLDER);

    String resolvedTemplate = uriWithPlaceholder.toString()
        .replace(PATH_PLACEHOLDER, pathTemplate);

    return resolvedTemplate;
  }
}
