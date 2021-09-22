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
