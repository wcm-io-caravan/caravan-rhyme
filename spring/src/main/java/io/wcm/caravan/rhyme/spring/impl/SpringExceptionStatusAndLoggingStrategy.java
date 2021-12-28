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

import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;

/**
 * This strategy will be passed to {@link RhymeBuilder#withExceptionStrategy(ExceptionStatusAndLoggingStrategy)}
 * so that the {@link VndErrorResponseRenderer} from the core framework knows how to handle some spring-specific
 * exceptions that happen while rendering the resource.
 * see {@link VndErrorHandlingControllerAdvice}
 */
class SpringExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

  @Override
  public Integer extractStatusCode(Throwable error) {

    if (error instanceof ResponseStatusException) {
      return ((ResponseStatusException)error).getStatus().value();
    }

    if (error.getClass().isAnnotationPresent(ResponseStatus.class)) {

      ResponseStatus annotation = error.getClass().getAnnotation(ResponseStatus.class);

      HttpStatus fromCodeAttribute = annotation.code();
      if (fromCodeAttribute != HttpStatus.INTERNAL_SERVER_ERROR) {
        return fromCodeAttribute.value();
      }

      return annotation.value().value();
    }

    if (error instanceof MissingServletRequestParameterException) {
      return HttpStatus.BAD_REQUEST.value();
    }

    if (error instanceof NoHandlerFoundException) {
      return HttpStatus.NOT_FOUND.value();
    }

    return null;
  }

  @Override
  public String getErrorMessageWithoutRedundantInformation(Throwable error) {

    // we don't want to log the repeated messages from the cause that appear in some Spring exceptions
    return StringUtils.substringBefore(error.getMessage(), "; nested exception is");
  }

}
