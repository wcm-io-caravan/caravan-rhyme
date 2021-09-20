/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.rhyme.spring.impl;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;

@RestControllerAdvice
class VndErrorHandlingControllerAdvice {

  @Autowired
  private SpringRhymeImpl rhyme;

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
