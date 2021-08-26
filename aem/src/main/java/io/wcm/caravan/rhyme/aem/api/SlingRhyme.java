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
package io.wcm.caravan.rhyme.aem.api;

import java.time.Duration;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * This interface is an AEM-specific wrapper for the general {@link Rhyme} facade interface.
 * <p>
 * It is implemented as a sling model that is initially adapted from the incoming request to
 * the framework's central {@link HalApiServlet} and will internally create a single {@link Rhyme} instance
 * that will be used for all interaction with the core Rhyme framework for the duration of the request.
 * </p>
 * <p>
 * You can inject a {@link SlingRhyme} instance into the Sling model that implements a {@link HalApiInterface}
 * by using the {@link RhymeObject} annotation on a private field of your class. If you are implementing a
 * {@link LinkableResource}, the easiest way to start is to extend the {@link AbstractLinkableResource} super class.
 * </p>
 * <p>
 * Note that that there will be a new leightweight {@link SlingRhyme} instance created every time you are using the
 * {@link #adaptResource(Resource, Class)} (directly or via {@link SlingResourceAdapter}) to create
 * a new sling model instance from a resource.
 * This ensures that all rhyme helpers injected to the new sling model with the {@link RhymeObject} annotation
 * know the "current resource" from which the model was adapted. But more importantly, some context information that
 * would get lost with a regular call to {@link Resource#adaptTo(Class)} is preserved (e.g.
 * the resource that was originally requested, the request itself and the underlying {@link Rhyme} object that
 * collects information throughout the current request).
 * </p>
 */
public interface SlingRhyme extends Adaptable {

  /**
   * @return the current resource being processed
   */
  @NotNull
  Resource getCurrentResource();

  /**
   * @return the resource that was originally requested in the incoming HTTP request
   */
  @NotNull
  Resource getRequestedResource();

  /**
   * @param <T>
   * @param resource from which the model will be adapted
   * @param slingModelClass that is adaptable from {@link Resource}
   * @return a new sling model instance for which all fields have been initialized
   * @throws HalApiDeveloperException if the resource could not be adapted
   */
  <T> @NotNull T adaptResource(Resource resource, Class<T> slingModelClass);

  void setResponseMaxAge(Duration duration);

  <T> @NotNull T getRemoteResource(String uri, Class<T> halApiInterface);

}
