/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.server.impl;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.common.EmbeddableResource;
import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.hal.api.server.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;
import rx.Single;

/**
 * Contains methods to generate a HalResource or JAX-RS response from a given server-side HAL resource implementation
 */
public final class AsyncHalResourceRendererImpl {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static Comparator<Method> methodRelationComparator = (method1, method2) -> {
    String curi1 = method1.getAnnotation(RelatedResource.class).relation();
    String curi2 = method2.getAnnotation(RelatedResource.class).relation();

    // make sure that all links with custom link relations are displayed first
    if (curi1.contains(":") && !curi2.contains(":")) {
      return -1;
    }
    // make sure that all links with standard relations are displayed last
    if (curi2.contains(":") && !curi1.contains(":")) {
      return 1;
    }
    // otherwise the links should be sorted alphabetically
    return curi1.compareTo(curi2);
  };

  private Observable<RelatedResourceInfo> findRelatedResources(Class<?> resourceInterface, Object resourceImplInstance) {

    // find all methods annotated with @RelatedResource and sort them alphabetilcally
    return getSortedRelatedResourceMethods(resourceInterface)
        .concatMap(method -> createRelatedResourceInfo(resourceImplInstance, method));
  }

  private Observable<RelatedResourceInfo> createRelatedResourceInfo(Object resourceImplInstance, Method method) {

    String fullMethodName = resourceImplInstance.getClass().getSimpleName() + "#" + method.getName();

    String relation = method.getAnnotation(RelatedResource.class).relation();

    // get the emitted result resource type from the method signature
    Class<?> relatedResourceInterface = RxJavaReflectionUtils.getObservableEmissionType(method);
    if (relatedResourceInterface.getAnnotation(HalApiInterface.class) == null && !LinkableResource.class.equals(relatedResourceInterface)) {
      throw new RuntimeException("The method " + fullMethodName + " returns an Observable<" + relatedResourceInterface.getName() + ">, "
          + " but it must return an Observable that emits objects that implement a HAL API interface annotated with the @"
          + HalApiInterface.class.getSimpleName() + " annotation");
    }

    // call the implementation of the method
    Observable<?> rxRelatedResources = RxJavaReflectionUtils.invokeMethodAndReturnObservable(resourceImplInstance, method).cache();

    Observable<HalResource> rxEmbeddedHalResources;
    if (method.getParameterCount() == 0) {

      // this is a method without any parameters, so we can assume the link is not a template, and it might
      // also be possible to embed the resource

      Observable<EmbeddableResource> rxEmbeddedResourceImpls = rxRelatedResources
          .filter(r -> r instanceof EmbeddableResource)
          .map(r -> (EmbeddableResource)r)
          .filter(r -> r.isEmbedded());

      rxEmbeddedHalResources = rxEmbeddedResourceImpls.concatMap(r -> renderResource(r).toObservable());
    }
    else {
      rxEmbeddedHalResources = Observable.empty();
    }

    Observable<LinkableResource> rxLinkedResourceImpls = rxRelatedResources
        .filter(r -> r instanceof LinkableResource)
        .map(r -> (LinkableResource)r);

    Observable<Link> rxLinks = rxLinkedResourceImpls
        .map(r -> r.createLink())
        .filter(Objects::nonNull);

    return Observable.zip(rxLinks.toList(), rxEmbeddedHalResources.toList(), (links, embeddedResources) -> {
      RelatedResourceInfo info = new RelatedResourceInfo();

      info.relation = relation;
      info.links = links;
      info.embedded = embeddedResources;

      return info;
    });
  }

  static Observable<Method> getSortedRelatedResourceMethods(Class<?> resourceInterface) {

    return Observable.from(resourceInterface.getMethods())
        .filter(method -> method.getAnnotation(RelatedResource.class) != null)
        .toSortedList((m1, m2) -> methodRelationComparator.compare(m1, m2))
        .flatMapIterable(l -> l);
  }

  static class RelatedResourceInfo {

    String relation;
    List<Link> links;
    List<HalResource> embedded;
  }

  public Single<HalResource> renderResource(Object resourceImplInstance) {

    Preconditions.checkNotNull(resourceImplInstance, "Can not create a HalResource from a null reference");

    // check which of the interface implemented by the given implementation class is the one annotated with @HalApiInterface
    Class<?> resourceInterface = HalApiReflectionUtils.findHalApiInterface(resourceImplInstance);

    Single<ObjectNode> rxState = HalApiReflectionUtils.getResourceStateObservable(resourceInterface, resourceImplInstance)
        .map(object -> OBJECT_MAPPER.convertValue(object, ObjectNode.class));

    Observable<RelatedResourceInfo> rxRelated = findRelatedResources(resourceInterface, resourceImplInstance);


    return Observable.zip(rxState.toObservable(), rxRelated.toList(), (stateNode, listOfRelated) -> {

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
    }).toSingle();
  }

  static HalResource renderResourceBlocking(Object resourceImplInstance) {
    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl();

    Single<HalResource> rxResource = renderer.renderResource(resourceImplInstance);

    return rxResource.toObservable().toBlocking().single();
  }
}
