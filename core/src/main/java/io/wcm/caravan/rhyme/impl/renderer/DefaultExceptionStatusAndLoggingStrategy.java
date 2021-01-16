/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.rhyme.impl.renderer;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;

final class DefaultExceptionStatusAndLoggingStrategy implements ExceptionStatusAndLoggingStrategy {

  @Override
  public Integer extractStatusCode(Throwable error) {

    if (error instanceof HalApiClientException) {
      return ((HalApiClientException)error).getStatusCode();
    }
    if (error instanceof HalApiServerException) {
      return ((HalApiServerException)error).getStatusCode();
    }

    return null;
  }

  @Override
  public boolean logAsCompactWarning(Throwable error) {

    if (error instanceof HalApiClientException) {
      return true;
    }

    return false;
  }

  public ExceptionStatusAndLoggingStrategy decorateWith(ExceptionStatusAndLoggingStrategy customStrategy) {

    if (customStrategy == null) {
      return this;
    }

    return new CompositeExceptionStatusAndLoggingStrategy(ImmutableList.of(customStrategy, this));
  }
}
