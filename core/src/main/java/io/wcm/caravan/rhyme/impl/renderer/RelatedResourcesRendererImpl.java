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

import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.getClassAndMethodName;
import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.getSortedRelatedResourceMethods;
import static io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils.invokeMethodAndReturnObservable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;

final class RelatedResourcesRendererImpl {

  private final Function<Object, Single<HalResource>> recursiveRenderFunc;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;

  RelatedResourcesRendererImpl(Function<Object, Single<HalResource>> recursiveRenderFunc, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {
    this.recursiveRenderFunc = recursiveRenderFunc;
    this.metrics = metrics;
    this.typeSupport = typeSupport;
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface}
   * @param resourceImplInstance the context resource for which the related resources should be discovered and rendered
   * @return a {@link Single} that emits a list with one {@link RelationRenderResult} instance for each method annotated
   *         with
   *         {@link Related}
   */
  Single<List<RelationRenderResult>> renderRelated(Class<?> apiInterface, Object resourceImplInstance) {

    // find all methods annotated with @RelatedResource
    List<Method> methods = getSortedRelatedResourceMethods(apiInterface, typeSupport);

    return Observable.fromIterable(methods)
        // create a RelatedContent instance with the links and embedded resources returned by each method
        .concatMapEager(method -> createRelatedContentForMethod(resourceImplInstance, method).toObservable())
        // and collect the results for each method in a single list
        .toList();
  }

  private Single<RelationRenderResult> createRelatedContentForMethod(Object resourceImplInstance, Method method) {

    verifyReturnType(resourceImplInstance, method);
    String relation = typeSupport.getRelation(method);
    boolean multiValue = typeSupport.isProviderOfMultiplerValues(method.getReturnType());

    // call the implementation of the method to get an observable of related resource implementation instances
    Observable<?> rxRelatedResources = invokeMethodAndReturnObservable(resourceImplInstance, method, metrics, typeSupport)
        .cache();

    // create links for those resources that implement LinkableResource
    Single<List<Link>> rxLinks = createLinksTo(rxRelatedResources);

    // and (asynchronously) render those resources that should be embedded
    Single<List<HalResource>> rxEmbeddedHalResources = renderEmbeddedResources(method, rxRelatedResources);

    // collect all resource impl classes that cannot be rendered (because they don't extend either of EmbeddableResource and LinkableResource)
    Single<List<String>> rxUnsupportedClassNames = findUnsupportedClassNames(rxRelatedResources);

    // wait for all this to be complete before creating a RelatedResourceInfo for this method
    Single<RelationRenderResult> renderResult = Single.zip(rxLinks, rxEmbeddedHalResources, rxUnsupportedClassNames,
        (links, embeddedResources, unsupportedClassNames) -> {

          if (!unsupportedClassNames.isEmpty()) {
            throw new HalApiDeveloperException("Your server side resource implementation classes must implement either "
                + EmbeddableResource.class.getSimpleName() + " or " + LinkableResource.class.getSimpleName() + ". "
                + " This is not the case for " + unsupportedClassNames + ", which is linked from "
                + resourceImplInstance.getClass() + " with relation " + relation);
          }

          return new RelationRenderResult(relation, links, embeddedResources, multiValue);
        });

    Class<?> emissionType = RxJavaReflectionUtils.getObservableEmissionType(method, typeSupport);

    // and measure the time of the emissions
    return renderResult
        .compose(EmissionStopwatch
            .collectMetrics(() -> "processing of related " + emissionType.getSimpleName() + " instances returned by "
                + getClassAndMethodName(resourceImplInstance, method, typeSupport), metrics));
  }

  private Single<List<String>> findUnsupportedClassNames(Observable<?> rxRelatedResources) {

    return rxRelatedResources
        .filter(res -> !(res instanceof LinkableResource))
        .filter(res -> !(res instanceof EmbeddableResource))
        .filter(res -> !HalApiReflectionUtils.isPlainLink(res.getClass()))
        .map(res -> HalApiReflectionUtils.getSimpleClassName(res, typeSupport))
        .distinct()
        .toList();
  }

  private void verifyReturnType(Object resourceImplInstance, Method method) {

    // get the emitted result resource type from the method signature
    Class<?> relatedResourceInterface = RxJavaReflectionUtils.getObservableEmissionType(method, typeSupport);

    if (!HalApiReflectionUtils.isHalApiInterface(relatedResourceInterface, typeSupport)
        && !HalApiReflectionUtils.isPlainLink(relatedResourceInterface)) {

      String returnTypeDesc = getReturnTypeDescription(method, relatedResourceInterface);

      String fullMethodName = getClassAndMethodName(resourceImplInstance, method, typeSupport);
      throw new HalApiDeveloperException("The method " + fullMethodName + " returns " + returnTypeDesc + ", "
          + "but it must return a Link or an interface annotated with the @" + HalApiInterface.class.getSimpleName()
          + " annotation (or a supported generic type that provides such instances, e.g. Observable)");
    }
  }

  private static String getReturnTypeDescription(Method method, Class<?> relatedResourceInterface) {
    Class<?> returnType = method.getReturnType();
    String returnTypeDesc = relatedResourceInterface.getSimpleName();
    if (!returnType.equals(relatedResourceInterface)) {
      return returnType.getSimpleName() + "<" + returnTypeDesc + ">";
    }
    return returnTypeDesc;
  }

  private Single<List<Link>> createLinksTo(Observable<?> rxRelatedResources) {

    // filter only those resources that are implementing LinkableResource
    Observable<LinkableResource> rxLinkedResourceImpls = rxRelatedResources
        .filter(r -> r instanceof LinkableResource)
        // decide whether to write links to resource that are also embedded
        .filter(this::filterLinksToEmbeddedResource)
        .map(LinkableResource.class::cast);

    // and let each resource create a link to itself
    Observable<Link> rxCreatedLinks = rxLinkedResourceImpls
        .map(linkedResource -> {

          try (RequestMetricsStopwatch sw = metrics.startStopwatch(AsyncHalResponseRenderer.class,
              () -> "calls to #createLink of " + getSimpleClassName(linkedResource))) {

            Link link = linkedResource.createLink();

            if (link == null) {
              throw new HalApiDeveloperException(getSimpleClassName(linkedResource) + " returned a null value");
            }
            return link;
          }
        });

    // Related methods also can return links directly instead
    Observable<Link> rxDirectLinks = rxRelatedResources
        .filter(r -> r instanceof Link)
        .map(Link.class::cast);

    return Observable.concat(rxCreatedLinks, rxDirectLinks)
        .toList();
  }

  private String getSimpleClassName(LinkableResource linkedResource) {

    return HalApiReflectionUtils.getSimpleClassName(linkedResource, typeSupport);
  }

  private boolean filterLinksToEmbeddedResource(Object relatedResource) {

    if (!(relatedResource instanceof EmbeddableResource)) {
      return true;
    }

    EmbeddableResource embedded = (EmbeddableResource)relatedResource;
    if (!embedded.isEmbedded()) {
      return true;
    }

    return embedded.isLinkedWhenEmbedded();
  }

  private Single<List<HalResource>> renderEmbeddedResources(Method method, Observable<?> rxRelatedResources) {

    // embedded resources can only occur for methods that don't have parameters
    // (because if the method has parameters, it must be a link template)
    if (method.getParameterCount() == 0) {

      // filter only those resources that are actually embedded
      Observable<EmbeddableResource> rxEmbeddedResourceImpls = rxRelatedResources
          .filter(r -> r instanceof EmbeddableResource)
          .map(EmbeddableResource.class::cast)
          .filter(r -> r.isEmbedded());

      // and render them by recursively calling the render function from AsyncHalResourceRendererImpl
      Observable<HalResource> rxHalResources = rxEmbeddedResourceImpls
          .concatMapEager(r -> recursiveRenderFunc.apply(r).toObservable());

      return rxHalResources.toList();
    }

    return Single.just(Collections.emptyList());
  }

  /**
   * A result class that combines all links and embedded resources for a given relation.
   */
  static final class RelationRenderResult {

    private final String relation;
    private final List<Link> links;
    private final List<HalResource> embedded;

    private final boolean multiValue;


    private RelationRenderResult(String relation, List<Link> links, List<HalResource> embedded, boolean multiValue) {
      this.relation = relation;
      this.links = links;
      this.embedded = embedded;
      this.multiValue = multiValue;
    }

    String getRelation() {
      return this.relation;
    }

    List<Link> getLinks() {
      return this.links;
    }

    List<HalResource> getEmbedded() {
      return this.embedded;
    }

    boolean isMultiValue() {
      return multiValue;
    }

  }
}
