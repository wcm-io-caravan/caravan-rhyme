/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.rhyme.jaxrs.api;

import javax.ws.rs.core.Application;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides information about the OSGi bundle version and the base-path of the {@link Application} contained in the
 * bundle
 */
@ProviderType
public interface JaxRsBundleInfo {

  /**
   * @return the base-path of the JAX-RS {@link Application} that is contained in the bundle where this service is used
   */
  String getApplicationPath();

  /**
   * @return the version of the bundle where this service is used
   */
  String getBundleVersion();
}
