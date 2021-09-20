/* Copyright (c) pro!vision GmbH. All rights reserved. */
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
    super(MediaTypes.HAL_JSON);
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
