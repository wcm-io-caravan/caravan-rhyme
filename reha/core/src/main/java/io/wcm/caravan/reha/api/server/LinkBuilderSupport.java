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
package io.wcm.caravan.reha.api.server;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.reha.api.resources.LinkableResource;

/**
 * Extension point interface for the {@link LinkBuilder} that allows to
 * delegate the framework-specific logic of extracting all information to build a valid link
 * (template) to a given server-side resource instance
 */
@ConsumerType
public interface LinkBuilderSupport {

  /**
   * @param targetResource the server-side resource instance to be linked
   * @return the path template of the resource (relative to the service's base URL)
   */
  String getResourcePathTemplate(LinkableResource targetResource);

  /**
   * @param targetResource the server-side resource instance to be linked
   * @return a map with the names and values of the variables defined in the path template
   */
  Map<String, Object> getPathParameters(LinkableResource targetResource);

  /**
   * @param targetResource the server-side resource instance to be linked
   * @return a map with the names and values of all query parameters to be added
   */
  Map<String, Object> getQueryParameters(LinkableResource targetResource);
}
