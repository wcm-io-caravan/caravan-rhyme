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

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;

/**
 * This advice renders a vnd.error response for all exceptions that aren't covered yet by the error handling within
 * {@link Rhyme#renderResponse(io.wcm.caravan.rhyme.api.resources.LinkableResource)} (e.g. because the exceptions occur
 * before the resources are being created).
 * @see SpringExceptionStatusAndLoggingStrategy
 * @see VndErrorResponseRenderer
 */
@RestControllerAdvice
class VndErrorHandlingControllerAdvice {

  private final SpringRhymeImpl rhyme;

  VndErrorHandlingControllerAdvice(@Autowired SpringRhymeImpl rhyme) {
    this.rhyme = rhyme;
  }

  private ResponseEntity<JsonNode> renderAsBadRequest(String msg, Throwable ex) {

    return rhyme.renderVndErrorResponse(new HalApiServerException(BAD_REQUEST.value(), msg, ex));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<JsonNode> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {

    // re-throw this exception a new message as the default message MethodArgumentTypeMismatchException does
    // not clearly point out which parameter was invalid
    return renderAsBadRequest("Invalid value for request parameter '" + ex.getName() + "'", ex);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<JsonNode> handleException(Exception ex) {

    return rhyme.renderVndErrorResponse(ex);
  }

}
