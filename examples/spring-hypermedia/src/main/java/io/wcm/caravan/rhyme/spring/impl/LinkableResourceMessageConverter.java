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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * This {@link HttpMessageConverter} is the essential component that allows to simply return instances of
 * {@link LinkableResource} in a {@link RestController} method. This converter will render those resource
 * using the request-scoped {@link SpringRhymeImpl} bean.
 * @see SpringRhymeImpl#renderResponse(LinkableResource)
 * @see LinkableResourceStatusCodeAdvice
 * @see VndErrorHandlingControllerAdvice
 */
@Component
class LinkableResourceMessageConverter extends AbstractHttpMessageConverter<LinkableResource> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SpringRhymeImpl rhyme;

  LinkableResourceMessageConverter(@Autowired SpringRhymeImpl rhyme) {
    super(MediaTypes.HAL_JSON, MediaTypes.HAL_FORMS_JSON);

    this.rhyme = rhyme;
  }

  @Override
  protected boolean supports(Class<?> clazz) {

    return LinkableResource.class.isAssignableFrom(clazz);
  }

  @Override
  protected LinkableResource readInternal(Class<? extends LinkableResource> clazz, HttpInputMessage inputMessage)
      throws IOException, HttpMessageNotReadableException {

    throw new HttpMessageNotReadableException("Parsing " + LinkableResource.class.getSimpleName() + " instances is not implemented", inputMessage);
  }

  @Override
  protected void writeInternal(LinkableResource resourceImpl, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    ResponseEntity<JsonNode> entity = rhyme.renderResponse(resourceImpl);

    outputMessage.getHeaders().addAll(entity.getHeaders());

    OBJECT_MAPPER.writer()
        .writeValue(outputMessage.getBody(), entity.getBody());
  }
}
