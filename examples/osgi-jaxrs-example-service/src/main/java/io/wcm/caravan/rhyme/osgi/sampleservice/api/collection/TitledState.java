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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.collection;

import io.wcm.caravan.rhyme.api.annotations.ResourceState;

/**
 * To be used with {@link ResourceState} if you only want to specify a "title" property
 * (which is a nice way to identify embedded resources in the HAL browser)
 */
public class TitledState {

  private String title;

  /**
   * @return a title to be displayed in the HAL browser
   */
  public String getTitle() {
    return this.title;
  }

  public TitledState withTitle(String title) {
    this.title = title;
    return this;
  }
}
