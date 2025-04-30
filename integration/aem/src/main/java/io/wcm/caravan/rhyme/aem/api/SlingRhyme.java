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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;

import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

/**
 * An AEM-specific wrapper for the general {@link Rhyme} facade interface.
 * <p>
 * It is implemented as a sling model that is initially adapted from the incoming request by
 * the integration's central {@link HalApiServlet} and will internally create a single {@link Rhyme} instance
 * that will be used for all interaction with the core framework for the duration of the request.
 * </p>
 * <p>
 * Your should implement your Rhyme resources as a
 * <a href="https://sling.apache.org/documentation/bundles/models.html">Sling Model</a> class that adapts from
 * {@link SlingRhyme} to your interface annotated with {@link HalApiInterface}, e.g.
 * </p>
 * <code>
 * &#64;Model(adaptables = SlingRhyme.class, adapters = YourHalApiInterface.class)
 * </code>
 * <p>
 * The {@link SlingRhyme} instance can be adapted to {@link SlingHttpServletRequest} and {@link Resource}, but also
 * to any other types (e.g. sling models, or AEM {@link Page}) that are adaptable from those types. This allows you to
 * to use the {@link Self} annotation to inject any any of those types into your model,
 * and they will be resolved with the context of the resource that is currently being processed.
 * </p>
 * <p>
 * This context resource will initially be set to the resource that was originally requested. You can use
 * {@link #adaptResource(Resource, Class)} to create other sling models for related
 * resources that you want to render with Rhyme. When calling that method, a new (lightweight) {@link SlingRhyme}
 * instance will be created that is using the new given resource as a context for resource-based injection (but is still
 * backed by the original {@link Rhyme} instance and the incoming request).
 * </p>
 * @see SlingResourceAdapter
 * @see AbstractLinkableResource
 * @see RhymeResourceRegistration
 */
public interface SlingRhyme extends Adaptable {

  /**
   * @return the current context resource being processed
   */
  @NotNull
  Resource getCurrentResource();

  /**
   * @return the resource that was originally requested in the incoming HTTP request
   */
  @NotNull
  Resource getRequestedResource();

  /**
   * Creates a new sling model instance for the given resource. Internally this will create a new {@link SlingRhyme}
   * instance with the new resource context from which the model class will be adapted.
   * @param resource from which the model will be adapted
   * @param slingModelClass that is adaptable from {@link SlingRhyme}
   * @param <T> the sling model class
   * @return a new sling model instance for which all fields have been initialized
   * @throws HalApiDeveloperException if the resource could not be adapted
   */
  <T> @NotNull T adaptResource(Resource resource, Class<T> slingModelClass);

  /**
   * Limit the maximum time for which the response should be cached by clients and downstream services. Note that
   * calling this method only sets the upper limit: if other upstream resource fetched during the current request
   * indicate a lower max-age value in their header, that lower value will be used instead.
   * @param duration the max cache time
   */
  void setResponseMaxAge(Duration duration);

  /**
   * Create a dynamic client proxy to load HAL+JSON resources from an upstream service.
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param uri the URI of the entry point, in any format that the {@link HalResourceLoader} being used can understand
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided {@link HalApiInterface} that you can use to navigate through the
   *         resources of the service
   */
  <T> @NotNull T getRemoteResource(String uri, Class<T> halApiInterface);

  /**
   * Provides access to the underlying {@link Rhyme} instance of the core framework, just in case you need
   * to call one of the methods for which there doesn't exist a delegate in the {@link SlingRhyme} interface
   * @return the single {@link Rhyme} instance that is used throughout the incoming request
   */
  Rhyme getCoreRhyme();

}
