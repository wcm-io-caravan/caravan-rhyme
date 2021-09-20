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
public class SpringRhymeImpl implements SpringRhyme {

  private static final Logger log = LoggerFactory.getLogger(SpringRhymeImpl.class);

  private static final SpringExceptionStatusAndLoggingStrategy EXCEPTION_STRATEGY = new SpringExceptionStatusAndLoggingStrategy();

  private final Rhyme rhyme;

  private ResponseEntity<JsonNode> renderedResponse;

  public SpringRhymeImpl(@Autowired HttpServletRequest httpRequest,
      @Autowired(required = false) HalResourceLoader jsonLoader) {

    log.debug("{} was instantiated", this.getClass().getSimpleName());

    String requestUrl = getRequestUrl(httpRequest);

    this.rhyme = RhymeBuilder
        .withResourceLoader(jsonLoader)
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

  ResponseEntity<JsonNode> renderVndErrorResponse(Throwable ex) {

    HalResponse response = rhyme.renderVndErrorResponse(ex);

    return createResponseEntity(response);
  }

  ResponseEntity<JsonNode> renderResponse(LinkableResource resourceImpl) {

    if (renderedResponse != null) {
      return renderedResponse;
    }

    HalResponse halResponse = rhyme.renderResponse(resourceImpl).blockingGet();

    renderedResponse = createResponseEntity(halResponse);

    return renderedResponse;
  }

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
