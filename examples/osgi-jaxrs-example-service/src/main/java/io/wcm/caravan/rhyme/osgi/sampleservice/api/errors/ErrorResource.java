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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.errors;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;

/**
 * This is used as the target resource type by the error handling examples,
 * but you'll never see a resource of this type being successfully rendered, because the examples
 * are implemented to always throw an exception which is rendered as a vnd.error resource
 */
@HalApiInterface
public interface ErrorResource {

  /**
   * @return an optional title (to be displayed in the HAL browser when this resource is embedded)
   */
  @ResourceProperty
  Maybe<String> getTitle();

}
