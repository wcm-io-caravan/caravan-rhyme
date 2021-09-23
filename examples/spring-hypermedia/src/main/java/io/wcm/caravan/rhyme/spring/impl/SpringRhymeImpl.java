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
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

@Component
@RequestScope
class SpringRhymeImpl implements SpringRhyme {

  private static final Logger log = LoggerFactory.getLogger(SpringRhymeImpl.class);

  private static final SpringExceptionStatusAndLoggingStrategy EXCEPTION_STRATEGY = new SpringExceptionStatusAndLoggingStrategy();

  private final Rhyme rhyme;

  private ResponseEntity<JsonNode> renderedResponse;

  SpringRhymeImpl(@Autowired HttpServletRequest httpRequest,
      @Autowired(required = false) HalResourceLoader resourceLoader,
      @Autowired SpringRhymeDocsIntegration rhymeDocs) {

    log.debug("{} was instantiated", this.getClass().getSimpleName());

    String requestUrl = getRequestUrl(httpRequest);

    this.rhyme = RhymeBuilder
        .withResourceLoader(resourceLoader)
        .withRhymeDocsSupport(rhymeDocs)
        .withExceptionStrategy(EXCEPTION_STRATEGY)
        .buildForRequestTo(requestUrl);
  }

  private static String getRequestUrl(HttpServletRequest httpRequest) {

    StringBuffer requestUrl = httpRequest.getRequestURL();

    if (httpRequest.getQueryString() != null) {
      requestUrl.append("?");
      requestUrl.append(httpRequest.getQueryString());
    }

    return requestUrl.toString();
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {

    return rhyme.getRemoteResource(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {

    rhyme.setResponseMaxAge(duration);
  }

  @Override
  public Rhyme getCoreRhyme() {

    return rhyme;
  }

  ResponseEntity<JsonNode> renderVndErrorResponse(Throwable ex) {

    HalResponse response = rhyme.renderVndErrorResponse(ex);

    return createResponseEntity(response);
  }

  ResponseEntity<JsonNode> renderResponse(LinkableResource resourceImpl) {

    // LinkableResourceMessageConverter and LinkableResourceStatusCodeAdvice are both calling this method
    // for the same request, as they need access to the rendered response entity.
    // We are assuming here that no one else is calling this method (with a different resource) and
    // make sure that the second call just gets the same instance as the first call
    if (renderedResponse != null) {
      return renderedResponse;
    }

    HalResponse halResponse = rhyme.renderResponse(resourceImpl).blockingGet();

    renderedResponse = createResponseEntity(halResponse);

    return renderedResponse;
  }

  /**
   * Convert the response rendered by the core Rhyme framework into a Spring {@link ResponseEntity}
   * @param halResponse a {@link HalResponse}
   * @return a {@link ResponseEntity} with status, contentType and cache-control header set
   */
  private ResponseEntity<JsonNode> createResponseEntity(HalResponse halResponse) {

    BodyBuilder builder = ResponseEntity.status(halResponse.getStatus());

    if (halResponse.getContentType() != null) {
      builder.contentType(MediaType.parseMediaType(halResponse.getContentType()));
    }

    if (halResponse.getMaxAge() != null) {
      builder.cacheControl(CacheControl.maxAge(halResponse.getMaxAge(), TimeUnit.SECONDS));
    }

    return builder.body(halResponse.getBody().getModel());
  }

}
