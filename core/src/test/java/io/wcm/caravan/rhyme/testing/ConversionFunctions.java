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
package io.wcm.caravan.rhyme.testing;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;

public final class ConversionFunctions {

  private ConversionFunctions() {
    // this class contains only static methods
  }

  public static HalResponse toJsonResponse(HalResource resource) {

    return new HalResponse()
        .withStatus(200)
        .withBody(resource);
  }
}
