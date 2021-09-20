/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.rhyme.spring.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@RestControllerAdvice
class LinkableResourceStatusCodeAdvice implements ResponseBodyAdvice<LinkableResource> {

  @Autowired
  private SpringRhymeImpl rhyme;

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return LinkableResourceMessageConverter.class.isAssignableFrom(converterType);
  }

  @Override
  public LinkableResource beforeBodyWrite(LinkableResource body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

    ResponseEntity<JsonNode> entity = rhyme.renderResponse(body);

    response.setStatusCode(entity.getStatusCode());

    return body;
  }
}
