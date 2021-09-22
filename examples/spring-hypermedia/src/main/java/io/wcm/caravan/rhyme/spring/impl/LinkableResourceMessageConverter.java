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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Component
class LinkableResourceMessageConverter extends AbstractHttpMessageConverter<LinkableResource> {

  private final Logger log = LoggerFactory.getLogger(LinkableResourceMessageConverter.class);

  private final SpringRhymeImpl rhyme;

  public LinkableResourceMessageConverter(@Autowired SpringRhymeImpl rhyme) {
    super(MediaTypes.HAL_JSON, MediaTypes.HAL_FORMS_JSON);
    log.debug("{} was instantiated", this.getClass().getSimpleName());

    this.rhyme = rhyme;
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return LinkableResource.class.isAssignableFrom(clazz);
  }

  @Override
  protected LinkableResource readInternal(Class<? extends LinkableResource> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {

    throw new UnsupportedOperationException("Parsing resources is not implemented");
  }

  @Override
  protected void writeInternal(LinkableResource resourceImpl, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    ResponseEntity<JsonNode> entity = rhyme.renderResponse(resourceImpl);

    outputMessage.getHeaders().addAll(entity.getHeaders());

    String jsonString = entity.getBody().toString();
    outputMessage.getBody().write(jsonString.getBytes(Charsets.UTF_8));
  }
}
