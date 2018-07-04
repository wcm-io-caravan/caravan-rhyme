/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.server.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.common.EmbeddableResource;
import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.hal.api.server.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.hal.api.server.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;
import rx.Single;

/**
 * Contains methods to generate a HalResource or JAX-RS response from a given server-side HAL resource implementation
 */
public final class AsyncHalResourceRendererImpl implements AsyncHalResourceRenderer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public Single<HalResource> renderResource(Object resourceImplInstance) {

    Preconditions.checkNotNull(resourceImplInstance, "Can not create a HalResource from a null reference");

    // check which of the interface implemented by the given implementation class is the one annotated with @HalApiInterface
    Class<?> resourceInterface = HalApiReflectionUtils.findHalApiInterface(resourceImplInstance);

    // call the method annotated with @ResourceState to get the JSON properties
    Single<ObjectNode> rxState = getResourceState(resourceInterface, resourceImplInstance);

    // also collect the linked or embedded resources for each method annotated with @RelatedResource
    Observable<RelatedResourceInfo> rxRelated = findRelatedResources(resourceInterface, resourceImplInstance);

    // wait until all this is available
    return Observable.zip(rxState.toObservable(), rxRelated.toList(), (stateNode, listOfRelated) -> {
      // and then create the HalResource instance
      return createHalResource(resourceImplInstance, stateNode, listOfRelated);
    }).toSingle();
  }

  private Single<ObjectNode> getResourceState(Class<?> resourceInterface, Object resourceImplInstance) {

    return HalApiReflectionUtils.getResourceStateObservable(resourceInterface, resourceImplInstance)
        .map(object -> OBJECT_MAPPER.convertValue(object, ObjectNode.class));
  }


  private Observable<RelatedResourceInfo> findRelatedResources(Class<?> resourceInterface, Object resourceImplInstance) {

    // find all methods annotated with @RelatedResource and sort them alphabetically
    return HalApiReflectionUtils.getSortedRelatedResourceMethods(resourceInterface)
        // create a RelatedResourceInfo with the links and embedded resources returned by each method
        .concatMap(method -> createRelatedResourceInfo(resourceImplInstance, method));
  }

  private Observable<RelatedResourceInfo> createRelatedResourceInfo(Object resourceImplInstance, Method method) {

    verifyReturnType(resourceImplInstance, method);

    // call the implementation of the method to get an observable of related resource implementation instances
    Observable<?> rxRelatedResources = RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImplInstance, method).cache();

    // create links for those resources that implement LinkableResource
    Observable<Link> rxLinks = createLinksTo(rxRelatedResources);

    // and completely render those resources that should be embeded (asynchronously)
    Observable<HalResource> rxEmbeddedHalResources = renderEmbeddedResources(method, rxRelatedResources);

    // wait for all this to be complete before creating a RelatedReseourceInfo with all thisdata
    String relation = method.getAnnotation(RelatedResource.class).relation();
    return Observable.zip(rxLinks.toList(), rxEmbeddedHalResources.toList(), (links, embeddedResources) -> {
      return new RelatedResourceInfo(relation, links, embeddedResources);
    });
  }

  private void verifyReturnType(Object resourceImplInstance, Method method) {
    String fullMethodName = resourceImplInstance.getClass().getSimpleName() + "#" + method.getName();

    // get the emitted result resource type from the method signature
    Class<?> relatedResourceInterface = RxJavaReflectionUtils.getObservableEmissionType(method);
    if (relatedResourceInterface.getAnnotation(HalApiInterface.class) == null && !LinkableResource.class.equals(relatedResourceInterface)) {
      throw new RuntimeException("The method " + fullMethodName + " returns an Observable<" + relatedResourceInterface.getName() + ">, "
          + " but it must return an Observable that emits objects that implement a HAL API interface annotated with the @"
          + HalApiInterface.class.getSimpleName() + " annotation");
    }
  }

  private Observable<Link> createLinksTo(Observable<?> rxRelatedResources) {

    Observable<LinkableResource> rxLinkedResourceImpls = rxRelatedResources
        .filter(r -> r instanceof LinkableResource)
        .map(r -> (LinkableResource)r);

    Observable<Link> rxLinks = rxLinkedResourceImpls
        .map(r -> r.createLink())
        .filter(Objects::nonNull);

    return rxLinks;
  }

  private Observable<HalResource> renderEmbeddedResources(Method method, Observable<?> rxRelatedResources) {

    if (method.getParameterCount() == 0) {

      // this is a method without any parameters, so we can assume the link is not a template, and it might
      // also be possible to embed the resource

      Observable<EmbeddableResource> rxEmbeddedResourceImpls = rxRelatedResources
          .filter(r -> r instanceof EmbeddableResource)
          .map(r -> (EmbeddableResource)r)
          .filter(r -> r.isEmbedded());

      return rxEmbeddedResourceImpls.concatMap(r -> renderResource(r).toObservable());
    }

    return Observable.empty();
  }

  private static class RelatedResourceInfo {

    final String relation;
    final List<Link> links;
    final List<HalResource> embedded;

    private RelatedResourceInfo(String relation, List<Link> links, List<HalResource> embedded) {

      this.relation = relation;
      this.links = links;
      this.embedded = embedded;
    }
  }

  private HalResource createHalResource(Object resourceImplInstance, ObjectNode stateNode, List<RelatedResourceInfo> listOfRelated) {
    HalResource hal = new HalResource(stateNode);

    if (resourceImplInstance instanceof LinkableResource) {
      Link selfLink = ((LinkableResource)resourceImplInstance).createLink();
      if (selfLink != null) {
        hal.setLink(selfLink);
      }
    }

    for (RelatedResourceInfo related : listOfRelated) {
      hal.addLinks(related.relation, related.links);
      hal.addEmbedded(related.relation, related.embedded);
    }

    return hal;
  }

  static HalResource renderResourceBlocking(Object resourceImplInstance) {
    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl();

    Single<HalResource> rxResource = renderer.renderResource(resourceImplInstance);

    return rxResource.toObservable().toBlocking().single();
  }
}
