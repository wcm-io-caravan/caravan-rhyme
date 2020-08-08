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
package io.wcm.caravan.reha.api;

import io.wcm.caravan.reha.api.client.JsonResourceLoader;
import io.wcm.caravan.reha.api.common.HalApiAnnotationSupport;
import io.wcm.caravan.reha.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.server.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.reha.impl.RehaBuilderImpl;

public interface RehaBuilder {

  static RehaBuilder withoutResourceLoader() {
    return new RehaBuilderImpl(null);
  }

  static RehaBuilder withResourceLoader(JsonResourceLoader jsonLoader) {
    return new RehaBuilderImpl(jsonLoader);
  }

  RehaBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport);

  RehaBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport);

  RehaBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy);

  Reha buildForRequestTo(String incomingRequestUri);

}
