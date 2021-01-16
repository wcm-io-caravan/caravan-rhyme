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
package io.wcm.caravan.rhyme.api.spi;

import java.lang.reflect.Method;

/**
 * An SPI interface that allows the core framework to support additional annotations in your HAL-API interfaces. Using
 * this extension point is only required if you are using interfaces with legacy annotations.
 */
public interface HalApiAnnotationSupport {

  /**
   * @param type the type to check
   * @return true if this type is annotated to be a HAL APi interface
   */
  boolean isHalApiInterface(Class<?> type);

  /**
   * @param halApiInterface annotated interface
   * @return the content type of the resource represented by the interface
   */
  String getContentType(Class<?> halApiInterface);

  /**
   * @param method from a HAL API interface
   * @return true if the method returns a link to the resource
   */
  boolean isResourceLinkMethod(Method method);

  /**
   * @param method from a HAL API interface
   * @return true if the method returns the resource representation
   */
  boolean isResourceRepresentationMethod(Method method);

  /**
   * @param method from a HAL API interface
   * @return true if the method returns a linked or embedded resource, or a collection thereof
   */
  boolean isRelatedResourceMethod(Method method);

  /**
   * @param method from a HAL API interface
   * @return true if the method returns the resource state
   */
  boolean isResourceStateMethod(Method method);

  /**
   * @param method for which {@link #isResourceLinkMethod(Method)} returns true
   * @return the relation to be used for links and embedded resources returned by the method
   */
  String getRelation(Method method);

}
