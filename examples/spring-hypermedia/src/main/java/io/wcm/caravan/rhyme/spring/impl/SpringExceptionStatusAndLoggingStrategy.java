/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.rhyme.spring.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;


class SpringExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

  @Override
  public Integer extractStatusCode(Throwable error) {
    if (error instanceof ResponseStatusException) {
      return ((ResponseStatusException)error).getStatus().value();
    }
    if (error instanceof MissingServletRequestParameterException) {
      return HttpStatus.BAD_REQUEST.value();
    }
    if (error instanceof NoHandlerFoundException) {
      return HttpStatus.NOT_FOUND.value();
    }
    if (error.getClass().isAnnotationPresent(ResponseStatus.class)) {
      return error.getClass().getAnnotation(ResponseStatus.class).code().value();
    }
    return null;
  }

  @Override
  public String getErrorMessageWithoutRedundantInformation(Throwable error) {
    return StringUtils.substringBefore(error.getMessage(), "; nested exception is");
  }

}
