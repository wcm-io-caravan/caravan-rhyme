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
package io.wcm.caravan.rhyme.impl.renderer;

import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.findHalApiInterface;
import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.getClassAndMethodName;
import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.getSimpleClassName;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.rhyme.impl.renderer.RelatedResourcesRendererImpl.RelationRenderResult;

/**
 * Full implementation of {@link AsyncHalResourceRenderer} that will collect detailed performance information
 * while rendering the {@link HalResource}
 */
public final class AsyncHalResourceRendererImpl implements AsyncHalResourceRenderer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RelatedResourcesRendererImpl relatedRenderer;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;

  /**
   * Create a new renderer to use (only) for the current incoming request
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param typeSupport the strategy to detect HAL API annotations and perform type conversions
   */
  public AsyncHalResourceRendererImpl(RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {
    this.relatedRenderer = new RelatedResourcesRendererImpl(this::renderLinkedOrEmbeddedResource, metrics, typeSupport);
    this.metrics = metrics;
    this.typeSupport = typeSupport;
  }

  @Override
  public Single<HalResource> renderResource(LinkableResource resourceImpl) {

    return renderLinkedOrEmbeddedResource(resourceImpl);
  }

  Single<HalResource> renderLinkedOrEmbeddedResource(Object resourceImplInstance) {

    Stopwatch assemblyTime = Stopwatch.createStarted();

    Preconditions.checkNotNull(resourceImplInstance, "Cannot create a HalResource from a null reference");

    // find the interface annotated with @HalApiInterface
    Class<?> apiInterface = findHalApiInterface(resourceImplInstance, typeSupport);

    // get the JSON resource state from the method annotated with @ResourceState
    Single<ObjectNode> rxState = renderResourceState(apiInterface, resourceImplInstance);

    // render links and embedded resources for each method annotated with @RelatedResource
    Single<List<RelationRenderResult>> rxRelated = relatedRenderer.renderRelated(apiInterface, resourceImplInstance);

    String simpleClassName = getSimpleClassName(resourceImplInstance, typeSupport);
    // wait until all this is available...
    Single<HalResource> rxHalResource = Single.zip(rxState, rxRelated,
        // ...and then create the HalResource instance
        (stateNode, listOfRelated) -> createHalResource(resourceImplInstance, stateNode, listOfRelated))
        // and measure the time of the emissions
        .compose(EmissionStopwatch.collectMetrics("rendering " + simpleClassName + " instances", metrics));

    metrics.onMethodInvocationFinished(AsyncHalResourceRenderer.class,
        "calling #renderLinkedOrEmbeddedResource with " + simpleClassName,
        assemblyTime.elapsed(TimeUnit.MICROSECONDS));

    return rxHalResource;
  }

  Single<ObjectNode> renderResourceState(Class<?> apiInterface, Object resourceImplInstance) {

    Single<ObjectNode> emptyObject = Single.fromCallable(() -> JsonNodeFactory.instance.objectNode());

    // find the first method annotated with @ResourceState (and return an empty object if there is none)
    Optional<Method> method = HalApiReflectionUtils.findResourceStateMethod(apiInterface, typeSupport);
    if (!method.isPresent()) {
      return emptyObject;
    }

    // invoke the method to get the state observable
    return RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImplInstance, method.get(), metrics, typeSupport)
        // convert the emitted state instance to a JSON object node
        .map(object -> OBJECT_MAPPER.convertValue(object, ObjectNode.class))
        // or use an empty object if the method returned an empty Maybe or Observable
        .singleElement()
        .switchIfEmpty(emptyObject)
        // and measure the total time of the emissions
        .compose(
            EmissionStopwatch.collectMetrics("rendering state emited by " + getClassAndMethodName(resourceImplInstance, method.get(), typeSupport), metrics));
  }

  HalResource createHalResource(Object resourceImplInstance, ObjectNode stateNode, List<RelationRenderResult> listOfRelated) {

    HalResource hal = new HalResource(stateNode);

    if (resourceImplInstance instanceof LinkableResource) {
      Stopwatch sw = Stopwatch.createStarted();
      Link selfLink = ((LinkableResource)resourceImplInstance).createLink();

      metrics.onMethodInvocationFinished(AsyncHalResourceRenderer.class,
          "calling #createLink of " + getSimpleClassName(resourceImplInstance, typeSupport),
          sw.elapsed(TimeUnit.MICROSECONDS));

      hal.setLink(selfLink);
    }

    for (RelationRenderResult related : listOfRelated) {
      String relation = related.getRelation();

      if (related.isMultiValue()) {
        hal.addLinks(relation, related.getLinks());
        hal.addEmbedded(relation, related.getEmbedded());
      }
      else {
        if (!related.getLinks().isEmpty()) {
          hal.setLink(relation, related.getLinks().get(0));
        }
        if (!related.getEmbedded().isEmpty()) {
          hal.setEmbedded(relation, related.getEmbedded().get(0));
        }
      }
    }

    return hal;
  }

  public HalApiTypeSupport getTypeSupport() {
    return typeSupport;
  }
}
