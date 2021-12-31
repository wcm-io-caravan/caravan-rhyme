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

/**
 * This {@link ResponseBodyAdvice} is required to set the status code for non-successful responses,
 * which isn't possible anymore in the related {@link LinkableResourceMessageConverter}
 */
@RestControllerAdvice
class LinkableResourceStatusCodeAdvice implements ResponseBodyAdvice<LinkableResource> {

  private final SpringRhymeImpl rhyme;

  LinkableResourceStatusCodeAdvice(@Autowired SpringRhymeImpl rhyme) {
    this.rhyme = rhyme;
  }

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
