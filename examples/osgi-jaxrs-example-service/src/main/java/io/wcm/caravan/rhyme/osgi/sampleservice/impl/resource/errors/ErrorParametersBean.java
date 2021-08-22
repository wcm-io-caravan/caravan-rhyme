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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorParameters;

/**
 * An implementation of the {@link CollectionParameters} interface that
 * contains JAX-RS annotations so it can be used as a {@link BeanParam}
 * argument in a resource method signature
 */
public class ErrorParametersBean implements ErrorParameters {

  @QueryParam("statusCode")
  private Integer statusCode;

  @QueryParam("message")
  @DefaultValue("An exception during resource generation was simulated")
  private String message;

  @QueryParam("wrapException")
  @DefaultValue("false")
  private Boolean wrapException;

  @Override
  public Integer getStatusCode() {
    return statusCode;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Boolean getWrapException() {
    return wrapException;
  }

  static ErrorParametersBean clone(ErrorParameters other) {

    if (other == null) {
      return null;
    }

    ErrorParametersBean cloned = new ErrorParametersBean();

    cloned.statusCode = other.getStatusCode();
    cloned.message = other.getMessage();
    cloned.wrapException = other.getWrapException();

    return cloned;
  }

  public ErrorParametersBean withStatusCode(Integer value) {

    ErrorParametersBean cloned = clone(this);
    cloned.statusCode = value;

    return cloned;
  }

  public ErrorParametersBean withMessage(String value) {

    ErrorParametersBean cloned = clone(this);
    cloned.message = value;

    return cloned;
  }

  public ErrorParametersBean withWrapException(Boolean value) {

    ErrorParametersBean cloned = clone(this);
    cloned.wrapException = value;

    return cloned;
  }
}
