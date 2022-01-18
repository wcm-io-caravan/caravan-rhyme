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
package io.wcm.caravan.rhyme.impl.client.proxy;

import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.isHalApiInterface;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;

class RelatedResourceHandler implements Function<HalResource, Observable<Object>> {

  private static final Logger log = LoggerFactory.getLogger(RelatedResourceHandler.class);

  private final HalApiMethodInvocation invocation;
  private final HalApiClientProxyFactory proxyFactory;
  private final HalApiAnnotationSupport annotationSupport;

  RelatedResourceHandler(HalApiMethodInvocation invocation, HalApiAnnotationSupport annotationSupport, HalApiClientProxyFactory proxyFactory) {
    this.invocation = invocation;
    this.proxyFactory = proxyFactory;
    this.annotationSupport = annotationSupport;
  }

  @Override
  public Observable<Object> apply(HalResource contextResource) {

    // check which relation should be followed and what type of objects the Observable emits
    String relation = invocation.getRelation();
    Class<?> relatedResourceType = invocation.getEmissionType();

    if (!isHalApiInterface(relatedResourceType, annotationSupport)
        && !HalApiReflectionUtils.isPlainLink(relatedResourceType)) {
      throw new HalApiDeveloperException("The method " + invocation + " has an invalid emission type " + relatedResourceType.getName() +
          " which does not have a @" + HalApiInterface.class.getSimpleName() + " annotation.");
    }

    List<Link> links = contextResource.getLinks(relation);
    List<HalResource> embeddedResources = contextResource.getEmbedded(relation);

    Observable<Object> rxEmbedded = getEmbedded(relation, relatedResourceType, embeddedResources, links);
    Observable<Object> rxLinked = getLinked(invocation, relation, relatedResourceType, embeddedResources, links);

    return rxEmbedded.concatWith(rxLinked);
  }

  private Observable<Object> getEmbedded(String relation, Class<?> relatedResourceType, List<HalResource> embeddedResources, List<Link> links) {

    log.trace("{} embedded resources with relation {} were found in the context resource", embeddedResources.size(), relation);

    return createProxiesFromEmbeddedResources(relatedResourceType, embeddedResources, links);
  }

  private Observable<Object> getLinked(HalApiMethodInvocation invocation, String relation, Class<?> relatedResourceType, List<HalResource> embeddedResources,
      List<Link> links) {

    log.trace("{} links with relation {} were found in the context resource", links.size(), relation);

    List<Link> relevantLinks = getOnlyLinksToResourcesThatArentAlreadyEmbedded(links, embeddedResources);

    long numTemplatedLinks = relevantLinks.stream().filter(Link::isTemplated).count();
    Map<String, Object> variables = invocation.getTemplateVariables();

    if (!variables.isEmpty()) {
      // if null values were specified for all method parameters, we assume that the caller is only interested in the link templates
      if (invocation.isCalledWithOnlyNullParameters()) {
        return createProxiesFromLinkTemplates(relatedResourceType, relevantLinks);
      }

      // otherwise we ignore any resolved links, and only consider link templates that contain all variables specified
      // in the method invocation
      relevantLinks = relevantLinks.stream()
          .filter(Link::isTemplated)
          .filter(link -> linkTemplateHasAllVariables(link, variables))
          .collect(Collectors.toList());

      if (relevantLinks.isEmpty()) {
        return Observable.error(new HalApiDeveloperException(
            "No matching link template found with relation " + relation + " which contains the variables " + variables.keySet()
                + " required for the invocation of " + invocation.toString()));
      }
    }
    else {
      // if the method being called doesn't contain parameters for template variables, then link templates should be ignored
      // (unless there are only link templates present)
      if (numTemplatedLinks != relevantLinks.size()) {
        relevantLinks = relevantLinks.stream().filter(link -> !link.isTemplated()).collect(Collectors.toList());
      }
    }

    // if the resources are linked, then we have to fetch those resources first
    return createProxiesForLinkedHalResources(relatedResourceType, relevantLinks, variables);
  }

  private static boolean linkTemplateHasAllVariables(Link link, Map<String, Object> variables) {

    List<String> variablesInInvocation = variables.entrySet().stream()
        .filter(e -> e.getValue() != null)
        .map(Entry::getKey)
        .collect(Collectors.toList());

    UriTemplate template = UriTemplate.fromTemplate(link.getHref());
    ImmutableList<String> variablesInTemplate = ImmutableList.copyOf(template.getVariables());
    return variablesInTemplate.containsAll(variablesInInvocation);
  }

  private static List<Link> getOnlyLinksToResourcesThatArentAlreadyEmbedded(List<Link> links, List<HalResource> embeddedResources) {

    Set<String> embeddedHrefs = embeddedResources.stream()
        .map(HalResource::getLink)
        .filter(Objects::nonNull)
        .map(Link::getHref)
        .collect(Collectors.toSet());

    return links.stream()
        .filter(link -> !embeddedHrefs.contains(link.getHref()))
        .collect(Collectors.toList());
  }

  private Observable<Object> createProxiesFromEmbeddedResources(Class<?> relatedResourceType, List<HalResource> embeddedResources, List<Link> links) {

    Map<String, Link> linksByHref = links.stream()
        .collect(Collectors.toMap(Link::getHref, link -> link, (l1, l2) -> l1));

    return Observable.fromIterable(embeddedResources)
        .map(embeddedResource -> {

          // if the embedded resource is also linked, we want to make the original (possibly named) link available
          // for extraction by the ResourceLinkHandler, but otherwise we just use the self link
          Link selfLink = embeddedResource.getLink();
          String selfHref = selfLink != null ? selfLink.getHref() : null;
          Link link = linksByHref.getOrDefault(selfHref, selfLink);

          return proxyFactory.createProxyFromHalResource(relatedResourceType, embeddedResource, link);
        });
  }

  private Observable<Object> createProxiesForLinkedHalResources(Class<?> relatedResourceType, List<Link> links, Map<String, Object> parameters) {

    return Observable.fromIterable(links)
        // if the link is templated then expand it with the method parameters
        .map(link -> link.isTemplated() ? expandLinkTemplates(link, parameters) : link)
        .map(link -> {

          // if the method returns a link, then there is no need for a proxy, but we can return it directly
          if (Link.class.equals(relatedResourceType)) {
            return link;
          }

          // otherwise create a new proxy implementing the HalApiInterface of the link target
          return proxyFactory.createProxyFromLink(relatedResourceType, link);
        });
  }

  private static Link expandLinkTemplates(Link link, Map<String, Object> parameters) {

    String uri = UriTemplate.expand(link.getHref(), parameters);

    Link clonedLink = new Link(link.getModel().deepCopy());
    clonedLink.setTemplated(false);
    clonedLink.setHref(uri);
    return clonedLink;
  }

  private Observable<Object> createProxiesFromLinkTemplates(Class<?> relatedResourceType, List<Link> links) {

    // do not expand the link templates
    return Observable.fromIterable(links)
        .map(link -> proxyFactory.createProxyFromLink(relatedResourceType, link));
  }

}
